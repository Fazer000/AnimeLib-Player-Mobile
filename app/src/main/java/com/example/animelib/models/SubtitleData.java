package com.example.animelib.models;

import java.io.Serializable;

public class SubtitleData implements Serializable {
    private int id;
    private String format;
    private String name;
    private String filename;
    private String src;

    public SubtitleData() {}

    public SubtitleData(int id, String format, String name, String filename, String src) {
        this.id = id;
        this.format = format;
        this.name = name;
        this.filename = filename;
        this.src = src;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getFormat() { return format; }
    public void setFormat(String format) { this.format = format; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getFilename() { return filename; }
    public void setFilename(String filename) { this.filename = filename; }

    public String getSrc() { return src; }
    public void setSrc(String src) { this.src = src; }
}
