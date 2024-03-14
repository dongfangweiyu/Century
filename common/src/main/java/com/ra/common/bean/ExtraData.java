package com.ra.common.bean;

import com.ra.common.domain.ExtraDataInterface;
import lombok.Data;

@Data
public class ExtraData {

    private String convertClass;

    private Object data;

    public ExtraDataInterface newInstance(){
        ExtraDataInterface extraDataInterface = null;
        try {
            extraDataInterface = (ExtraDataInterface)Class.forName(this.convertClass).newInstance();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return extraDataInterface;
    }
}
