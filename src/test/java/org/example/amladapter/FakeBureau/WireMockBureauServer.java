package org.example.amladapter.FakeBureau;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED;

public class WireMockBureauServer {
    private WireMockServer server;

    public void start() {
        server = new WireMockServer(wireMockConfig().port(8082));
        server.start();
        configureFor("localhost", 8082);
    }

    public void stop() {
        if (server != null) {
            server.stop();
        }
    }

    public void reset() {
        if (server != null) {
            server.resetAll();
        }
    }

    // === СЦЕНАРИИ ===

    // 1. Успешный ответ (amlStatus = true/false)
    public void stubSuccess(boolean amlStatus) {
        String xml = "<checkClientResponse><amlStatus>" + amlStatus + "</amlStatus></checkClientResponse>";
        stubFor(post("/check")
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/xml")
                        .withBody(xml)));
    }

    // 2. Ошибка (SOAP Fault)
    public void stubError() {
        String xml = "<soap:Envelope><soap:Body><soap:Fault><faultcode>soap:Server</faultcode><faultstring>Service unavailable</faultstring></soap:Fault></soap:Body></soap:Envelope>";
        stubFor(post("/check")
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/xml")
                        .withBody(xml)));
    }

    // 3. Таймаут (молчит)
    public void stubTimeout() {
        stubFor(post("/check")
                .willReturn(aResponse()
                        .withFixedDelay(1000) // 1 секунда
                        .withStatus(200)
                        .withBody("<checkClientResponse><amlStatus>true</amlStatus></checkClientResponse>")));
    }


    // 4. Падает 4 раза подряд, потом успех
    public void stubFailFourThenSuccess(boolean amlStatus) {
        String successXml = "<checkClientResponse><amlStatus>" + amlStatus + "</amlStatus></checkClientResponse>";

        String errorXml = "<soap:Envelope><soap:Body><soap:Fault>" +
                            "<faultcode>soap:Server</faultcode>" +
                            "<faultstring>Service unavailable</faultstring>" +
                        "</soap:Fault></soap:Body></soap:Envelope>";

        stubFor(post("/check")
                .inScenario("bureau-retry")
                .whenScenarioStateIs(STARTED)
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/xml")
                        .withBody(errorXml))
                .willSetStateTo("ATTEMPT_2"));

        stubFor(post("/check")
                .inScenario("bureau-retry")
                .whenScenarioStateIs("ATTEMPT_2")
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/xml")
                        .withBody(errorXml))
                .willSetStateTo("ATTEMPT_3"));

        stubFor(post("/check")
                .inScenario("bureau-retry")
                .whenScenarioStateIs("ATTEMPT_3")
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/xml")
                        .withBody(errorXml))
                .willSetStateTo("ATTEMPT_4"));

        stubFor(post("/check")
                .inScenario("bureau-retry")
                .whenScenarioStateIs("ATTEMPT_4")
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/xml")
                        .withBody(successXml)));
    }
}