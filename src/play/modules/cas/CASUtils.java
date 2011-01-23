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

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import play.Logger;
import play.Play;
import play.Play.Mode;
import play.cache.Cache;
import play.libs.WS;
import play.modules.cas.models.CASUser;
import play.mvc.Router;

/**
 * Utils class for CAS.
 * 
 * @author bsimard
 * 
 */
public class CASUtils {

    /**
     * Method that generate the CAS login page URL.
     * 
     * @param request
     * 
     * @param possibleGateway
     * @throws Throwable
     */
    public static String getCasLoginUrl(Boolean possibleGateway) {

        String casLoginUrl;

        if (isCasMockServer()) {
            casLoginUrl = Router.getFullUrl("modules.cas.MockServer.login");
        }
        else {
            casLoginUrl = Play.configuration.getProperty("cas.loginUrl");
        }

        // we add the service URL (the reverse route for SecureCas.
        casLoginUrl += "?service=" + getCasServiceUrl();

        // Gateway feature
        if (possibleGateway && Boolean.valueOf(Play.configuration.getProperty("cas.gateway"))) {
            casLoginUrl += "&gateway=true";
        }
        Logger.debug("[SecureCAS]: login CAS URL is " + casLoginUrl);

        return casLoginUrl;
    }

    /**
     * Method that generate the CAS logout page URL.
     * 
     * @throws Throwable
     */
    public static String getCasLogoutUrl() {
        if (isCasMockServer()) {
            return Router.getFullUrl("modules.cas.MockServer.logout");
        }
        else {
            return Play.configuration.getProperty("cas.logoutUrl");
        }
    }

    /**
     * Method that return service url.
     * 
     * @throws Throwable
     */
    private static String getCasServiceUrl() {
        String casServiceUrl = Play.configuration.getProperty("application.url");
        casServiceUrl += Router.reverse("modules.cas.SecureCAS.authenticate").url;
        return casServiceUrl;
    }

    /**
     * Method that return service validation url.
     * 
     * @throws Throwable
     */
    private static String getCasServiceValidateUrl() {
        String casServiceValidateUrl;
        if (isCasMockServer()) {
            casServiceValidateUrl = Router.getFullUrl("modules.cas.MockServer.serviceValidate");
        }
        else {
            casServiceValidateUrl = Play.configuration.getProperty("cas.validateUrl");
        }
        return casServiceValidateUrl;
    }

    /**
     * Method that return proxy call back url.
     * 
     * @throws Throwable
     */
    private static String getCasProxyCallBackUrl() {
        String casProxyCallBackUrl = "";
        // proxy call back url must be in https, but not in Mock mode
        if (isCasMockServer()) {
            casProxyCallBackUrl = Play.configuration.getProperty("application.url");
        }
        else {
            if (Play.configuration.getProperty("application.url.ssl") != null
                    && !Play.configuration.getProperty("application.url.ssl").equals("")) {
                casProxyCallBackUrl = Play.configuration.getProperty("application.url.ssl");
            }
            else {
                casProxyCallBackUrl = Play.configuration.getProperty("application.url");
                casProxyCallBackUrl = casProxyCallBackUrl.replaceFirst("http://", "https://");
            }
        }
        casProxyCallBackUrl += Router.reverse("modules.cas.SecureCAS.pgtCallBack").url;
        return casProxyCallBackUrl;
    }

    /**
     * Method that return cas proxy url.
     * 
     * @return
     */
    private static String getCasProxyUrl() {
        String casProxyUrl;
        if (isCasMockServer()) {
            casProxyUrl = Router.getFullUrl("modules.cas.MockServer.proxy");
        }
        else {
            casProxyUrl = Play.configuration.getProperty("cas.proxyUrl");
        }
        return casProxyUrl;
    }

    /**
     * Method to know if proxy cas is enabled (by testing conf).
     * 
     * @return
     */
    private static Boolean isProxyCas() {
        Boolean isProxyCas = Boolean.FALSE;
        if (Play.configuration.getProperty("cas.proxyUrl") != null
                && !Play.configuration.getProperty("cas.proxyUrl").equals("")) {
            isProxyCas = Boolean.TRUE;
        }
        return isProxyCas;
    }

