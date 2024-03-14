package com.ra.dao.channel;

/**
 * PayInter的实例化工厂
 */
public class PayInterfaceFactory {

    public static PayInterface newInstance(PayInterfaceEnum payInterfaceEnum){
        try {
            return (PayInterface)Class.forName(payInterfaceEnum.getClassName()).newInstance();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        throw new IllegalArgumentException(payInterfaceEnum.getClassName()+"实例化异常.");
    }
}
