package com.example.ecommerce.product.domain;

/**
 * 商品類型：
 * NORMAL：一般商品（支援多品項結帳）
 * FLASH_SALE：限量搶購商品（一次只能買一種且限購 1）
 */
public enum ProductType {
    NORMAL,
    FLASH_SALE
}