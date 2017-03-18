package com.jfinal.core;

import com.jfinal.kit.JsonKit;
import com.jfinal.template.ext.directive.Str;
import org.junit.Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by iaceob on 2017/3/17.
 */
public class RestfulMappingTest {

    @Test
    public void testBuidReg() {
        String uri = "/test/tt/:id/update/:op";
        Pattern pattern = Pattern.compile("/:(?<key>[\\w\\-:]+)");
        Matcher matcher = pattern.matcher(uri);
        while (matcher.find()) {
            String next = matcher.group("key");
            System.out.println(next);
        }

    }

    @Test
    public void testReg1() {
        String text = "/zoo/813/animal/2";
        text = "/zoo";
        text = "/";
        String[] rets = text.split("/");
        System.out.println(JsonKit.toJson(rets));
    }

    @Test
    public void testReg2() {
        String str = "3.601.9";
        Boolean ret = str.matches("^[-+]?(\\d+(\\.\\d*)?|\\.\\d+)([eE]([-+]?([012]?\\d{1,2}|30[0-7])|-3([01]?[4-9]|[012]?[0-3])))?[dD]?$");
        System.out.println(ret);
    }

}