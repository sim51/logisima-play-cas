package app;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

import play.Logger;
import play.Play;
import play.mvc.Before;
import play.mvc.Controller;
import play.utils.Java;
import edu.yale.its.tp.cas.client.ServiceTicketValidator;


public class SecureCAS extends Controller {

	@Before(unless = { "fail", "login", "authenticate", "logout" })
	static void checkAccess() throws Throwable {
		Logger.debug("[SecureCAS]: checkAccess " + request.url);
		
		// Authent
		if (!session.contains("username")) {
			Logger.debug("[SecureCAS]: session does'nt contain a username");
			flash.put("url", request.method == "GET" ? request.url : "/"); // seems a good default
			String ticket = params.get("ticket");
			if (ticket != null) {
				flash.put("params", params);
				Logger.debug("[SecureCAS]: validate ticket " + ticket + " by CAS");
				authenticate(ticket);
			}
			login();
		}
		// Checks
		Check check = getActionAnnotation(Check.class);
		if (check != null) {
			check(check);
		}
		check = getControllerInheritedAnnotation(Check.class);
		if (check != null) {
			check(check);
		}
	}

	private static void check(Check check) throws Throwable {
		for (String profile : check.value()) {
			boolean hasProfile = (Boolean) Security.invoke("check", profile);
			if (!hasProfile) {
				Security.invoke("onCheckFailed", profile);
			}
		}
	}

	static void redirectToCas() throws Throwable {
		String urlCas = Play.configuration.getProperty("cas.loginUrl");
		String url = flash.get("url");
		if (url == null) {
			url = "/";
		}
		redirect(urlCas + "?service=" + request.getBase() + url);
	}

	public static void login() throws Throwable {
		redirectToCas();
	}

	public static void fail() throws Throwable {
		render();
	}

	public static void authenticate(String ticket) throws Throwable {
		Logger.debug("authenticate " + flash.get("params"));
		
		// Init
		String username = "";
		boolean isvalid = false;
		String url = flash.get("url");
		if (url == null) {
			url = "/";
		}
		// Instantiate a new ServiceTicketValidator
		Logger.debug("Try to validate ticket " + ticket + " for service " +  getUrlwithoutTicket(ticket));
		ServiceTicketValidator sv = new ServiceTicketValidator();
		// Set its parameters 
		sv.setCasValidateUrl(Play.configuration.getProperty("cas.validateUrl"));
		sv.setService(getUrlwithoutTicket(ticket));
		sv.setServiceTicket(ticket);
		sv.validate();
		
		if(sv.isAuthenticationSuccesful()){
			username = sv.getUser();
			isvalid = (Boolean) Security.invoke("authentify", username);
			session.put("username", username);
			Logger.debug("User " + username + " is authenticated");
		}
		else {
			Logger.debug("User is not authenticated");
		}

		if (!isvalid) {
			flash.keep("url");
			flash.error("secure.error");
			params.flash();
			fail();
			return;
		}
		Logger.info("authenticate : " + params);
		
		// Redirect to the URL without ticket)
		if (params.get("ticket") != null) {
			url = getUrlwithoutTicket(ticket);
			Logger.info("redirect to url " +url);
			flash.put("url", url);
		}
		// Do the redirection
		redirectToOriginalURL();
	}

	public static void logout() throws Throwable {
		session.clear();
		Security.invoke("onDisconnected");
		flash.success("secure.logout");
		redirect(Play.configuration.getProperty("cas.logoutUrl"));
	}

	static void redirectToOriginalURL() throws Throwable {
		Security.invoke("onAuthenticated");
		String url = flash.get("url");
		if (url == null) {
			url = "/";
		}
		redirect(url);
	}
	
	static String getUrlwithoutTicket(String ticket) throws Throwable {
		String url = request.getBase() + flash.get("url");
		url = url.replace("?ticket=" + ticket + "&", "?");
		url = url.replace("?ticket=" + ticket, "");
		url = url.replace("ticket=" + ticket + "&", "");
		url = url.replace("ticket=" + ticket, "");
		Logger.info("url is " + url);
		return url;
	}

	public static class Security extends Controller {

		static boolean authentify(String username) {
			return true;
		}

		static boolean check(String profile) {
			return true;
		}

		static String connected() {
			return session.get("username");
		}

		static boolean isConnected() {
			return session.contains("username");
		}

		static void onAuthenticated() {
		}

		static void onDisconnected() {
		}

		static void onCheckFailed(String profile) {
			forbidden();
		}

		private static Object invoke(String m, Object... args) throws Throwable {
			Logger.info(m);
			Class security = null;
			List<Class> classes = Play.classloader
					.getAssignableClasses(Security.class);
			if (classes.size() == 0) {
				security = Security.class;
			} else {
				security = classes.get(0);
			}
			try {
				return Java.invokeStaticOrParent(security, m, args);
			} catch (InvocationTargetException e) {
				throw e.getTargetException();
			}
		}

	}

}
