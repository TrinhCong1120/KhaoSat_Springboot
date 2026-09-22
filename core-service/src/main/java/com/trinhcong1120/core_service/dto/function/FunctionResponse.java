package com.trinhcong1120.core_service.dto.function;

public class FunctionResponse {

    private Integer id;
    private String name;
    private String code;

    public FunctionResponse() {
    }

    public FunctionResponse(
            Integer id,
            String name,
            String code) {

        this.id = id;
        this.name = name;
        this.code = code;
    }

    public Integer getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getCode() {
        return code;
    }
}