package com.henryplatform.telemetry.exception;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data @Builder
public class ApiError {
    private int status;
    private String message;
    // SEC: lista de erros de validação sem expor stack trace ou detalhes internos
    private List<String> errors;
    private LocalDateTime timestamp;
}
