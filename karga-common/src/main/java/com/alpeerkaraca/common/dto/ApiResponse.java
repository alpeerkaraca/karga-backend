package com.alpeerkaraca.common.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Setter
@Getter
public class ApiResponse<T> {
    private boolean success;
    private String message;
    private T data;

    public static <T> ApiResponse<T> success(T data, String message) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setSuccess(true);
        response.setData(data);
        response.setMessage(message);
        return response;
    }

    public static <T> ApiResponse<T> error(String message) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setSuccess(false);
        response.setMessage(message);
        response.setData(null);
        return response;
    }


    public static <T> ApiResponse<T> errorWithDetails(String s, Map<String, String> errors) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setSuccess(false);
        response.setData(null);
        StringBuilder messageBuilder = new StringBuilder();
        messageBuilder.append(s).append(" ");
        errors.forEach((k, v) ->
                messageBuilder.append(v).append(", ")
        );
        String message = messageBuilder.toString();
        response.setMessage(message);
        return response;
    }
}
