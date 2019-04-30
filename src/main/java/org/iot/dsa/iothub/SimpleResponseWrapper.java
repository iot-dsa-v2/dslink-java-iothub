package org.iot.dsa.iothub;

import org.iot.dsa.dslink.restadapter.ResponseWrapper;
import org.iot.dsa.time.DSDateTime;

public class SimpleResponseWrapper implements ResponseWrapper {
    
    private int code;
    private String data;
    private DSDateTime ts;
    
    public SimpleResponseWrapper(int code, String data, DSDateTime ts) {
        this.code = code;
        this.data = data;
        this.ts = ts;
    }

    @Override
    public int getCode() {
        return code;
    }

    @Override
    public String getData() {
        return data;
    }

    @Override
    public DSDateTime getTS() {
        return ts;
    }

}
