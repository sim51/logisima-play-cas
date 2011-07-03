package controllers;

import play.Logger;
import play.cache.Cache;
import play.modules.cas.models.CASUser;
import play.mvc.Scope;
import controllers.modules.cas.Security;

public class MySecurity extends Security {

    public static boolean check(String profile) {
        Logger.debug("[MySecurity]: check :" + profile);
        return profile.equals("role1");
    }

    public static void onAuthenticated(CASUser user) {
        Logger.debug("[MySecurity]: onAutenticated method");
        Cache.set(Scope.Session.current().get("username"), user);
    }

    public static void onDisconnected() {
        Logger.debug("[MySecurity]: onAutenticated method");
        Cache.delete(Scope.Session.current().get("username"));
        Scope.Session.current().clear();
    }

    public static Object connected() {
        Logger.debug("[MySecurity]: onAutenticated method");
        return Cache.get(Scope.Session.current().get("username"));
    }
}
