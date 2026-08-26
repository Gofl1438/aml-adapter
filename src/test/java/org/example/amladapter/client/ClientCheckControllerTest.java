package org.example.amladapter.client;

import org.example.amladapter.controller.ClientCheckController;
import org.example.amladapter.service.CheckResult;
import org.example.amladapter.service.ClientCheckService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ClientCheckController.class)
class ClientCheckControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ClientCheckService service;

    @Test
    void shouldReturnRetryRequired() throws Exception {
        when(service.checkClient(1L)).thenReturn(new CheckResult.RetryRequired(300));

        mockMvc.perform(post("/api/v1/clients/1/check"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.error").value("Результат не определен."))
                .andExpect(jsonPath("$.retryAfter").value(300));
    }
}