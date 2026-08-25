package com.sky.service;

import com.sky.dto.ShoppingCartDTO;

/**
 * 购物车相关业务接口
 */
public interface ShoppingCartService {

    /**
     * 添加购物车
     * @param shoppingCartDTO
     */
    void add(ShoppingCartDTO shoppingCartDTO);
}
