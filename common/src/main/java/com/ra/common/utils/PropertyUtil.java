package com.ra.common.utils;

import com.ra.common.configuration.SpringContextHolder;
import org.springframework.core.env.Environment;

/**
 * 获取配置文件中的属性配置
 * @author ouyang
 *
 */
public final class PropertyUtil {

    private static Environment environment;

    private PropertyUtil() {
    }

    private static void initEnvironment() {
        if (environment == null) {
            environment= SpringContextHolder.getApplicationContext().getEnvironment();
        }
    }

    public static String getValue(String key) {
        initEnvironment();
        return environment.getProperty(key);
    }

    public static String getValue(String key, String defaultValue) {
        initEnvironment();
        return environment.getProperty(key, defaultValue);
    }
}