package com.ra.common.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * 重写JPA  PageImpl类
 * 主要是修改的jpa返回值page默认从0开始的问题
 * 已重写成page从1开始计数
 * @param <T>
 */
//返回时,重写getNumber父类加1,接受参数时-1
public class PagerImpl<T> extends PageImpl<T> implements Page<T> {

    /**
     * 前台传参1为起始页修改为spring data jpa的0为起始页
     * @param pageable
     * @return
     */
    public static Pageable pageRequest(Pageable pageable){
        return new PageRequest(pageable.getPageNumber()-1,pageable.getPageSize(),pageable.getSort());
    }

    /**
     * Constructor of {@code PageImpl}.
     *
     * @param content  the content of this page, must not be {@literal null}.
     * @param pageable the paging information, can be {@literal null}.
     * @param total    the total amount of items available. The total might be adapted considering the length of the content
     */
    public PagerImpl(List<T> content, Pageable pageable, long total) {
        super(content, pageable, total);
    }

    /**
     * 重写当前页，将当前页加1返回前台，spring data jpa起始页0加1后返回前台
     * @return
     */
    @Override
    public int getNumber() {
        return super.getNumber() + 1;
    }
}