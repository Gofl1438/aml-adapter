package org.example.amladapter;

import org.example.amladapter.client.CreditBureauClient;
import org.example.amladapter.client.PortfolioClient;
import org.example.amladapter.dto.Client;
import org.example.amladapter.result.AmlCheckResult;
import org.example.amladapter.result.GetClientResult;
import org.example.amladapter.result.UpdateAmlResult;
import org.example.amladapter.service.CheckResult;
import org.example.amladapter.service.ClientCheckService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@SpringBootTest
class AmlAdapterApplicationTests {

    @MockBean
    private PortfolioClient portfolioClient;

    @MockBean
    private CreditBureauClient creditBureauClient;

    @Autowired
    private ClientCheckService service;

    private final long CLIENT_ID = 1L;

    @Test
    void contextLoads() {
        // Просто проверка, что контекст поднялся
    }

    //Проверка клиента, положительно
    @Test
    void checkClient_True_Bureau() {
        Client mockClient = new Client(CLIENT_ID, "Иванов", "Иван", "Иванович", "1234567890", "123-456-789-00", null);
        when(portfolioClient.getClient(CLIENT_ID)).thenReturn(new GetClientResult.Success(mockClient));
        when(creditBureauClient.checkStatus(any())).thenReturn(new AmlCheckResult.Success(true));
        when(portfolioClient.updateAmlStatus(CLIENT_ID, true)).thenReturn(new UpdateAmlResult.Success());

        CheckResult result = service.checkClient(CLIENT_ID);

        assertInstanceOf(CheckResult.Success.class, result);
        assertEquals(true, ((CheckResult.Success) result).amlStatus());
        verify(portfolioClient).updateAmlStatus(CLIENT_ID, true);
    }

    //должно вернуть false, проверка клиента
    @Test
    void checkClient_False_Bureau() {
        Client mockClient = new Client(CLIENT_ID, "Иванов", "Иван", "Иванович", "1234567890", "123-456-789-00", null);
        when(portfolioClient.getClient(CLIENT_ID)).thenReturn(new GetClientResult.Success(mockClient));
        when(creditBureauClient.checkStatus(any())).thenReturn(new AmlCheckResult.Success(false));
        when(portfolioClient.updateAmlStatus(CLIENT_ID, false)).thenReturn(new UpdateAmlResult.Success());

        CheckResult result = service.checkClient(CLIENT_ID);

        assertInstanceOf(CheckResult.Success.class, result);
        assertEquals(false, ((CheckResult.Success) result).amlStatus());
    }

    // Клиент не найден
    @Test
    void checkClient_ClientNotFound() {
        when(portfolioClient.getClient(CLIENT_ID)).thenReturn(new GetClientResult.NotFound());

        CheckResult result = service.checkClient(CLIENT_ID);

        assertInstanceOf(CheckResult.ClientNotFound.class, result);
        verify(creditBureauClient, never()).checkStatus(any());
        verify(portfolioClient, never()).updateAmlStatus(anyLong(), anyBoolean());
    }


    // Тест работы таймера, а именно блокировка проверки
    @Test
    void checkClient_Timer_Block() {
        Instant fourMinutesAgo = Instant.now().minusSeconds(60 * 4);
        Client mockClient = new Client(CLIENT_ID, "Иванов", "Иван", "Иванович", "1234567890", "123-456-789-00", fourMinutesAgo);
        when(portfolioClient.getClient(CLIENT_ID)).thenReturn(new GetClientResult.Success(mockClient));

        CheckResult result = service.checkClient(CLIENT_ID);

        assertInstanceOf(CheckResult.TooEarly.class, result);
        CheckResult.TooEarly tooEarly = (CheckResult.TooEarly) result;
        assertTrue(tooEarly.retryAfter() > 0 && tooEarly.retryAfter() <= 60);
        verify(creditBureauClient, never()).checkStatus(any());
        verify(portfolioClient, never()).updateAmlStatus(anyLong(), anyBoolean());
    }

