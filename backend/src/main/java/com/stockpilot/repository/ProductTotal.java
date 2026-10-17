package com.stockpilot.repository;

/**
 * A "projection": Spring Data fills this interface from the columns of a custom query.
 * Used to fetch total stock for many products in ONE grouped query.
 */
public interface ProductTotal {
    Long getProductId();

    Long getTotal();
}
