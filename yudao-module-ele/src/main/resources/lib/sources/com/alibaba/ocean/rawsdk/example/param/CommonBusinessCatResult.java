package com.alibaba.ocean.rawsdk.example.param;

public class CommonBusinessCatResult {

    private Long errno;
    private String error;
    private CBCResponseData[] data;

    
    public Long getErrno() {
        return errno;
    }

    
    public void setErrno(Long errno) {
        this.errno = errno;
    }

    
    public String getError() {
        return error;
    }

    
    public void setError(String error) {
        this.error = error;
    }

    
    public CBCResponseData[] getData() {
        return data;
    }

    
    public void setData(CBCResponseData[] data) {
        this.data = data;
    }

}
