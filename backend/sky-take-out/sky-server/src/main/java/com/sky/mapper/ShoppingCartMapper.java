package com.sky.mapper;

import com.sky.entity.ShoppingCart;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface ShoppingCartMapper {

    /**
     * 根据条件查询购物车
     * @param shoppingCart
     * @return
     */
    List<ShoppingCart> list(ShoppingCart shoppingCart);

    /**
     * 更新购物车数量
     * @param existShoppingCart
     */
    @Update("update shopping_cart set number = #{number} where id = #{id}")
    void update(ShoppingCart existShoppingCart);

    /**
     * 新增购物车
     * @param shoppingCart
     */
    @Insert("insert into shopping_cart (user_id, dish_id, setmeal_id, dish_flavor, name, amount, image, number, create_time) " +
            "values (#{userId}, #{dishId}, #{setmealId}, #{dishFlavor}, #{name}, #{amount}, #{image}, #{number}, #{createTime})")
    void insert(ShoppingCart shoppingCart);
}
