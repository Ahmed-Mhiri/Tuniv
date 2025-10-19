package com.tuniv.backend.shared.dto;

// If you use Jackson for JSON, you might need @JsonProperty
// import com.fasterxml.jackson.annotation.JsonProperty;

public record ApiResponse(
    // @JsonProperty("success") // <-- Uncomment this if you need the JSON field to be "success"
    boolean isSuccess, // <-- Renamed from "success" to "isSuccess"
    String message
) {

    /**
     * Creates a success response with a custom message.
     * This will now work correctly.
     */
    public static ApiResponse success(String message) {
        // Note: pass 'true' to the 'isSuccess' field
        return new ApiResponse(true, message);
    }

    /**
     * Creates a standard success response with a default message.
     */
    public static ApiResponse success() {
        return new ApiResponse(true, "Operation completed successfully");
    }

    /**
     * Creates an error response with a custom message.
     * (This one was fine, but we update it to use 'isSuccess')
     */
    public static ApiResponse error(String message) {
        return new ApiResponse(false, message);
    }
}