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

import play.mvc.Controller;

public class MockServer extends Controller {

    private final static String ST                = "ST-1856339-aA5Yuvrxzpv8Tau1cYQ7";
    private final static String PT                = "PT-1856376-1HMgO86Z2ZKeByc5XdYD";
    private final static String PGT               = "PGT-490649-W81Y9Sa2vTM7hda7xNTkezTbVge4CUsybAr";
    private final static String PGTIOU            = "PGTIOU-84678-8a9d";
    private final static String serviceValidateOK = "<cas:serviceResponse xmlns:cas='http://www.yale.edu/tp/cas'><cas:authenticationSuccess><cas:user>$$username$$</cas:user><cas:proxyGrantingTicket>$$PGTIOU$$</cas:proxyGrantingTicket></cas:authenticationSuccess></cas:serviceResponse>";
    private final static String serviceValidateKO = "<cas:serviceResponse xmlns:cas='http://www.yale.edu/tp/cas'><cas:authenticationFailure code=\"INVALID_TICKET\">Ticket ST-1856339-aA5Yuvrxzpv8Tau1cYQ7 not recognized</cas:authenticationFailure></cas:serviceResponse>";
    private final static String proxyValidateOK   = "<cas:serviceResponse xmlns:cas='http://www.yale.edu/tp/cas'><cas:authenticationSuccess><cas:user>username</cas:user><cas:proxyGrantingTicket>PGTIOU-84678-8a9d...</cas:proxyGrantingTicket><cas:proxies><cas:proxy>https://proxy2/pgtUrl</cas:proxy></cas:proxies></cas:authenticationSuccess></cas:serviceResponse>";
    private final static String proxyValidateKO   = "<cas:serviceResponse xmlns:cas='http://www.yale.edu/tp/cas'><cas:authenticationFailure code=\"INVALID_TICKET\">ticket PT-1856376-1HMgO86Z2ZKeByc5XdYD not recognized</cas:authenticationFailure></cas:serviceResponse>";
    private final static String proxyOK           = "<cas:serviceResponse xmlns:cas='http://www.yale.edu/tp/cas'><cas:proxySuccess><cas:proxyTicket>PT-1856392-b98xZrQN4p90ASrw96c8</cas:proxyTicket></cas:proxySuccess></cas:serviceResponse>";
    private final static String proxyKO           = "<cas:serviceResponse xmlns:cas='http://www.yale.edu/tp/cas'><cas:proxyFailure code=\"INVALID_REQUEST\">'pgt' and 'targetService' parameters are both required</cas:proxyFailure></cas:serviceResponse>";

    public static void login() {
        String serviceUrl = params.get("serviceUrl");
        render(serviceUrl);
    }

    public static void loginAction() {
        String login = params.get("login");
        String password = params.get("password");
        String serviceUrl = params.get("serviceUrl");

        if (login.equals(password)) {
            redirect(serviceUrl + "&ticket=" + ST);
        }
    }

    public static void logout() {
        render();
    }

    public static void serviceValidate() {
        renderXml(serviceValidateOK);
    }

    public static void proxy() {
        renderXml(proxyOK);
    }

    public static void proxyValidate() {
        renderXml(proxyValidateOK);
    }
}
