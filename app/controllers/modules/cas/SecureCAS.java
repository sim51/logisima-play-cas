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
package controllers.modules.cas;

import org.apache.commons.lang.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import play.Logger;
import play.Play;
import play.cache.Cache;
import play.libs.XML;
import play.modules.cas.CASUtils;
import play.modules.cas.annotation.Check;
import play.modules.cas.models.CASUser;
import play.mvc.Before;
import play.mvc.Controller;
import play.mvc.Router;
import play.mvc.Util;

import java.net.URLDecoder;
import java.net.URLEncoder;

/**
 * This class is a part of the play module secure-cas. It add the ability to check if the user have access to the
 * request. If the user is note logged, it redirect the user to the CAS login page and authenticate it.
 *
 * @author bsimard
 */
public class SecureCAS extends Controller {
    private static final String APP_URL = Play.configuration.getProperty("application.url");
    private static final String USERNAME = "username";
    public static final String TEN_YEARS_EXPIRATION = "3650d";

    /**
     * Action for the login route. We simply redirect to CAS login page.
     *
     * @throws Throwable
     */
    public static void login() throws Throwable {
        // We must avoid infinite loops after success authentication
        String originUrl = null;
        if (!Router.route(request).action.equals("modules.cas.SecureCAS.login")) {
            // we put into cache the url we come from
            originUrl = request.method.equals("GET") ? request.url : "/";
        } else {
            originUrl = APP_URL;
        }
        Logger.debug("[SecureCAS]: adding url " + originUrl + " into cache with key " + "url_" + session.getId());
        Cache.add("url_" + session.getId(), StringUtils.isEmpty(originUrl) ? "/" : originUrl, "10min");

        // we redirect the user to the cas login page
        String casLoginUrl = CASUtils.getCasLoginUrl(false);
        redirect(casLoginUrl);
    }

    /**
     * Action for the logout route. We clear cache & session and redirect the user to CAS logout page.
     *
     * @throws Throwable
     */
    public static void logout(String redirect) throws Throwable {
        doLogout();

        // we redirect to the cas logout page.
        String casLogoutUrl = getCasLogoutUrl(redirect);
        redirect(casLogoutUrl);
    }

    private static String getCasLogoutUrl(String redirect) {
        StringBuilder url = new StringBuilder();
        String casLogoutUrl = CASUtils.getCasLogoutUrl();
        url.append(casLogoutUrl);
        if (!StringUtils.isEmpty(redirect)) {
            url.append("?service=").append(redirect);
        }
        return url.toString();
    }

    private static final String LOGOUT_REQ_PARAMETER = "logoutRequest";

    public static void handleLogout(String body) throws Throwable {
        int i = StringUtils.indexOf(body, LOGOUT_REQ_PARAMETER);
        String postBody =  URLDecoder.decode(body, "UTF-8");
        if (i == 0 ) {
            String logoutRequestMessage = StringUtils.substring(postBody, LOGOUT_REQ_PARAMETER.length() + 1);
            if (StringUtils.isNotEmpty(logoutRequestMessage)) {

                Document document = XML.getDocument(logoutRequestMessage);
                if (document != null) {
                    NodeList nodeList = document.getElementsByTagName("samlp:SessionIndex");
                    if (nodeList != null && nodeList.getLength() > 0) {
                        Node node = nodeList.item(0);
                        String ticket = node.getTextContent();
                        String stKey = ST + "_" + ticket;
                         String username = Cache.get(stKey, String.class);
                        if (username != null){
                            Cache.delete(stKey);
                            Cache.set(LOGOUT_TAG + "_" + username, 1, TEN_YEARS_EXPIRATION);
                            Logger.debug("Mark that %s has been logout ", username);
                            return;
                        }
                    }
                }
            }
        }
        Logger.warn("illegal logout message: %s", postBody);
    }

    @Util
    public static void doLogout() throws Throwable {
        String username = session.get("username");

        // we clear cache
        Cache.delete("pgt_" + username);

        // we clear session
        session.clear();

        // we invoke the implementation of "onDisconnected"
        Security.invoke("onDisconnected");
    }

