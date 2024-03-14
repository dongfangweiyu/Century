package com.ra.common.domain;

import lombok.Data;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;

import java.util.ArrayList;
import java.util.List;

/**
 * 分页请求
 */
@Data
public class Pager {

    private int page;

    private int size;

    public Pager(){
        this.page=1;
        this.size=10;
    }

    public Pager(int page,int size){
        this.page=page;
        this.size=size;
    }

    public org.springframework.data.domain.PageRequest of(){
        return org.springframework.data.domain.PageRequest.of(this.page-1,this.size);
    }

    public org.springframework.data.domain.PageRequest of(Sort sort){
        return org.springframework.data.domain.PageRequest.of(this.page-1,this.size,sort);
    }

    public org.springframework.data.domain.PageRequest of(int page,int size,Sort sort){
        this.page=page;
        this.size=size;
        return of(sort);
    }

    public org.springframework.data.domain.PageRequest of(int page, int size, Sort.Direction direction, String... properties) {
        this.page=page;
        this.size=size;
        return org.springframework.data.domain.PageRequest.of(this.page-1,this.size,direction,properties);
    }

    /**
     * 把LIST按分页拆分
     * 返回列表数量，受限page和size
     * @param t
     * @param <T>
     * @return
     */
    public <T extends List> Page p(T t){
        int fromList=(getPage() - 1) * getSize();
        int toList=t.size()> getPage() *getSize()? getPage() * getSize():t.size();
        List<T> result =new ArrayList<>();
        if(fromList>=0&&fromList<t.size()){
            result = t.subList(fromList,toList );
        }
        return new PagerImpl(result, of(),t.size());
    }
}
