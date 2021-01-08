package com.example.handheld.bean;

public class VersionBean {

    private String title;

    private String value;

    public VersionBean() {
    }

    public VersionBean(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
