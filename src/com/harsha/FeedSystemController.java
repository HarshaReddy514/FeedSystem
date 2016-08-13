package com.harsha;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.jdo.PersistenceManager;
import javax.jdo.Query;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.xml.bind.DatatypeConverter;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import com.google.gson.Gson;

@Controller
public class FeedSystemController {

	PersistenceManager pm = PMF.get().getPersistenceManager();
	List<String> userData, feedData, userMailId;
	UserDetails userDetails;
	String userMail,userName,picture;

	@RequestMapping("/")
	public String home() {
		return "index";
	}

	@RequestMapping(value = "/update", method = RequestMethod.GET)
	public ModelAndView updates(HttpServletResponse response, HttpServletRequest request) throws IOException {
		HttpSession session = request.getSession();
		System.out.println("Username to display:" + userData.get(0));
		session.setAttribute("name", userData.get(0));
		session.setAttribute("mail", userData.get(1));
		System.out.println(userData);
		return new ModelAndView("update", "userName", userData.get(0));
	}

	@SuppressWarnings("unchecked")
	@RequestMapping(value = "/getUsers", method = RequestMethod.GET)
	public void getUsers(HttpServletResponse response) throws IOException {
		String queryStr = "select FROM " + UserDetails.class.getName() + " ORDER BY signUpUserName ASC";
		Query q = pm.newQuery(queryStr);
		try {
			List<UserDetails> results = null;
			results = (List<UserDetails>) q.execute();
			if (!results.isEmpty()) {
				System.out.println(results);
				response.getWriter().write(new Gson().toJson(results));
			}
		} finally {
			q.closeAll();
		}
	}

	@RequestMapping(value = "/signupData", method = RequestMethod.GET)
	public ModelAndView signUpData() {
		System.out.println(userDetails.getSignUpUserName());
		return new ModelAndView("signup", "name", userDetails.getSignUpUserName());
	}

	@RequestMapping(value = "/login", method = RequestMethod.POST)
	public void login(HttpServletRequest request, HttpServletResponse response) throws IOException {
		String userName = request.getParameter("email");
		String password = request.getParameter("password");
		System.out.println(userName);
		System.out.println(password);
		Login login = new Login();
		List<String> userData = data(userName);
		System.out.println("userdata:" + userData);
		if (userData.contains(userName) && userData.contains(password)) {
			login.setUserName(userName);
			login.setPassword(password);
			HttpSession session = request.getSession();
			session.setAttribute("name", userData.get(0));
			response.getWriter().write(new Gson().toJson("false"));
		} else {
			response.getWriter().write(new Gson().toJson(userName));
		}
	}

