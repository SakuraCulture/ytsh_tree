package com.alibaba.ocean.rawsdk.client.exception;

public class OceanException extends Exception {

    
    private static final long serialVersionUID = -1828858210195741131L;

    protected String errorCode;

    protected String errorMessage;

    public OceanException() {
    }

    
    public OceanException(String message) {
        super(message);
        this.errorMessage = message;
    }

    
    public OceanException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.errorMessage = message;
    }

    
    public OceanException(Throwable cause) {
        super(cause);
    }

    
    public OceanException(String message, Throwable cause) {
        super(message, cause);
    }

    
    public String getErrorCode() {
        return errorCode;
    }

    
    public String getErrorMessage() {
        return errorMessage;
    }
}
