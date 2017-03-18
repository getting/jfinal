package com.jfinal.core;

import com.jfinal.config.Routes;
import com.jfinal.core.Action;
import com.jfinal.core.Controller;
import com.jfinal.restful.RestfulAction;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by iaceob on 2017/3/16.
 */
public abstract class UrlMapping {

    protected List<Routes> getRoutesList(Routes routes) {
        List<Routes> routesList = Routes.getRoutesList();
        List<Routes> ret = new ArrayList<Routes>(routesList.size() + 1);
        ret.add(routes);
        ret.addAll(routesList);
        return ret;
    }

    protected Set<String> buildExcludedMethodName() {
        Set<String> excludedMethodName = new HashSet<String>();
        Method[] methods = Controller.class.getMethods();
        for (Method m : methods) {
            if (m.getParameterTypes().length == 0)
                excludedMethodName.add(m.getName());
        }
        return excludedMethodName;
    }

    protected String buildMsg(String actionKey, Class<? extends Controller> controllerClass, Method method){
        StringBuilder sb = new StringBuilder("The action \"")
                .append(controllerClass.getName()).append(".")
                .append(method.getName()).append("()\" can not be mapped, ")
                .append("actionKey \"").append(actionKey).append("\" is already in use.");

        String msg = sb.toString();
        System.err.println("\nException: " + msg);
        return msg;
    }

    protected abstract void buildActionMapping();

    protected abstract RestfulAction getAction(String url, String[] urlPara, com.jfinal.restful.Method method);

    protected abstract List<String> getAllActionKeys();
}
