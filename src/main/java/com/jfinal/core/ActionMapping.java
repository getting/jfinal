/**
 * Copyright (c) 2011-2017, James Zhan 詹波 (jfinal@126.com).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.jfinal.core;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.jfinal.aop.Interceptor;
import com.jfinal.aop.InterceptorManager;
import com.jfinal.config.Interceptors;
import com.jfinal.config.Routes;
import com.jfinal.config.Routes.Route;
import com.jfinal.restful.MappingRecord;
import com.jfinal.restful.RestfulAction;

/**
 * ActionMapping
 */
final class ActionMapping {

	private static final String SLASH = "/";
	private Routes routes;
	private Boolean isRestful;
	// private Interceptors interceptors;

	// private final Map<String, Action> mapping = new HashMap<String, Action>();
	private final List<MappingRecord> mappings = new ArrayList<MappingRecord>();

    private final Pattern paraPattern = Pattern.compile("/:(?<key>[\\w\\-:]+)");
    private static Map<String, Pattern> urlPatternCache = new HashMap<String, Pattern>();

	ActionMapping(Routes routes, Interceptors interceptors, Boolean isRestful) {
		this.routes = routes;
		// this.interceptors = interceptors;
		this.isRestful = isRestful;
	}

	private Set<String> buildExcludedMethodName() {
		Set<String> excludedMethodName = new HashSet<String>();
		Method[] methods = Controller.class.getMethods();
		for (Method m : methods) {
			if (m.getParameterTypes().length == 0)
				excludedMethodName.add(m.getName());
		}
		return excludedMethodName;
	}

	private List<Routes> getRoutesList() {
		List<Routes> routesList = Routes.getRoutesList();
		List<Routes> ret = new ArrayList<Routes>(routesList.size() + 1);
		ret.add(routes);
		ret.addAll(routesList);
		return ret;
	}

