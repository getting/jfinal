package com.jfinal.action;

import com.jfinal.config.Constants;
import com.jfinal.handler.Handler;

import javax.servlet.ServletContext;

/**
 * Created by iaceob on 2017/3/15.
 */
public class ActionManager {

    private Handler handler;
    private Constants constants;
    private ServletContext servletContext;
    private IActionFactory actionFactory = null;

    private static final ActionManager me = new ActionManager();

    public ActionManager() {
    }

    public Handler getHandler() {
        return handler;
    }

    public Constants getConstants() {
        return constants;
    }

    public ServletContext getServletContext() {
        return servletContext;
    }

    public IActionFactory getActionFactory() {
        return actionFactory;
    }

    public void setActionFactory(IActionFactory actionFactory) {
        this.actionFactory = actionFactory;
    }


    public void init(Handler handler, Constants constants, ServletContext servletContext) {
        this.handler = handler;
        this.constants = constants;
        this.servletContext = servletContext;
        
    }


}
