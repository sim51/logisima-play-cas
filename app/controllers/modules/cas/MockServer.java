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

import java.io.IOException;
import java.util.concurrent.ExecutionException;

import play.Logger;
import play.cache.Cache;
import play.libs.Codec;
import play.libs.WS;
import play.mvc.Controller;

public class MockServer extends Controller {

    private final static String serviceValidateOK = "<cas:serviceResponse xmlns:cas='http://www.yale.edu/tp/cas'><cas:authenticationSuccess><cas:user>_LOGIN_</cas:user><cas:proxyGrantingTicket>_PGTIOU_</cas:proxyGrantingTicket></cas:authenticationSuccess></cas:serviceResponse>";
    private final static String serviceValidateKO = "<cas:serviceResponse xmlns:cas='http://www.yale.edu/tp/cas'><cas:authenticationFailure code=\"INVALID_TICKET\">_TICKET_ not recognized</cas:authenticationFailure></cas:serviceResponse>";
    private final static String proxyOK           = "<cas:serviceResponse xmlns:cas='http://www.yale.edu/tp/cas'><cas:proxySuccess><cas:proxyTicket>_PT_</cas:proxyTicket></cas:proxySuccess></cas:serviceResponse>";
    private final static String proxyKO           = "<cas:serviceResponse xmlns:cas='http://www.yale.edu/tp/cas'><cas:proxyFailure code=\"INVALID_REQUEST\">'pgt' and 'targetService' parameters are both required</cas:proxyFailure></cas:serviceResponse>";

    public static void login() {
        Logger.debug("[MockCAS]: login page");
        String serviceUrl = request.params.get("service");
        Logger.debug("[MockCAS]: Service URL is ", serviceUrl);
        render(serviceUrl);
    }

    public static void loginAction() {
        Logger.debug("[MockCAS]: validate credential");
        String login = params.get("login");
        String password = params.get("password");
        String serviceUrl = params.get("serviceUrl");
        String ST = "ST-" + Codec.UUID();
        Cache.set(ST, login, "1h");
        if (login.equals(password)) {
            Logger.debug("[MockCAS]: redirect to " + serviceUrl + "?ticket=" + ST);
            redirect(serviceUrl + "?ticket=" + ST);
        }
        else {
            flash.keep();
            flash.error("FAIL : login and password are not the same !");
            login();
        }
    }

    public static void logout() {
        render();
    }

    public static void serviceValidate() throws InterruptedException, ExecutionException, IOException {
        String ST = params.get("ticket");
        Logger.debug("[MockCAS]: service validate for ticket " + ST);
        String login = (String) Cache.get(ST);
        if (login != null && !login.equals("")) {
            String PGTIOU = "PGT-IOU" + Codec.UUID();
            String PGT = "PGT-" + Codec.UUID();
            Cache.set(PGT, login, "1h");
            if (params.get("pgtUrl") != null && !params.get("pgtUrl").isEmpty()) {
                String pgtUrl = params.get("pgtUrl");
                // we create a http client
                Logger.debug("[MockCAS]: send PGT via  " + pgtUrl + "?pgtIou=" + PGTIOU + "&pgtId=" + PGT);
                WS.url(pgtUrl + "?pgtIou=" + PGTIOU + "&pgtId=" + PGT).get();
            }
            Logger.debug("[MockCAS]: ticket " + ST + " is valid");
            renderXml(serviceValidateOK.replaceFirst("_LOGIN_", login).replaceFirst("_PGTIOU_", PGTIOU));
        }
        else {
            Logger.debug("[MockCAS]: ticket " + ST + " is not valid");
            renderXml(serviceValidateKO.replaceFirst("_TICKET_", ST));
        }
    }

    public static void proxy() {
        String PGT = params.get("pgt");
        String PT = "PT-" + Codec.UUID();
        String login = (String) Cache.get(PGT);
        Cache.set(PT, login, "1h");
        renderXml(proxyOK.replaceFirst("_PT_", PT));
    }

}