	private String extraActionKey(Class<? extends Controller> controllerClass, String controllerKey, String methodName, ActionKey ak) {
        if (ak != null) {
            String ret = ak.value().trim();
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

    private Boolean existsRoute(String src, com.jfinal.restful.Method method, Boolean isRegex) {
        for (MappingRecord mapping : this.mappings) {
            if (!this.isRestful) {
                // jfinal action key repeat valid
                if (mapping.getUri().equals(src))
                    return true;
            } else {
                // restful action key repeat valid
                if (isRegex) {
                    if (mapping.getRegex().equals(src) && method == mapping.getMethod())
                        return true;
                } else {
                    if (mapping.getUri().equals(src) && method == mapping.getMethod())
                        return true;
                }
            }
        }
        return false;
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


    void buildActionMapping() {
		mappings.clear();
		Set<String> excludedMethodName = buildExcludedMethodName();
		InterceptorManager interMan = InterceptorManager.me();
		for (Routes routes : getRoutesList()) {
		for (Route route : routes.getRouteItemList()) {
			Class<? extends Controller> controllerClass = route.getControllerClass();
			Interceptor[] controllerInters = interMan.createControllerInterceptor(controllerClass);

			boolean sonOfController = (controllerClass.getSuperclass() == Controller.class);
			Method[] methods = (sonOfController ? controllerClass.getDeclaredMethods() : controllerClass.getMethods());
			for (Method method : methods) {
				String methodName = method.getName();
				if (excludedMethodName.contains(methodName) || method.getParameterTypes().length != 0)
					continue ;
				if (sonOfController && !Modifier.isPublic(method.getModifiers()))
					continue ;

				Interceptor[] actionInters = interMan.buildControllerActionInterceptor(routes.getInterceptors(), controllerInters, controllerClass, method);
				String controllerKey = route.getControllerKey();

				ActionKey ak = method.getAnnotation(ActionKey.class);
                String actionKey = this.extraActionKey(controllerClass, controllerKey, methodName, ak);

				com.jfinal.restful.Method reqMethod = ak == null ? com.jfinal.restful.Method.GET : ak.method();

                // 首次校验, 是否存在 url 重复
                if (this.existsRoute(actionKey, reqMethod, false))
                    throw new RuntimeException(buildMsg(actionKey, controllerClass, method));

				Action action = new Action(controllerKey, actionKey, controllerClass, method, methodName, actionInters, route.getFinalViewPath(routes.getBaseViewPath()));

                MappingRecord record = new MappingRecord();
                record.setUri(actionKey);
                record.setAction(action);
                if (this.isRestful) {
                    record.setMethod(reqMethod);
                    this.fillMapping(record, actionKey, controllerClass, method);
                }

                if (this.isRestful)
                    // restful 格式时, 进行路由二次校验, 避免发生有重复正则的路由
                    if (this.existsRoute(record.getRegex(), reqMethod, true))
                        throw new RuntimeException(buildMsg(actionKey, controllerClass, method));

                this.mappings.add(record);
			}
		}
		}
		routes.clear();

        // support url = controllerKey + urlParas with "/" of controllerKey
        RestfulAction restfulAction = this.getAction("/", new String[]{null}, com.jfinal.restful.Method.GET);
        if (restfulAction != null) {
            MappingRecord mapping = new MappingRecord();
            mapping.setUri("");
            mapping.setAction(restfulAction.getAction());
            this.mappings.add(mapping);
        }
	}

	private static final String buildMsg(String actionKey, Class<? extends Controller> controllerClass, Method method) {
		StringBuilder sb = new StringBuilder("The action \"")
			.append(controllerClass.getName()).append(".")
			.append(method.getName()).append("()\" can not be mapped, ")
			.append("actionKey \"").append(actionKey).append("\" is already in use.");

		String msg = sb.toString();
		System.err.println("\nException: " + msg);
		return msg;
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
            paras.put(mapping.getParakeys()[i], urlMatcher.group(i + 1));
        }

        return new RestfulAction(mapping.getAction(), paras);
    }

	/**
     *
	 * Support four types of url
	 * 1: http://abc.com/controllerKey                 ---> 00
	 * 2: http://abc.com/controllerKey/para            ---> 01
	 * 3: http://abc.com/controllerKey/method          ---> 10
	 * 4: http://abc.com/controllerKey/method/para     ---> 11
	 * The controllerKey can also contains "/"
	 * Example: http://abc.com/uvw/xyz/method/para
     *
     *
     * New support restful url
     * Action key: http://abc.com/controllerKey/method/:para1/sub/:para2/...
     * Handle url: http://abc.com/controllerKey/method/123/sub/123/...
     *
     * if want open restful open
     * AppConfig.configConstant add constants.setRestful(true);
     * default restful model is close.
     *
     *
	 */
    RestfulAction getAction(String url, String[] urlPara, com.jfinal.restful.Method method) {

        int i = url.lastIndexOf(SLASH);
        for (MappingRecord mapping : this.mappings) {

            // restful 模式才去校验 method
            if (this.isRestful)
                if (method != mapping.getMethod())
                    continue;

            if (mapping.getUri().equals(url)) {
                return new RestfulAction(mapping.getAction());
            }
            if (i != -1) {
                if (mapping.getUri().equals(url.substring(0, i))) {
                    // urlPara 是 jfinal 的 restful 实现形式中, 可以通过索引获取的第一个 get 参数, 但是标准的 restful url 格式实现则不再使用
                    if (!this.isRestful)
                        urlPara[0] = url.substring(i + 1);
                    return new RestfulAction(mapping.getAction());
                }
            }
            if (this.isRestful) {
                RestfulAction action = this.matchAction(mapping, url);
                if (action == null)
                    continue;

                return action;
            }
        }
        return null;
	}

	List<String> getAllActionKeys() {
        List<String> allActionKeys = new ArrayList<String>();
        for (MappingRecord mapping : this.mappings)
            allActionKeys.add(mapping.getUri());
        Collections.sort(allActionKeys);
        return allActionKeys;
	}
}







