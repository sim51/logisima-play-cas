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
import play.PlayPlugin;
import play.mvc.Router;

/**
 * Integrate CAS module to the play lifecycle, to add mock CAS server routes.
 * 
 * @author bsimard
 * 
 */
public class CASPlugin extends PlayPlugin {

    @Override
    public void onApplicationStart() {
        Logger.info("Module CAS conf : [Mock Server:" + CASUtils.isCasMockServer() + "]");
    }

    @Override
    public void onRoutesLoaded() {
        if (CASUtils.isCasMockServer()) {
            Logger.debug("adding routes for CAS Mock Server");
            Router.addRoute("GET", "/@cas/login", "modules.cas.MockServer.login");
            Router.addRoute("POST", "/@cas/authenticate", "modules.cas.MockServer.loginAction");
            Router.addRoute("GET", "/@cas/logout", "modules.cas.MockServer.logout");
            Router.addRoute("GET", "/@cas/serviceValidate", "modules.cas.MockServer.serviceValidate");
            Router.addRoute("GET", "/@cas/proxy", "modules.cas.MockServer.proxy");
        }
    }

}
