package com.jfinal.restful;

/**
 * Created by iaceob on 2017/3/15.
 */
public enum Method {

    GET,

    POST,

    PUT,

    PATCH,

    DELETE,

    HEAD,

    OPTIONS;

//    public static Method getMethod(String method) {
//        if (StrKit.isBlank(method))
//            throw new RuntimeException("Method can not be null.");
//        method = method.toUpperCase();
//        for (Method m : Method.values())
//            if (m.name().equals(method))
//                return m;
//        throw new RuntimeException("Can not find " + method);
//    }


}
