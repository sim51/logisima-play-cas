package controllers;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

import org.xml.sax.SAXException;

import play.modules.cas.CASUtils;
import play.modules.cas.annotation.Check;
import play.modules.cas.models.CASUser;
import play.mvc.Controller;
import play.mvc.With;
import controllers.modules.cas.SecureCAS;

@With(SecureCAS.class)
public class Private extends Controller {

    public static void checkAuthentication() {
        render();
    }

    @Check("role1")
    public static void checkRightOk() {
        render();
    }

    @Check("role2")
    public static void checkRightKo() {
        render();
    }

    public static void proxyTicket() throws IOException, SAXException, InterruptedException, ExecutionException {
        CASUser user = (CASUser) MySecurity.connected();
        String PT = CASUtils.getProxyTicket(user.getUsername(), "logisima");
        render(PT);
    }
}
