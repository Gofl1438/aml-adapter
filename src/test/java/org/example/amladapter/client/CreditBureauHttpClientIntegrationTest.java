package org.example.amladapter.client;

import org.example.amladapter.FakeBureau.WireMockBureauServer;
import org.example.amladapter.dto.CreditBureauRequest;
import org.example.amladapter.result.AmlCheckResult;
import org.junit.jupiter.api.*;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CreditBureauHttpClientIntegrationTest {

    private WireMockBureauServer fakeBureau;
    //используем адаптер, но используем поддельное бюро
    private CreditBureauHttpClient client;

    @BeforeAll
    void startFakeBureau() {
        fakeBureau = new WireMockBureauServer();
        fakeBureau.start();
    }

    @AfterAll
    void stopFakeBureau() {
        fakeBureau.stop();
    }

    @BeforeEach
    void setUp() {
        client = new CreditBureauHttpClient(new RestTemplate(), "http://localhost:8082/check");
        fakeBureau.reset();
    }

    @Test
    void testSuccessResponse() {
        fakeBureau.stubSuccess(true);
        CreditBureauRequest request = new CreditBureauRequest("Иванов Иван Иванович", "123", "456");

        AmlCheckResult result = client.checkStatus(request);

        assertInstanceOf(AmlCheckResult.Success.class, result);
        assertTrue(((AmlCheckResult.Success) result).amlStatus());
    }

    @Test
    void testErrorResponse() {
        fakeBureau.stubError();
        CreditBureauRequest request = new CreditBureauRequest("Иванов Иван Иванович", "123", "456");

        AmlCheckResult result = client.checkStatus(request);

        assertInstanceOf(AmlCheckResult.TechnicalError.class, result);
        assertEquals(AmlCheckResult.TechnicalError.ErrorCode.SERVICE_ERROR, ((AmlCheckResult.TechnicalError) result).code());
    }

    @Test
    void testTimeout() {
        fakeBureau.stubTimeout();
        CreditBureauRequest request = new CreditBureauRequest("Иванов Иван Иванович", "123", "456");

        // RestTemplate должен упасть по таймауту
        assertThrows(Exception.class, () -> client.checkStatus(request));
    }

    @Test
    void testFailFiveThenSuccess() {
        fakeBureau.stubFailFiveThenSuccess(true);
        CreditBureauRequest request = new CreditBureauRequest("Иванов Иван Иванович", "123", "456");

        AmlCheckResult result = client.checkStatus(request);

        assertInstanceOf(AmlCheckResult.Success.class, result);
        assertTrue(((AmlCheckResult.Success) result).amlStatus());
    }
}