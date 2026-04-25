package com.alibaba.ocean.rawsdk.example.param;

public class CommonBusinessCatResult {

    private Long errno;
    private String error;
    private CBCResponseData[] data;

    /**
     * @return 返回错误码
     */
    public Long getErrno() {
        return errno;
    }

    /**
     * 设置返回错误码 *
     * <p>
     * 此参数必填
     */
    public void setErrno(Long errno) {
        this.errno = errno;
    }

    /**
     * @return 返回错误信息
     */
    public String getError() {
        return error;
    }

    /**
     * 设置返回错误信息 *
     * <p>
     * 此参数必填
     */
    public void setError(String error) {
        this.error = error;
    }

    /**
     * @return 返回数据
     */
    public CBCResponseData[] getData() {
        return data;
    }

    /**
     * 设置返回数据 *
     * <p>
     * 此参数必填
     */
    public void setData(CBCResponseData[] data) {
        this.data = data;
    }

}
