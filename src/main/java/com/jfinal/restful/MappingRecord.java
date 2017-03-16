package com.jfinal.restful;

import com.jfinal.core.Action;

/**
 * Created by iaceob on 2017/3/16.
 */
public class MappingRecord {

    private String uri;
    private Method method;
    private Action action;

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public Method getMethod() {
        return method;
    }

    public void setMethod(Method method) {
        this.method = method;
    }

    public Action getAction() {
        return action;
    }

    public void setAction(Action action) {
        this.action = action;
    }

    @Override
    public String toString() {
        return "MappingRecord{" +
                "uri='" + uri + '\'' +
                ", method=" + method +
                ", action=" + action +
                '}';
    }
}
