package play.module.cas;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

import javax.xml.parsers.ParserConfigurationException;

import org.junit.Test;
import org.xml.sax.SAXException;

import play.Play;
import play.modules.cas.CASUtils;
import play.modules.cas.models.CASUser;
import play.test.UnitTest;

public class CASUtilsTest extends UnitTest {

    @Test
    public void getCasLoginUrlTest1() {
        Play.configuration.setProperty("cas.mockserver", "true");
        Play.configuration.setProperty("cas.gateway", "false");
        String casLoginUrl = CASUtils.getCasLoginUrl(Boolean.TRUE);
        assertEquals(casLoginUrl, "http://localhost:9000/@cas/login?service=http://localhost:9000/authenticate");
        Play.configuration.setProperty("cas.gateway", "true");
        casLoginUrl = CASUtils.getCasLoginUrl(Boolean.TRUE);
        assertEquals(casLoginUrl,
                "http://localhost:9000/@cas/login?service=http://localhost:9000/authenticate&gateway=true");
    }

    @Test
    public void getCasLoginUrlTest2() {
        Play.configuration.setProperty("cas.mockserver", "false");
        String casLoginUrl = CASUtils.getCasLoginUrl(Boolean.FALSE);
        assertEquals(casLoginUrl, "http://www.logisima.com/cas/login?service=http://localhost:9000/authenticate");
        casLoginUrl = CASUtils.getCasLoginUrl(Boolean.TRUE);
        assertEquals(casLoginUrl,
                "http://www.logisima.com/cas/login?service=http://localhost:9000/authenticate&gateway=true");
    }

    @Test
    public void getCasLogoutUrlTest() {
        Play.configuration.setProperty("cas.mockserver", "false");
        String casLogoutUrl = CASUtils.getCasLogoutUrl();
        assertEquals(casLogoutUrl, "http://www.logisima.com/cas/logout");
    }

    @Test
    public void getCasLogoutUrlTest2() {
        Play.configuration.setProperty("cas.mockserver", "true");
        String casLogoutUrl = CASUtils.getCasLogoutUrl();
        assertEquals(casLogoutUrl, "http://localhost:9000/@cas/logout");
    }

    @Test
    public void isMockSeverTest1() {
        Play.configuration.setProperty("cas.mockserver", "true");
        assertTrue(CASUtils.isCasMockServer());
    }

    @Test
    public void isMockSeverTest2() {
        Play.configuration.setProperty("cas.mockserver", "false");
        assertFalse(CASUtils.isCasMockServer());
    }

    @Test
    public void validateCasTicketTest() throws IOException, SAXException, ParserConfigurationException,
            InterruptedException, ExecutionException {
        Play.configuration.setProperty("cas.mockserver", "true");
        CASUser user = CASUtils.valideCasTicket("123");
        assertTrue(user == null);
    }

}
