package com.ra.dao.base;

import com.alibaba.fastjson.JSONObject;
import lombok.Data;

import javax.persistence.*;
import java.io.Serializable;
import java.sql.Timestamp;


/**
 * 实体最顶级基类
 * @author Administrator
 *
 */
@Data
@MappedSuperclass
public class BaseEntity implements Serializable{

	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

	/**
	 * 创建时间
	 */
//	@Column(name = "createTime", columnDefinition = "TIMESTAMP COMMENT '创建时间'")
	private Timestamp createTime=new Timestamp(System.currentTimeMillis());

	/**
	 * 逻辑删除
	 * 0：未删除  1 已删除
	 */
	@Column
	private boolean deleted=false;


	/**
	 * 转成json返回
	 * @return
	 */
	public String toJson(){
		return JSONObject.toJSONString(this);
	}

	/**
	 * 从json字符串序列化对象
	 * @param json
	 * @param cls
	 * @param <T>
	 * @return
	 */
	public static <T> T fromJson(String json,Class<T> cls){
		return JSONObject.parseObject(json, cls);
	}
}