    /**
     * Action when the user authentification or checking rights fails.
     *
     * @throws Throwable
     */
    public static void fail() throws Throwable {
        forbidden();
    }

    private static final String ST = "CAS-ST";

    /**
     * Action for the CAS return.
     *
     * @throws Throwable
     */
    public static void authenticate() throws Throwable {
        Boolean isAuthenticated = Boolean.FALSE;
        String ticket = params.get("ticket");
        if (ticket != null) {
            Logger.debug("[SecureCAS]: Try to validate ticket " + ticket);
            CASUser user = CASUtils.valideCasTicket(ticket);
            if (user != null) {
                isAuthenticated = Boolean.TRUE;
                String username = user.getUsername();
                session.put("username", username);
                Cache.set(ST + "_" + ticket, username, TEN_YEARS_EXPIRATION);
                // we invoke the implementation of onAuthenticate
                Security.invoke("onAuthenticated", user);
            }
        }

        if (isAuthenticated) {
            // we redirect to the original URL
            String url = (String) Cache.get("url_" + session.getId());
            Logger.debug("[SecureCAS]: find url " + url + " into cache for the key " + "url_" + session.getId());
            Cache.delete("url_" + session.getId());
            if (url == null) {
                url = "/";
            }
            Logger.debug("[SecureCAS]: redirect to url -> " + url);
            redirect(url);
        } else {
            fail();
        }
    }

    /**
     * Action for the proxy call back url.
     */
    public static void pgtCallBack() throws Throwable {
        // CAS server call this URL with PGTIou & PGTId
        String pgtIou = params.get("pgtIou");
        String pgtId = params.get("pgtId");

        // here we put in cache PGT with PGTIOU as key
        if (pgtIou != null || pgtId != null) {
            Cache.set(pgtIou, pgtId);
        }
    }

    private static final String LOGOUT_TAG = "CAS-LOGOUT";

    /**
     * 检查登陆状态
     */
    @Before(priority = 50)
    public static void checkLoginStatus() {
        if (session.contains(USERNAME)) {
            String username = session.get(USERNAME);
            String logoutTagKey = LOGOUT_TAG + "_" + username;
            if (Cache.get(logoutTagKey) != null) {
                try {
                    doLogout();
                } catch (Throwable throwable) {
                    Logger.warn(throwable.getMessage());
                } finally {
                    Cache.delete(logoutTagKey);
                }
                Logger.debug("%s is logout ", username);
            }
        }
    }

    /**
     * Method that do CAS Filter and check rights.
     *
     * @throws Throwable
     */
    @Before(unless = {"login", "logout", "fail", "authenticate", "pgtCallBack", "handleLogout"}, priority = 100)
    public static void filter() throws Throwable {
        Logger.debug("[SecureCAS]: CAS Filter for URL -> " + request.url);

        // if user is authenticated, the username is in session !
        if (session.contains("username")) {
            // We check the user's profile with class annotation
            Check controllerCheck = getControllerInheritedAnnotation(Check.class);
            if (controllerCheck != null) {
                check(controllerCheck);
            }
            // We check the user's profile with action annotation
            Check actionCheck = getActionAnnotation(Check.class);
            if (actionCheck != null) {
                check(actionCheck);
            }
        } else {
            Logger.debug("[SecureCAS]: user is not authenticated");
            String originUrl = request.method.equals("GET") ? request.url : APP_URL;
            // we put into cache the url we come from
            Cache.add("url_" + session.getId(), StringUtils.isEmpty(originUrl) ? "/" : originUrl, "10min");
            Logger.debug("[SecureCAS]: adding url " + originUrl + " into cache with key " + "url_" + session.getId());

            // we redirect the user to the cas login page
            String casLoginUrl = CASUtils.getCasLoginUrl(true);
            redirect(casLoginUrl);
        }
    }

    /**
     * Function to check the rights of the user. See your implementation of the Security class with the method check.
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

}
