package com.example.handheld.lib;

public class TagAccessException extends RuntimeException {

    private int code;

    public TagAccessException(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
