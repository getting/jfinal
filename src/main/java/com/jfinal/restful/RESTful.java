package com.jfinal.restful;

import java.lang.annotation.*;

/**
 * Created by iaceob on 2017/3/16.
 */
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface RESTful {
    String path();
    Method method() default Method.GET;
}