	@SuppressWarnings("unchecked")
	@RequestMapping(value = "/updateservlet", method = RequestMethod.POST)
	public ModelAndView update(HttpServletRequest request, HttpServletResponse response) throws IOException {

		String userName1 = (String) request.getSession().getAttribute("name");
		System.out.println(userName1);
		String feedText = request.getParameter("feed");
		String userName = request.getParameter("userName");
		String userMail = request.getParameter("userMail");
		String completeUserName = userName.substring(8);
		System.out.println("Complete UserName:" + completeUserName);
		long millis = System.currentTimeMillis();
		UpdateFeed updateFeed = new UpdateFeed();
		if (!feedText.equals("")) {
			updateFeed.setFeed(feedText);
			updateFeed.setUserMail(userMail);
			updateFeed.setDate(millis);
			updateFeed.setUserName(completeUserName);
			System.out.println("userMail:" + updateFeed.getUserMail());
			pm.makePersistent(updateFeed);
			String userNameToDisplay = new Gson().toJson(completeUserName);
			System.out.println("UserNameTDisplay:" + userNameToDisplay);
			String feedToDisplay = new Gson().toJson(feedText);
			System.out.println("Feed To display:" + feedToDisplay);
			String dateToDisplay = new Gson().toJson(millis);
			String jsonObjects = "[" + userNameToDisplay + "," + feedToDisplay + "," + dateToDisplay + "]";// creating
																											// json
																											// array
			response.getWriter().write(jsonObjects); // sending response as json
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	@RequestMapping(value = "/signup", method = RequestMethod.POST)
	public void signUp(HttpServletRequest request, HttpServletResponse response) throws IOException {
		String signUpUserName = request.getParameter("userName");
		String signUpPassword = request.getParameter("password");
		String signUpConfirmPassword = request.getParameter("confirmPassword");
		String signUpEmail = request.getParameter("email");

		byte[] message = signUpPassword.getBytes("UTF-8");
		String encoded = DatatypeConverter.printBase64Binary(message);
		byte[] decoded = DatatypeConverter.parseBase64Binary(encoded);

		System.out.println(encoded);
		System.out.println(new String(decoded, "UTF-8"));

		int index = signUpEmail.indexOf("@");
		int dot = signUpEmail.lastIndexOf(".");

		userDetails = new UserDetails();
		if (!signUpUserName.equals("") && !signUpPassword.equals("") && (signUpPassword.length() >= 6)
				&& signUpConfirmPassword.equals(signUpPassword) && index > 1 && dot > index + 2
				&& dot + 2 < signUpEmail.length()) {
			userDetails.setSignUpUserName(signUpUserName);
			userDetails.setSignUpPassword(encoded);
			userDetails.setSignUpEmail(signUpEmail);
			userDetails.setIsDelete(false);
			userDetails.setSource("default");
			long millis;
			userDetails.setDate(millis = System.currentTimeMillis());
			List<String> userData = data(userDetails.getSignUpEmail());
			System.out.println(userData);
			if (!userData.contains(signUpEmail)) {
				try {
					pm.makePersistent(userDetails);
				} finally {
					// pm.close();
				}
				response.getWriter().write(new Gson().toJson("false"));
			} else {
				response.getWriter().write(new Gson().toJson(signUpEmail));
			}
		} else {
			response.getWriter().write(new Gson().toJson(signUpEmail));
		}
	}

	@RequestMapping(value = "/logout")
	public String logout(HttpServletRequest request) {
		HttpSession session = request.getSession(false);
		session.removeAttribute("name");
		session.invalidate();
		return "index";
	}

	@SuppressWarnings({ "unchecked", "null" })
	public List<String> data(String userMail) {
		Query q = pm.newQuery("select from " + UserDetails.class.getName() + " where signUpEmail == signUpEmailParam "
				+ "parameters String signUpEmailParam " + "order by date desc");
		try {
			List<UserDetails> results = null;
			userData = new ArrayList<String>();
			results = (List<UserDetails>) q.execute(userMail);
			if (!results.isEmpty() && !(results == null)) {
				for (UserDetails data : results) {
					userData.add(data.getSignUpUserName());
					userData.add(data.getSignUpEmail());
					byte[] decoded = DatatypeConverter.parseBase64Binary(data.getSignUpPassword());
					try {
						userData.add(new String(decoded, "UTF-8"));
					} catch (UnsupportedEncodingException e) {
						e.printStackTrace();
					}
				}
			}
		} finally {
			q.closeAll();
		}
		return userData;
	}

	@SuppressWarnings("unchecked")
	@RequestMapping(value = "/fetchUpdates")
	public void fetchUpdates(HttpServletResponse response) throws IOException {
		Query q = pm.newQuery("select from " + UpdateFeed.class.getName() + " order by date desc");
		List<UpdateFeed> feeds = null;
		try {
			feeds = (List<UpdateFeed>) q.execute();
			System.out.println("Feeds" + feeds);
			if (!(feeds == null) && !feeds.isEmpty()) {
				System.out.println("Feeds: " + feeds);
				response.getWriter().write(new Gson().toJson(feeds));
			}
		} finally {
			q.closeAll();
		}
	}
	
	@RequestMapping("/goWithGoogle")
	public ModelAndView signUpWithGoogle()
	{
		return new ModelAndView("redirect:https://accounts.google.com/o/oauth2/auth?redirect_uri=http://localhost:8080/get_auth_code&response_type=code&client_id=187391095236-ikc9rlbs4ldqta29d1ejuf2756qba77s.apps.googleusercontent.com&approval_prompt=force&scope=email&access_type=online");
	}
	
	@RequestMapping(value="/get_auth_code")
	public String get_code(@RequestParam String code, HttpServletRequest req,
			HttpServletResponse resp) throws IOException
	{
		
		// code for getting authorization_code
		System.out.println("Getting Authorization.");
		String auth_code = code;
		System.out.println(auth_code);
		
		//code for getting access token
		
		URL url = new URL("https://www.googleapis.com/oauth2/v3/token?"
				+ "client_id=187391095236-ikc9rlbs4ldqta29d1ejuf2756qba77s.apps.googleusercontent.com"
				+ "&client_secret=htzLprFZYXa2RNT4qxTvZ4cp&" + "redirect_uri=http://localhost:8080/get_auth_code&"
				+ "grant_type=authorization_code&" + "code=" + auth_code);
		HttpURLConnection connect = (HttpURLConnection) url.openConnection();
		connect.setRequestMethod("POST");
		connect.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
		connect.setDoOutput(true);
		BufferedReader in = new BufferedReader(new InputStreamReader(connect.getInputStream()));
		String inputLine;
		String response = "";
		while ((inputLine = in.readLine()) != null) {
			response += inputLine;
		}
		in.close();
		System.out.println(response.toString());
		
		JSONParser jsonParser = new JSONParser();
		JSONObject jsonAccessToken=null;
		try {
			jsonAccessToken = (JSONObject) jsonParser.parse(response);
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		String access_token = (String)jsonAccessToken.get("access_token");
		System.out.println("Access token =" + access_token);
		System.out.println("access token caught");
		
		URL obj1 = new URL("https://www.googleapis.com/oauth2/v3/userinfo?access_token=" + access_token);
		HttpURLConnection conn = (HttpURLConnection) obj1.openConnection();
		BufferedReader in1 = new BufferedReader(new InputStreamReader(conn.getInputStream()));
		String inputLine1;
		String responsee = "";
		while ((inputLine1 = in1.readLine()) != null) {
			responsee += inputLine1;
		}
		in1.close();
		System.out.println(responsee.toString());
		JSONObject json_user_details = null;
		try {
			json_user_details = (JSONObject) jsonParser.parse(responsee);
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		userMail=(String)json_user_details.get("email");
		userName=(String)json_user_details.get("name");
		picture=(String)json_user_details.get("picture");
		
		System.out.println(userMail);
		System.out.println(userName);
		
		if(userMail==null || userName==null)
		{
			return "redirect:/";
		}
		
		String CHAR_LIST ="abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890";
		int RANDOM_STRING_LENGTH = 6;
		StringBuffer randStr = new StringBuffer();
        for(int i=0; i<RANDOM_STRING_LENGTH; i++){
            int number = new Random().nextInt(CHAR_LIST.length());
            char ch = CHAR_LIST.charAt(number);
            randStr.append(ch);
        }
		
        if(!userMail.isEmpty() && !userName.isEmpty() && !(userMail=="") && !(userName==""))
		{
        	userDetails = new UserDetails();
			userDetails.setSignUpEmail(userMail);
			userDetails.setSignUpUserName(userName);
			userDetails.setSignUpPassword(randStr.toString());
			userDetails.setIsDelete(false);
			userDetails.setSource("google");
			userDetails.setProfilePic(picture);
			long millis;
			userDetails.setDate(millis = System.currentTimeMillis());
			userData = data(userDetails.getSignUpEmail());
			if(!userData.contains(userMail))
			{
				try
				{
					pm.makePersistent(userDetails);
				}
				finally{					
				}
			}
		}
		HttpSession session = req.getSession();
		session.setAttribute("name", userName);
		session.setAttribute("mail", userMail);
		return "update";
	}
	
	@RequestMapping("/loginWithGoogle")
	public ModelAndView loginWithGoogle()
	{
		return new ModelAndView("redirect:https://accounts.google.com/o/oauth2/auth?redirect_uri=http://localhost:8080/get_auth_code&response_type=code&client_id=187391095236-ikc9rlbs4ldqta29d1ejuf2756qba77s.apps.googleusercontent.com&approval_prompt=force&scope=email&access_type=online");
	}
	
	@RequestMapping(value="/settings")
	public ModelAndView settings(HttpServletRequest request,HttpServletResponse response)
	{
		return new ModelAndView("settings.jsp?name="+userName+"&mail="+userMail);
	}
	
}