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
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

import play.Logger;
import play.Play;
import play.cache.Cache;
import play.modules.cas.models.CASUser;
import play.mvc.Router;
import edu.yale.its.tp.cas.client.ServiceTicketValidator;
import edu.yale.its.tp.cas.proxy.ProxyTicketReceptor;
import edu.yale.its.tp.cas.util.SecureURL;

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
        String casLoginUrl = Play.configuration.getProperty("cas.loginUrl");

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
        return Play.configuration.getProperty("cas.logoutUrl");
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
     * Method that return proxy call back url.
     * 
     * @throws Throwable
     */
    private static String getCasProxyCallBackUrl() {
        String casProxyCallBackUrl = Play.configuration.getProperty("application.url");
        casProxyCallBackUrl += Router.reverse("modules.cas.SecureCAS.pgtCallBack").url;
        return casProxyCallBackUrl;
    }
    
    /**
     * Method that return cas proxy url
     * @return
     */
    private static String getCasProxyUrl(){
        String casProxyUrl = Play.configuration.getProperty("cas.proxyUrl");
        return casProxyUrl;
    }

    /**
     * Method that verify if the cas ticket is valid.
     * 
     * @param ticket
     *            cas tickets
     * @throws ParserConfigurationException
     * @throws SAXException
     * @throws IOException
     * @throws Throwable
     */
    public static CASUser valideCasTicket(String ticket) throws IOException, SAXException, ParserConfigurationException {
        // Instantiate a new ServiceTicketValidator
        ServiceTicketValidator sv = new ServiceTicketValidator();

        // Set its parameters
        sv.setCasValidateUrl(Play.configuration.getProperty("cas.validateUrl"));
        sv.setProxyCallbackUrl(getCasProxyCallBackUrl());
        sv.setService(getCasServiceUrl());
        sv.setServiceTicket(ticket);
        // ticket validation
        sv.validate();

        // we retrieve CAS user from the response
        CASUser user = null;
        if (sv.isAuthenticationSuccesful()) {
            Map<String, String> casAttribut = null;
            casAttribut = getCasAttributes(sv.getResponse());
            // we populate the CASUser
            user = new CASUser();
            user.setUsername(sv.getUser());
            user.setAttribut(casAttribut);
            
            // here we get PGT from cache
            String pgt = (String) Cache.get(sv.getPgtIou());
            Cache.delete(sv.getPgtIou());

            // we put in cache PGT with PGT_username
            Cache.add("pgt_" + user.getUsername(), pgt);
        }

        return user;
    }
    
    private static Map<String, String> getCasAttributes(String xml) throws SAXException{
        Map<String, String> casAttribut = null; 
        if (xml.indexOf("<cas:attributes>") != -1) {
            XMLReader reader = XMLReaderFactory.createXMLReader();
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
     */
    public static String getProxyTicket(String username, String serviceName) throws IOException {
        String proxyTicket = null;
        String pgt = (String) Cache.get("pgt_" + username);
        String url = getCasProxyUrl() + "?pgt=" + pgt + "&targetService=" + serviceName;
        String response = SecureURL.retrieve(url);

        // parse this response (use a lightweight approach for now)
        if (response.indexOf("<cas:proxySuccess>") != -1 && response.indexOf("<cas:proxyTicket>") != -1) {
            int startIndex = response.indexOf("<cas:proxyTicket>") + "<cas:proxyTicket>".length();
            int endIndex = response.indexOf("</cas:proxyTicket>");
            proxyTicket = response.substring(startIndex, endIndex);
        } else {
            Logger.error("CAS server responded with error for request [" + url + "].  Full response was [" + response + "]");
        }
        Logger.debug("[SecureCAS]: PT for user " + username + " and service " + serviceName + " is " + proxyTicket);
        return proxyTicket;
    }

}