    //Таймер после окончания таймера
    @Test
    void checkClient_Timer_Pass() {
        Instant sixMinutesAgo = Instant.now().minusSeconds(60 * 6);
        Client mockClient = new Client(CLIENT_ID, "Иванов", "Иван", "Иванович", "1234567890", "123-456-789-00", sixMinutesAgo);
        when(portfolioClient.getClient(CLIENT_ID)).thenReturn(new GetClientResult.Success(mockClient));
        when(creditBureauClient.checkStatus(any())).thenReturn(new AmlCheckResult.Success(true));
        when(portfolioClient.updateAmlStatus(CLIENT_ID, true)).thenReturn(new UpdateAmlResult.Success());

        CheckResult result = service.checkClient(CLIENT_ID);

        assertInstanceOf(CheckResult.Success.class, result);
        verify(creditBureauClient).checkStatus(any());
    }


    //ошибка, сервис недоступен
    @Test
    void checkClient_Bureau_Unavailable() {
        Client mockClient = new Client(CLIENT_ID, "Иванов", "Иван", "Иванович", "1234567890", "123-456-789-00", null);
        when(portfolioClient.getClient(CLIENT_ID)).thenReturn(new GetClientResult.Success(mockClient));
        when(creditBureauClient.checkStatus(any()))
                .thenReturn(new AmlCheckResult.TechnicalError(AmlCheckResult.TechnicalError.ErrorCode.SERVICE_ERROR));

        CheckResult result = service.checkClient(CLIENT_ID);

        assertInstanceOf(CheckResult.RetryRequired.class, result);
        verify(portfolioClient, never()).updateAmlStatus(anyLong(), anyBoolean());
    }

    //ошибка валидации
    @Test
    void checkClient_Bureau_ProcessingError() {
        Client mockClient = new Client(CLIENT_ID, "Иванов", "Иван", "Иванович", "1234567890", "123-456-789-00", null);
        when(portfolioClient.getClient(CLIENT_ID)).thenReturn(new GetClientResult.Success(mockClient));
        when(creditBureauClient.checkStatus(any()))
                .thenReturn(new AmlCheckResult.TechnicalError(AmlCheckResult.TechnicalError.ErrorCode.VALIDATION_ERROR));

        CheckResult result = service.checkClient(CLIENT_ID);

        assertInstanceOf(CheckResult.ProcessingError.class, result);
    }

    //ошибка обновления статуса
    @Test
    void checkClient_ServiceUnavailable() {
        Client mockClient = new Client(CLIENT_ID, "Иванов", "Иван", "Иванович", "1234567890", "123-456-789-00", null);
        when(portfolioClient.getClient(CLIENT_ID)).thenReturn(new GetClientResult.Success(mockClient));
        when(creditBureauClient.checkStatus(any())).thenReturn(new AmlCheckResult.Success(true));
        when(portfolioClient.markAmlCheckAttempt(CLIENT_ID)).thenReturn(new UpdateAmlResult.Success());
        when(portfolioClient.updateAmlStatus(CLIENT_ID, true)).thenReturn(new UpdateAmlResult.TechnicalError());

        CheckResult result = service.checkClient(CLIENT_ID);

        assertInstanceOf(
                CheckResult.RetryRequired.class,
                result
        );
    }

    //статус не найден
    @Test
    void checkClient_StatusNotFound() {
        Client mockClient = new Client(CLIENT_ID, "Иванов", "Иван", "Иванович", "1234567890", "123-456-789-00", null);
        when(portfolioClient.getClient(CLIENT_ID)).thenReturn(new GetClientResult.Success(mockClient));

        when(creditBureauClient.checkStatus(any())).thenReturn(new AmlCheckResult.Success(true));

        when(portfolioClient.markAmlCheckAttempt(CLIENT_ID)).thenReturn(new UpdateAmlResult.Success());

        when(portfolioClient.updateAmlStatus(CLIENT_ID, true)).thenReturn(new UpdateAmlResult.NotFound());

        CheckResult result = service.checkClient(CLIENT_ID);
        assertInstanceOf(CheckResult.RetryRequired.class, result);
    }

    // клиент не найден, сервис не доступен
    @Test
    void checkClient_GetClientTechnicalError() {
        when(portfolioClient.getClient(CLIENT_ID)).thenReturn(new GetClientResult.TechnicalError());

        CheckResult result = service.checkClient(CLIENT_ID);

        assertInstanceOf(CheckResult.ServiceUnavailable.class, result);
        verify(creditBureauClient, never()).checkStatus(any());
        verify(portfolioClient, never()).updateAmlStatus(anyLong(), anyBoolean());
    }

}