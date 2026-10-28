package com.example.myapp.domain.repository;

/**
 * Interface chung để xử lý callback dữ liệu bất đồng bộ.
 */
public interface DataCallback<T> {
    void onSuccess(T data);
    void onError(String errorMessage);
}