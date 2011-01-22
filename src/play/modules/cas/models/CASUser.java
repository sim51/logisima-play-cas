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
package play.modules.cas.models;

import java.io.Serializable;
import java.util.Map;

/**
 * CAS user object.
 * 
 * @author bsimard
 * 
 */
public class CASUser implements Serializable {

    /**
     * 
     */
    private static final long   serialVersionUID = -7063462255687685373L;
    private String              username;
    private Map<String, String> attribut;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Map<String, String> getAttribut() {
        return attribut;
    }

    public void setAttribut(Map<String, String> attribut) {
        this.attribut = attribut;
    }

}
