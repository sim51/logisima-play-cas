/**
 *  This file is part of LogiSima-play-cas.
 *
 *  LogiSima-play-cas is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  LogiSima-play-cas is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with LogiSima-play-cas.  If not, see <http://www.gnu.org/licenses/>.
 */
package play.modules.cas;

import play.Logger;
import play.Play;
import play.modules.cas.annotation.Check;
import play.mvc.Before;
import play.mvc.Controller;
import edu.yale.its.tp.cas.client.ServiceTicketValidator;

/**
 * This class is a part of the play module secure-cas. It add the ability to
 * check if the user have access to the request. If the user is note logged, it
 * redirect the user to the CAS login page and authenticate it.
 * 
 * @author bsimard
 * 
 */
public class SecureCAS extends Controller {

    /**
     * Method call for the logout route. We simply redirect the user to CAS
     * logout page.
     * 
     * @throws Throwable
     */
    public static void logout() throws Throwable {
        session.clear();
        Security.invoke("onDisconnected");
        flash.success("secure.logout");
        redirect(Play.configuration.getProperty("cas.logoutUrl"));
    }

    /**
     * Method call for the login route. We simply redirect to CAS login page.
     * 
     * @throws Throwable
     */
    public static void login() throws Throwable {
        // Here we lokk if there is the gateway into flsah scope. This is to do
        // the dif between the login method (gateway OK) and the login action (gateway KO).
        if (flash.get("gateway") != null) {
            redirectToCas(Boolean.TRUE);
        } else {
            redirectToCas(Boolean.FALSE);
        }
    }

    /**
     * Method call when the user has not the right to see the page.
     * 
     * @throws Throwable
     */
    public static void fail() throws Throwable {
        render();
    }

    /**
     * Method that check if the user has the rigth to see the page.
     * 
     * @throws Throwable
     */
    @Before(unless = { "fail", "login", "logout" })
    static void checkAccess() throws Throwable {
        Logger.debug("[SecureCAS]: checkAccess " + request.url);

        // Authent
        if (!session.contains("username")) {
            Logger.debug("[SecureCAS]: session doesn't contain a username");
            session.put("url", request.method == "GET" ? request.url : "/");
            String ticket = params.get("ticket");
            if (ticket != null) {
                flash.put("params", params);
                Logger.debug("[SecureCAS]: validate ticket " + ticket + " by CAS");
                valideCasTicket(ticket);
            }
            flash.put("gateway", Boolean.TRUE);
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

    /**
     * Function to check the rights of the user. See your implementation of the
     * Security class with the method check.
     * 
     * @param check
     * @throws Throwable
     */
    private static void check(Check check) throws Throwable {
        for (String profile : check.value()) {
            boolean hasProfile = (Boolean) Security.invoke("check", profile);
            if (!hasProfile) {
                Security.invoke("onCheckFailed", profile);
            }
        }
    }

    /**
     * Method to redirect the user to CAS login page.
     * 
     * @param gateway
     * @throws Throwable
     */
    private static void redirectToCas(Boolean gateway) throws Throwable {
        String urlCas = Play.configuration.getProperty("cas.loginUrl");

        // Service URL (mandatory for proxy CAS !!!
        if (Play.configuration.getProperty("cas.serviceUrl") != null && !Play.configuration.getProperty("cas.serviceUrl").equals("")) {
            urlCas = urlCas + "?service=" + Play.configuration.getProperty("cas.serviceUrl");
        } else {
            String url = flash.get("url");
            if (url == null) {
                url = "/";
            }
            urlCas = urlCas + "?service=" + request.getBase() + url;
        }

        // Gateway feature
        if (gateway && Boolean.valueOf(Play.configuration.getProperty("cas.gateway"))) {
            urlCas = urlCas + "&gateway=true";
        }

        Logger.debug("[SecureCAS]: redirect to cas :" + urlCas);
        redirect(urlCas);
    }

    /**
     * Method that verify if the cas ticket is valid.
     * 
     * @param ticket
     *            the cas ticket.
     * @throws Throwable
     */
    private static void valideCasTicket(String ticket) throws Throwable {
        try {
            // Init
            String username = "";
            boolean isvalid = false;
            String url = flash.get("url");
            if (url == null) {
                url = "/";
            }
            // Instantiate a new ServiceTicketValidator
            Logger.debug("[SecureCAS]: Try to validate ticket " + ticket + " for service " + getUrlwithoutTicket(ticket));
            ServiceTicketValidator sv = new ServiceTicketValidator();
            // Set its parameters
            sv.setCasValidateUrl(Play.configuration.getProperty("cas.validateUrl"));
            sv.setService(getUrlwithoutTicket(ticket));
            sv.setServiceTicket(ticket);
            sv.validate();

            if (sv.isAuthenticationSuccesful()) {
                username = sv.getUser();
                isvalid = (Boolean) Security.invoke("authentify", username);
                session.put("username", username);
                Logger.debug("[SecureCAS]: User " + username + " is authenticated");
            } else {
                Logger.debug("[SecureCAS]: User is not authenticated");
            }

            if (!isvalid) {
                flash.keep("url");
                flash.error("secure.error");
                params.flash();
                fail();
                return;
            }

            Logger.info("[SecureCAS]: authenticate : " + params);

            // Redirect to the URL without ticket
            if (params.get("ticket") != null) {
                url = getUrlwithoutTicket(ticket);
                Logger.info("redirect to url " + url);
                flash.put("url", url);
            }
            // Do the redirection
            redirectToOriginalURL();
        } catch (Exception e) {
            throw new Throwable(e);
        }
    }

    /**
     * Method that redirect the user to the original URL (argument url in the
     * flash scope).
     * 
     * @throws Throwable
     */
    private static void redirectToOriginalURL() throws Throwable {
        Security.invoke("onAuthenticated");
        String url = flash.get("url");
        if (url == null) {
            url = "/";
        }
        redirect(url);
    }

    /**
     * Function to return the url without the cas ticket.
     * 
     * @param ticket
     *            the cas ticket
     * @return
     * @throws Throwable
     */
    private static String getUrlwithoutTicket(String ticket) throws Throwable {
        String url = request.getBase() + flash.get("url");
        url = url.replace("?ticket=" + ticket + "&", "?");
        url = url.replace("?ticket=" + ticket, "");
        url = url.replace("ticket=" + ticket + "&", "");
        url = url.replace("ticket=" + ticket, "");
        Logger.info("url is " + url);
        return url;
    }

}
