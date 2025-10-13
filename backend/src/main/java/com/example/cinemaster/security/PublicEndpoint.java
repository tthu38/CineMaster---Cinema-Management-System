package com.example.cinemaster.security;

import java.lang.annotation.*;

/**
 * Đánh dấu endpoint được truy cập công khai (không cần JWT).
 * Dành cho các API GET public như xem phim, tin tức, ưu đãi, v.v.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface PublicEndpoint {}
