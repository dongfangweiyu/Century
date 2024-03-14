package com.ra.dao.factory;

import com.alibaba.fastjson.JSON;
import com.ra.common.configuration.SpringContextHolder;
import com.ra.common.exception.ConfigIllegalException;
import com.ra.dao.Enum.ConfigEnum;
import com.ra.dao.entity.config.ConfigSystem;
import com.ra.dao.repository.config.ConfigRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;

/**
 * 系统配置工厂
 * 添加配置，获取配置，修改配置
 */
public final class ConfigFactory {

    private static final Logger logger = LoggerFactory.getLogger(ConfigFactory.class);
    private static ConfigRepository configRepository;

    /**
     * 初始化ConfigRepository
     */
    private static void init() {
        if (configRepository == null) {
            configRepository = SpringContextHolder.getBean(ConfigRepository.class);
        }
    }

    /**
     * 添加/修改配置
     *
     * @param keyEnum
     * @param value
     */
    public static void set(ConfigEnum keyEnum, String value) {
        String key = keyEnum.toString();
        //检测key or value Exception
        ConfigIllegalException.filterKeyOrValue(key, value);
        try {
            init();//初始化
            ConfigSystem config = configRepository.findByCKey(key);
            if (config == null) {
                config = new ConfigSystem();
            }
            config.setCKey(key);
            config.setCValue(value);
            config.setDescription(keyEnum.getDesc());
            configRepository.save(config);
        } catch (Exception e) {
            throw new ConfigIllegalException(key);
        }
    }

    /**
     * 获取配置
     * 如果获取为null,会抛出异常
     *
     * @param keyEnum
     * @return
     */
    public static String get(ConfigEnum keyEnum) {
        String key = keyEnum.toString();
        try {
            init();//初始化
            ConfigSystem config = configRepository.findByCKey(key);
            if (config == null) {
                return keyEnum.getDefaultValue();
            }
            return config.getCValue();
        } catch (Exception e) {
            throw new ConfigIllegalException(key);
        }
    }

//    /**
//     * 获取配置
//     *
//     * @param keyEnum
//     * @param defaultValue 如果查询为null,返回默认值
//     * @return
//     */
//    public static <T> T get(ConfigEnum keyEnum, T defaultValue) {
//        String key = keyEnum.toString();
//        try {
//            init();//初始化
//            ConfigSystem config = configRepository.findByCKey(key);
//            if (config == null) {
//                return defaultValue;
//            }
//            return (T) config.getCValue();
//        } catch (Exception e) {
//            throw new ConfigIllegalException(key);
//        }
//    }

    /**
     * 获取配置
     *
     * @param key
     * @return T
     * @Param T
     */
    public static <T> T get(ConfigEnum key, Class<T> cls) {
        String value = get(key);
        return JSON.parseObject(value, cls);
    }

    /**
     * 获取
     *
     * @param key，如果为null,抛出异常
     * @return int
     */
    public static int getInt(ConfigEnum key) {
        String value = get(key);
        return Integer.parseInt(value);
    }

//    /**
//     * 获取
//     * 如果为null,返回默认值
//     *
//     * @param key
//     * @param defaultValue
//     * @return int
//     */
//    public static int getInt(ConfigEnum key, int defaultValue) {
//        Object value = get(key, defaultValue);
//        return Integer.parseInt(value.toString());
//    }

    /**
     * 获取
     *
     * @param key
     * @return int
     */
    public static double getDouble(ConfigEnum key) {
        String value = get(key);
        return Double.parseDouble(value);
    }

//    /**
//     * 获取
//     * 如果为null,返回默认值
//     *
//     * @param key
//     * @return int
//     */
//    public static double getDouble(ConfigEnum key, double defaultValue) {
//        Object value = get(key, defaultValue);
//        return Double.parseDouble(value.toString());
//    }

    /**
     * 获取
     *
     * @param key
     * @return
     */
    public static BigDecimal getBigDecimal(ConfigEnum key) {
        return BigDecimal.valueOf(getDouble(key));
    }
//
//    /**
//     * 获取
//     * 如果为null,返回默认值
//     *
//     * @param key
//     * @return
//     */
//    public static BigDecimal getBigDecimal(ConfigEnum key, BigDecimal defaultValue) {
//        return BigDecimal.valueOf(getDouble(key, defaultValue.doubleValue()));
//    }

    /**
     * 获取
     *
     * @param key
     * @return int
     */
    public static boolean getBoolean(ConfigEnum key) {
        String value = get(key);
        return Boolean.parseBoolean(value);
    }

//    /**
//     * 获取
//     * 如果为null,返回默认值
//     *
//     * @param key
//     * @return int
//     */
//    public static boolean getBoolean(ConfigEnum key, boolean defaultValue) {
//        Object value = get(key, defaultValue);
//        return Boolean.parseBoolean(value.toString());
//    }
}
