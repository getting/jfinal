package com.jfinal.core;

import com.jfinal.action.UrlMapping;
import com.jfinal.aop.Interceptor;
import com.jfinal.aop.InterceptorManager;
import com.jfinal.config.Routes;
import com.jfinal.core.Action;
import com.jfinal.core.Controller;
import com.jfinal.restful.MappingRecord;
import com.jfinal.restful.RESTful;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Created by iaceob on 2017/3/16.
 */
final class RestfulMapping extends UrlMapping {


    private static final String SLASH = "/";
    private Routes routes;

    private final List<MappingRecord> mappings = new ArrayList<MappingRecord>();

    public RestfulMapping(Routes routes) {
        this.routes = routes;
    }

    private Boolean exists(String uri, com.jfinal.restful.Method method) {
        for (MappingRecord mapping : mappings)
            if (mapping.getUri().equals(uri) && method == mapping.getMethod())
                return true;
        return false;
    }

    @Override
    protected final String buildMsg(String actionKey, Class<? extends Controller> controllerClass, Method method) {
        StringBuilder sb = new StringBuilder("The action \"")
                .append(controllerClass.getName()).append(".")
                .append(method.getName()).append("()\" can not be mapped, ")
                .append("actionKey \"").append(actionKey).append("\" is already in use.");

        String msg = sb.toString();
        System.err.println("\nException: " + msg);
        return msg;
    }

    @Override
    public void buildActionMapping() {
        mappings.clear();
        Set<String> excludedMethodName = super.buildExcludedMethodName();
        InterceptorManager interMan = InterceptorManager.me();
        for (Routes routes : getRoutesList(this.routes)) {
            for (Routes.Route route : routes.getRouteItemList()) {
                Class<? extends Controller> controllerClass = route.getControllerClass();
                Interceptor[] controllerInters = interMan.createControllerInterceptor(controllerClass);

                boolean sonOfController = (controllerClass.getSuperclass() == Controller.class);
                Method[] methods = (sonOfController ? controllerClass.getDeclaredMethods() : controllerClass.getMethods());

                for (Method method : methods) {
                    String methodName = method.getName();
                    if (excludedMethodName.contains(methodName) || method.getParameterTypes().length != 0)
                        continue;
                    if (sonOfController && !Modifier.isPublic(method.getModifiers()))
                        continue;

                    Interceptor[] actionInters = interMan.buildControllerActionInterceptor(routes.getInterceptors(), controllerInters, controllerClass, method);
                    String controllerKey = route.getControllerKey();

                    RESTful restful = method.getAnnotation(RESTful.class);
                    String actionKey = this.extraActionKey(controllerClass, controllerKey, methodName, restful);

                    if (this.exists(actionKey, restful.method()))
                        throw new RuntimeException(buildMsg(actionKey, controllerClass, method));

                    Action action = new Action(controllerKey, actionKey, controllerClass, method, methodName, actionInters, route.getFinalViewPath(routes.getBaseViewPath()));
                    MappingRecord record = new MappingRecord();
                    record.setUri(actionKey);
                    record.setMethod(restful.method());
                    record.setAction(action);
                    this.mappings.add(record);

                }
            }
        }

    }

    /**
     * 提取 ActionKey 路径
     *
     * @param controllerKey 控制器键
     * @param methodName    方法名
     * @param restful       RESTful 注解
     * @return String
     */
    private String extraActionKey(Class<? extends Controller> controllerClass, String controllerKey, String methodName, RESTful restful) {
        if (restful != null) {
            String ret = restful.path().trim();
            if ("".equals(ret))
                throw new IllegalArgumentException(controllerClass.getName() + "." + methodName + "(): The argument of ActionKey can not be blank.");
            if (!ret.startsWith(SLASH))
                return SLASH + ret;
            return ret;
        }

        if (methodName.equals("index"))
            return controllerKey;

        return controllerKey.equals(SLASH) ? SLASH + methodName : controllerKey + SLASH + methodName;
    }

}
