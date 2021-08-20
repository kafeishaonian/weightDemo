package com.hongming.image.widget;

interface ImageUploadStatus {
    void uploadStart(String key);
    void uploadSuccess(String key, String value);
    void uploadFailure(String key, String error);
}