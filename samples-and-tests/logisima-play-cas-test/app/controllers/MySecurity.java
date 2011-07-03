package controllers;

import play.Logger;
import play.cache.Cache;
import play.modules.cas.models.CASUser;
import controllers.modules.cas.Security;

public class MySecurity extends Security {

    static boolean check(String profile) {
        Logger.debug("[MySecurity]: check :" + profile);
        return profile.equals("role1");
    }

    static void onAuthenticated(CASUser user) {
        Logger.debug("[MySecurity]: onAutenticated method");
        Cache.set(session.get("username"), user);
    }

    static void onDisconnected() {
        Logger.debug("[MySecurity]: onAutenticated method");
        Cache.delete(session.get("username"));
        session.clear();
    }

    public static Object connected() {
        Logger.debug("[MySecurity]: onAutenticated method");
        return Cache.get(session.get("username"));
    }

}
