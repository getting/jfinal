package com.jfinal.restful;

import com.jfinal.core.Action;

/**
 * Created by iaceob on 2017/3/18.
 */
public class MappingRecord {

    private String uri;
    private Method method;
    private Action action;
    private String regex;
    private String[] parakeys;

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

    public String getRegex() {
        return regex;
    }

    public void setRegex(String regex) {
        this.regex = regex;
    }

    public String[] getParakeys() {
        return parakeys;
    }

    public void setParakeys(String[] parakeys) {
        this.parakeys = parakeys;
    }
}
