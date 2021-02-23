package com.condor.launcher.theme.bean;

/**
 * author：liuzuo on 18-12-28 12:13
 */
public class ThemeDescriptionBean  {
    private String version;

    private String name;

    private String category;

    private String author;

    private String details;

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "ThemeDescriptionBean{" +
                "version='" + version + '\'' +
                ", name='" + name + '\'' +
                ", category='" + category + '\'' +
                ", author='" + author + '\'' +
                ", details='" + details + '\'' +
                '}';
    }
}
