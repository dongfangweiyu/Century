package com.ra.common.exception;

/**
 * @description 检查状态异常
 * @tips 用于判断一下检查的条件，比如检查是否开通，是否授权等
 * Created by EDZ on 2018/9/28.
 */
public class StatusException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public StatusException(String message){
        super(message);
    }
}
