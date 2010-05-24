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

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import edu.yale.its.tp.cas.client.ServiceTicketValidator;
import play.Logger;
import play.Play;
import play.modules.cas.models.CASUser;
import play.mvc.Router;
import play.mvc.Http.Request;

/**
 * Utils class for CAS.
 * 
 * @author bsimard
 * 
 */
public class CASUtils {

    /**
     * Method that generate the CAS login page URL.
     * @param request 
     * 
     * @param possibleGateway
     * @throws Throwable
     */
    public static String getCasLoginUrl(Boolean possibleGateway) {
        String casLoginUrl = Play.configuration.getProperty("cas.loginUrl");

        // we add the service URL (the reverse route for SecureCas.
        casLoginUrl += "?service=";
        casLoginUrl += getCasServiceUrl();

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
     * Method that return the service url.
     * 
     * @throws Throwable
     */
    public static String getCasServiceUrl() {
        String casServiceUrl = Play.configuration.getProperty("application.url");
        casServiceUrl += Router.reverse("modules.cas.SecureCAS.authenticate").url;
        return casServiceUrl;
    }

    /**
     * Method that verify if the cas ticket is valid.
     * 
     * @param ticket cas tickets
     * @throws ParserConfigurationException 
     * @throws SAXException 
     * @throws IOException 
     * @throws Throwable
     */
    public static CASUser valideCasTicket(Request request,String ticket) throws IOException, SAXException, ParserConfigurationException {
        // Instantiate a new ServiceTicketValidator
        ServiceTicketValidator sv = new ServiceTicketValidator();
        
        // Set its parameters
        sv.setCasValidateUrl(Play.configuration.getProperty("cas.validateUrl"));
        sv.setService(getCasServiceUrl());
        sv.setServiceTicket(ticket);
        sv.validate();

        // we retrieve CAS user from the response
        CASUser user = null;
        if (sv.isAuthenticationSuccesful()) {
            user = new CASUser();
            user.setUsername(sv.getUser());
        }
        
        return user;
    }

}
