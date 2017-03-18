package com.jfinal.restful;

import com.jfinal.core.Action;

import java.util.Map;

/**
 * Created by iaceob on 2017/3/18.
 */
public class RestfulAction {


    private Action action;
    private Map<String, Object> paras;

    public RestfulAction(Action action) {
        this.action = action;
    }

    public RestfulAction(Action action, Map<String, Object> paras) {
        this.action = action;
        this.paras = paras;
    }

    public Action getAction() {
        return action;
    }

    public void setAction(Action action) {
        this.action = action;
    }

    public Map<String, Object> getParas() {
        return paras;
    }

    public void setParas(Map<String, Object> paras) {
        this.paras = paras;
    }

    public void clear() {
        if (this.paras != null)
            this.paras.clear();
    }

    @Override
    public String toString() {
        return "RestfulAction{" +
                "action=" + action +
                ", paras=" + paras +
                '}';
    }
}
