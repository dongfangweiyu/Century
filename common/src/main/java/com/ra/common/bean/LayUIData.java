package com.ra.common.bean;

import lombok.Data;

/**
 * 这个是layui的transfer组件的data bean
 * 穿梭框
 */
@Data
public class LayUIData {

    public LayUIData(){}

    public LayUIData(String value, String title){
        this.value=value;
        this.title=title;
    }

    private String value;
    private String title;
    private boolean disabled;
}
