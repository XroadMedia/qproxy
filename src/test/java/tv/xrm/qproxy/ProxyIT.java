package tv.xrm.qproxy;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.github.tomakehurst.wiremock.stubbing.Scenario;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.util.StringContentProvider;
import org.eclipse.jetty.http.HttpField;
import org.eclipse.jetty.http.HttpHeader;
import org.junit.ClassRule;
import org.junit.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.Assert.assertEquals;

/**
 * Integration/system tests for the proxy. These assume that the proxy is running on localhost.
 */
public class ProxyIT {

    private static final int MOCK_SERVER_PORT = 8088;
    public static final String MOCK_SERVER_BASE_URL = "http://localhost:" + MOCK_SERVER_PORT + "/";

    private static final int PROXY_SERVER_PORT = 8080;
    public static final String PROXY_SERVER_BASE_URL = "http://localhost:" + PROXY_SERVER_PORT + "/";

    private final HttpClient client = new HttpClient();
    {
        try {
            client.setUserAgentField(new HttpField(HttpHeader.USER_AGENT, "integration test client"));
            client.start();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @ClassRule
    public static WireMockRule wireMockRule = new WireMockRule(MOCK_SERVER_PORT);

    @Test
    public void forwardsSimplePost() throws Exception {
        final String body = "\"El veloz murciélago hindú comía feliz cardillo y kiwi. La cigüeña tocaba el saxofón detrás del palenque de paja.\"";

        ContentResponse response = client.POST(PROXY_SERVER_BASE_URL).param("url", MOCK_SERVER_BASE_URL + "boo")
                .content(new StringContentProvider(body))
                .header("Content-Type", "application/json; charset=UTF-8")
                .header("Foo-Header", "foo")
                .send();

        assertEquals(202, response.getStatus());

        Thread.sleep(500);

        verify(postRequestedFor(urlMatching("/boo")).withHeader("Foo-Header", equalTo("foo")).withRequestBody(equalTo(body)));
    }

    @Test
    public void retriesSimplePost() throws Exception {
        final String body = "\"El veloz murciélago hindú comía feliz cardillo y kiwi. La cigüeña tocaba el saxofón detrás del palenque de paja.\"";

        stubFor(post(urlMatching("/"))
                .inScenario("retry")
                .whenScenarioStateIs(Scenario.STARTED)
                .willReturn(aResponse().withStatus(503))
                .willSetStateTo("retry1"));

        stubFor(post(urlMatching("/"))
                .inScenario("retry")
                .whenScenarioStateIs("retry1")
                .willReturn(aResponse().withStatus(202))
                .willSetStateTo("retryN"));

        stubFor(post(urlMatching("/"))
                .inScenario("retry")
                .whenScenarioStateIs("retryN")
                .willReturn(aResponse().withStatus(208)));

        ContentResponse response = client.POST(PROXY_SERVER_BASE_URL)
                .param("url", MOCK_SERVER_BASE_URL)
                .content(new StringContentProvider(body))
                .header("Content-Type", "application/json; charset=UTF-8")
                .header("Foo-Header", "foo")
                .send();

        assertEquals(202, response.getStatus());

        Thread.sleep(5000);

        verify(2, postRequestedFor(urlMatching("/")).withHeader("Foo-Header", equalTo("foo")).withRequestBody(equalTo(body)));

        // there seems to be no way to ask WireMock directly for the current state of a scenario, so do this:
        assertEquals(208, client.POST(MOCK_SERVER_BASE_URL).send().getStatus());
    }


    @Test
    public void rejectsMalformedUri() throws Exception {
        final String body = "\"El veloz murciélago hindú comía feliz cardillo y kiwi.\"";

        ContentResponse response = client.POST(PROXY_SERVER_BASE_URL)
                .param("url", MOCK_SERVER_BASE_URL.replace('/', '\\'))
                .content(new StringContentProvider(body))
                .send();

        assertEquals(400, response.getStatus());
    }

    @Test
    public void rejectsMissingUri() throws Exception {
        final String body = "\"boo\"";

        ContentResponse response = client.POST("http://localhost:" + PROXY_SERVER_PORT + "/")
                .content(new StringContentProvider(body))
                .send();

        assertEquals(400, response.getStatus());
    }

}
