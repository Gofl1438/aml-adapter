package org.example.amladapter.controller;

import org.example.amladapter.service.CheckResult;
import org.example.amladapter.service.ClientCheckService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/clients")
public class ClientCheckController {
    private final ClientCheckService service;

    public ClientCheckController(ClientCheckService service) {
        this.service = service;
    }

    @PostMapping("/{id}/check")
    public ResponseEntity<?> checkClient(@PathVariable long id){
        CheckResult result = service.checkClient(id);

        if(result instanceof CheckResult.TooEarly tooEarly){
            return ResponseEntity
                    .status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(Map.of(
                            "error", "Повторная проверка пока недоступна",
                            "retryAfter", tooEarly.retryAfter()
                    ));
        }

        if(result instanceof  CheckResult.ClientNotFound){
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(Map.of(
                             "error", "Клиент не найден"
                    ));
        }

        if (result instanceof CheckResult.RetryRequired retryRequired) {
            return ResponseEntity
                    .status(HttpStatus.BAD_GATEWAY)
                    .body(Map.of(
                            "error", "Результат не определен.",
                            "retryAfter", retryRequired.retryAfter()
                    ));
        }

        if (result instanceof CheckResult.ProcessingError) {
            return ResponseEntity
                    .status(HttpStatus.BAD_GATEWAY)
                    .body(Map.of(
                            "error", "Не удалось выполнить проверку клиента"
                    ));
        }

        if(result instanceof  CheckResult.ServiceUnavailable){
            return ResponseEntity
                    .status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of(
                            "error", "Сервис проверки временно недоступен"
                    ));
        }

        CheckResult.Success successResult = (CheckResult.Success)result;

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(Map.of(
                        "amlStatus", successResult.amlStatus()
                ));
    }
}
