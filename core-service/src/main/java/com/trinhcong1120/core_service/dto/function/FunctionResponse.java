package com.trinhcong1120.core_service.dto.function;

import java.util.UUID;

public class FunctionResponse {

    private UUID id;
    private String name;
    private String code;

    public FunctionResponse() {
    }

    public FunctionResponse(
            UUID id,
            String name,
            String code) {

        this.id = id;
        this.name = name;
        this.code = code;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getCode() {
        return code;
    }
}