    /**
     * Method to know if CAS Mock server is enabled (by testing conf).
     * 
     * @return
     */
    public static Boolean isCasMockServer() {
        Boolean isCasMockServer = Boolean.FALSE;
        if (Play.configuration.getProperty("cas.mockserver") != null
                && Play.configuration.getProperty("cas.mockserver").equals("true") && Play.mode == Mode.DEV) {
            isCasMockServer = Boolean.TRUE;
        }
        return isCasMockServer;
    }

    /**
     * Method that verify if the cas ticket is valid.
     * 
     * @param ticket
     *            cas tickets
     * @throws ParserConfigurationException
     * @throws SAXException
     * @throws IOException
     * @throws ExecutionException
     * @throws InterruptedException
     * @throws Throwable
     */
    public static CASUser valideCasTicket(String ticket) throws IOException, SAXException,
            ParserConfigurationException, InterruptedException, ExecutionException {

        // contruction of the cas validation URL for service ticket
        String casValidationTicketUrl = getCasServiceValidateUrl();
        // required parameter
        casValidationTicketUrl += "?service=" + getCasServiceUrl();
        casValidationTicketUrl += "&ticket=" + ticket;
        // if proxyCas ON
        if (isProxyCas()) {
            casValidationTicketUrl += "&pgtUrl=" + getCasProxyCallBackUrl();
        }

        // we do the validation request
        Logger.debug("[SecureCAS]: validate cas ticket by calling " + casValidationTicketUrl);
        // com.ning.http.client.Response validationResponse =
        // client.prepareGet(casValidationTicketUrl).execute().get();
        Document response = WS.url(casValidationTicketUrl).get().getXml();

        /** Parsing validation response **/
        // search node "cas:authenticationSuccess"
        NodeList list = response.getElementsByTagName("cas:authenticationSuccess");

        CASUser user = null;
        if (list.getLength() > 0) {
            Map<String, String> casAttribut = null;
            casAttribut = getCasAttributes(response);
            // we populate the CASUser
            user = new CASUser();
            user.setUsername(response.getElementsByTagName("cas:user").item(0).getTextContent());
            user.setAttribut(casAttribut);

            if (isProxyCas()) {
                // here we get PGT from cache
                String PGTIOU = response.getElementsByTagName("cas:proxyGrantingTicket").item(0).getTextContent();
                String pgt = (String) Cache.get(PGTIOU);
                Cache.delete(PGTIOU);

                // we put in cache PGT with PGT_username
                Cache.add("pgt_" + user.getUsername(), pgt);
            }
        }

        return user;
    }

    /**
     * Method to get CAS atribut from cas response.
     * 
     * @param xml
     * @return
     * @throws SAXException
     * @throws IOException
     */
    private static Map<String, String> getCasAttributes(Document document) throws IOException, SAXException {
        Map<String, String> casAttribut = new HashMap<String, String>();

        // search node "cas:attribute"
        NodeList list = document.getElementsByTagName("cas:attributes");

        if (list.getLength() > 0) {
            Node nodeAttribute = list.item(0);
            for (int i = 0; i < nodeAttribute.getChildNodes().getLength(); i++) {
                Node attribute = nodeAttribute.getChildNodes().item(i);
                casAttribut.put(attribute.getNodeName(), attribute.getTextContent());
            }
        }
        return casAttribut;

    }

    /**
     * Method to get a proxy ticket.
     * 
     * @param username
     * @param serviceName
     * @return
     * @throws IOException
     * @throws SAXException
     * @throws ExecutionException
     * @throws InterruptedException
     */
    public static String getProxyTicket(String username, String serviceName) throws IOException, SAXException,
            InterruptedException, ExecutionException {
        String proxyTicket = null;

        // construction of the proxy validation URL
        String pgt = (String) Cache.get("pgt_" + username);
        String url = getCasProxyUrl() + "?pgt=" + pgt + "&targetService=" + serviceName;

        // we do the validation request
        Document response = WS.url(url).get().getXml();

        // search node "cas:authenticationSuccess"
        NodeList list = response.getElementsByTagName("cas:proxySuccess");

        // parse this response (use a lightweight approach for now)
        if (list.getLength() > 0) {
            proxyTicket = response.getElementsByTagName("cas:proxyTicket").item(0).getTextContent();
        }
        else {
            Logger.error("CAS server responded with error for request [" + url + "].  Full response was ["
                    + response.getElementsByTagName("cas:proxyFailure").item(0).getTextContent() + "]");
        }
        Logger.debug("[SecureCAS]: PT for user " + username + " and service " + serviceName + " is " + proxyTicket);
        return proxyTicket;
    }

}
