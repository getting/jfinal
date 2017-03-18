package com.jfinal.core;

import com.jfinal.aop.Interceptor;
import com.jfinal.aop.InterceptorManager;
import com.jfinal.config.Routes;
import com.jfinal.kit.StrKit;
import com.jfinal.restful.MappingRecord;
import com.jfinal.restful.RESTful;
import com.jfinal.restful.RestfulAction;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by iaceob on 2017/3/16.
 */
final class RestfulMapping extends UrlMapping {


    private static final String SLASH = "/";
    private Routes routes;

    private final List<MappingRecord> mappings = new ArrayList<MappingRecord>();

    private final Pattern paraPattern = Pattern.compile("/:(?<key>[\\w\\-:]+)");
    private static Map<String, Pattern> urlPatternCache = new HashMap<String, Pattern>();

    public RestfulMapping(Routes routes) {
        this.routes = routes;
    }

    private Boolean existsRoute(String src, com.jfinal.restful.Method method, Boolean isRegex) {
        for (MappingRecord mapping : mappings) {
            if (isRegex) {
                if (mapping.getRegex().equals(src) && method == mapping.getMethod())
                    return true;
            } else {
                if (mapping.getUri().equals(src) && method == mapping.getMethod())
                    return true;
            }
        }
        return false;
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
            String ret = restful.value().trim();
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

    @Override
    protected void buildActionMapping() {
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

                    com.jfinal.restful.Method reqMethod = restful == null ? com.jfinal.restful.Method.GET : restful.method();

                    // 首次校验, 是否存在 url 重复
                    if (this.existsRoute(actionKey, reqMethod, false))
                        throw new RuntimeException(buildMsg(actionKey, controllerClass, method));

                    Action action = new Action(controllerKey, actionKey, controllerClass, method, methodName, actionInters, route.getFinalViewPath(routes.getBaseViewPath()));
                    MappingRecord record = new MappingRecord();
                    record.setUri(actionKey);
                    record.setMethod(reqMethod);
                    record.setAction(action);
                    // record.setRegex(this.genRegex(actionKey));
                    this.fillMapping(record, actionKey, controllerClass, method);
                    // 进行路由二次校验, 避免发生有重复正则的路由
                    if (this.existsRoute(record.getRegex(), reqMethod, true))
                        throw new RuntimeException(buildMsg(actionKey, controllerClass, method));
                    this.mappings.add(record);

                }
            }
        }
    }

    private void fillMapping(MappingRecord record, String actionKey, Class<? extends Controller> controllerClass, Method method) {
        Matcher paraMatch = this.paraPattern.matcher(actionKey);
        StringBuffer sb = new StringBuffer();
        Set<String> paraKeys = new HashSet<String>();
        Integer count = 0;
        while (paraMatch.find()) {
            String paraName = paraMatch.group("key");
            paraKeys.add(paraName);
            paraMatch.appendReplacement(sb, "/([\\\\w\\\\-:]+)");
            count += 1;
        }
        paraMatch.appendTail(sb);

        // 校验参数名是否有重复, 如果有重复则不允许启动
        if (paraKeys.size() != count)
            throw new RuntimeException(String.format("The action %s.%s url parameter names can not be repeated, actionKey %s",
                    controllerClass.getName(), method.getName(), actionKey));

        // 设置 url 匹配正则
        record.setRegex(String.format("^%s$", sb.toString()));
        // 设置 url 参数识别符
        record.setParakeys(paraKeys.toArray(new String[paraKeys.size()]));
        paraKeys.clear();
    }

    private Pattern getUrlPattern(String regex) {
        if (urlPatternCache.get(regex) != null)
            return urlPatternCache.get(regex);
        Pattern pattern = Pattern.compile(regex);
        urlPatternCache.put(regex, pattern);
        return pattern;
    }

    private RestfulAction matchAction(MappingRecord mapping, String url) {

        String resturi = mapping.getUri();

        // 避免不必要的正则匹配
        if (!resturi.contains(String.format("%s%s", SLASH, url.split("/")[1])))
            return null;

        Pattern urlPattern = this.getUrlPattern(mapping.getRegex());
        Matcher urlMatcher = urlPattern.matcher(url);
        if (!urlMatcher.find())
            return null;

        Integer groupCount = urlMatcher.groupCount();
        Map<String, Object> paras = new HashMap<String, Object>();
        for (Integer i = groupCount; i-- > 0; ) {
            paras.put(mapping.getParakeys()[i], this.convert(urlMatcher.group(i + 1)));
        }

        return new RestfulAction(mapping.getAction(), paras);
    }

    private Object convert(String val) {
        if (StrKit.isBlank(val))
            return null;
        val = val.trim();
        if (val.matches("\\d+")) {
            Long ret = Long.parseLong(val);
            return ret <= Integer.MAX_VALUE ? ret.intValue() : ret;
        }
        if ("true".equals(val.toLowerCase()))
            return true;
        if ("false".equals(val.toLowerCase()))
            return false;
        return val;
    }

    @Override
    protected RestfulAction getAction(String url, String[] urlPara, com.jfinal.restful.Method method) {
        // --------
        int i = url.lastIndexOf(SLASH);
        for (MappingRecord mapping : this.mappings) {
            if (method != mapping.getMethod())
                continue;

            if (mapping.getUri().equals(url)) {
                return new RestfulAction(mapping.getAction());
            }
            if (i != -1) {
                if (mapping.getUri().equals(url.substring(0, i))) {
                    // urlPara 是 jfinal 的 restful 实现形式中, 可以通过索引获取的第一个 get 参数, 但是这里标准的 restful url 格式实现则不再使用
                    // urlPara[0] = url.substring(i + 1);
                    return new RestfulAction(mapping.getAction());
                }
            }

            RestfulAction action = this.matchAction(mapping, url);
            if (action == null)
                continue;

            return action;
        }
        return null;
    }

    @Override
    protected List<String> getAllActionKeys() {
        List<String> rets = new ArrayList<String>();
        for (MappingRecord mapping : this.mappings)
            rets.add(mapping.getUri());
        Collections.sort(rets);
        return rets;
    }

}
