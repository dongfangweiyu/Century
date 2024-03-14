package com.ra.common.utils;

import java.util.Random;
import java.util.UUID;


public final class NumberUtil {

	
	/**
	 * 生成yyyyMMddHHmmssSSS格式字符串
	 * @return
	 */
	public static String generateTimeStrapFormat(){
		String currentDate = DateUtil.getCurrentDateTime("yyyyMMddHHmmssSSS");
		return currentDate;
	}
	
	/**
	 * 生成指定长度数字
	 * @param length
	 * @return
	 */
	 public static String generateDigitalString(int length) {
        StringBuffer sb = new StringBuffer();
        Random random = new Random();

        for(int i = 0; i < length; ++i) {
            sb.append("0123456789".charAt(random.nextInt("0123456789".length())));
        }

        return sb.toString();
    }

	/**
	 * 生成指定长度字符
	 * @param length
	 * @return
	 */
	public static String generateCharacterString(int length) {
		StringBuffer sb = new StringBuffer();
		Random random = new Random();

		for(int i = 0; i < length; ++i) {
			sb.append("0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ".charAt(random.nextInt("0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ".length())));
		}

		return sb.toString();
	}

	/**
	 * 生成UUID
	 * @return
	 */
	public static String UUID(){
		return UUID.randomUUID().toString();
	}
}
