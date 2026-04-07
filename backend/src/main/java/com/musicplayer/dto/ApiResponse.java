    package com.musicplayer.dto;

    import com.fasterxml.jackson.annotation.JsonInclude;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public class ApiResponse<T> {

        private boolean success;
        private String message;
        private T data;
        private String error;
        private long timestamp;

        public ApiResponse() {}

        public ApiResponse(boolean success, String message, T data, String error, long timestamp) {
            this.success = success;
            this.message = message;
            this.data = data;
            this.error = error;
            this.timestamp = timestamp;
        }

        public static <T> ApiResponse<T> success(T data) {
            return new ApiResponse<>(true, null, data, null, System.currentTimeMillis());
        }

        public static <T> ApiResponse<T> success(T data, String message) {
            return new ApiResponse<>(true, message, data, null, System.currentTimeMillis());
        }

        public static <T> ApiResponse<T> error(String errorMessage) {
            return new ApiResponse<>(false, null, null, errorMessage, System.currentTimeMillis());
        }

        public boolean isSuccess() {
            return success;
        }

        public void setSuccess(boolean success) {
            this.success = success;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public T getData() {
            return data;
        }

        public void setData(T data) {
            this.data = data;
        }

        public String getError() {
            return error;
        }

        public void setError(String error) {
            this.error = error;
        }

        public long getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(long timestamp) {
            this.timestamp = timestamp;
        }
    }