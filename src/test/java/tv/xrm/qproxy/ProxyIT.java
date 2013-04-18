package tv.xrm.qproxy;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.github.tomakehurst.wiremock.stubbing.Scenario;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.Response;
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

    private final AsyncHttpClient client = new AsyncHttpClient();

    @ClassRule
    public static WireMockRule wireMockRule = new WireMockRule(MOCK_SERVER_PORT);

    @Test
    public void forwardsSimplePost() throws Exception {
        final String body = "\"El veloz murciélago hindú comía feliz cardillo y kiwi. La cigüeña tocaba el saxofón detrás del palenque de paja.\"";

        Response response = client.preparePost(PROXY_SERVER_BASE_URL)
                .addQueryParameter("uri", MOCK_SERVER_BASE_URL + "boo")
                .setBody(body)
                .addHeader("Content-Type", "application/json")
                .addHeader("Foo-Header", "foo")
                .execute().get();

        assertEquals(202, response.getStatusCode());

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

        Response response = client.preparePost(PROXY_SERVER_BASE_URL)
                .addQueryParameter("uri", MOCK_SERVER_BASE_URL)
                .setBody(body)
                .addHeader("Content-Type", "application/json")
                .addHeader("Foo-Header", "foo")
                .execute().get();

        assertEquals(202, response.getStatusCode());

        Thread.sleep(5000);

        // there seems to be no way to ask WireMock directly for the current state of a scenario, so do this:
        assertEquals(208, client.preparePost(MOCK_SERVER_BASE_URL).execute().get().getStatusCode());
    }


    @Test
    public void rejectsMalformedUri() throws Exception {
        final String body = "\"El veloz murciélago hindú comía feliz cardillo y kiwi.\"";

        Response response = client.preparePost(PROXY_SERVER_BASE_URL)
                .addQueryParameter("uri", MOCK_SERVER_BASE_URL.replace('/', '\\'))
                .setBody(body)
                .execute().get();

        assertEquals(400, response.getStatusCode());
    }

    @Test
    public void rejectsMissingUri() throws Exception {
        final String body = "\"boo\"";

        Response response = client.preparePost("http://localhost:" + PROXY_SERVER_PORT + "/")
                .setBody(body)
                .execute().get();

        assertEquals(400, response.getStatusCode());
    }

}
