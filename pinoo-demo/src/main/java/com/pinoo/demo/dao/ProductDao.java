package com.pinoo.demo.dao;

import java.util.List;

import com.pinoo.annotation.method.MethodParam;
import com.pinoo.annotation.method.MethodProxy;
import com.pinoo.demo.model.Product;
import com.pinoo.mapping.MethodType;
import com.pinoo.storage.mybatis.annotation.model.ModelInfo;

@ModelInfo(entityClass = Product.class, tableName = "Products")
public interface ProductDao {

    @MethodProxy(type = MethodType.SELECT)
    public List<Product> get(@MethodParam("brandId") int brandId);

}
