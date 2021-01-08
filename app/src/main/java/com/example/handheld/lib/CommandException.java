package com.example.handheld.lib;

public class CommandException extends RuntimeException {

    private int code;

    public CommandException(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
