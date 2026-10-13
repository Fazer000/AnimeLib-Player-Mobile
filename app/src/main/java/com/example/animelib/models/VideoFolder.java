package com.example.animelib.models;

public class VideoFolder {
    private final String name;
    private int count;
    private boolean isSelected;

    public VideoFolder(String name, int count) {
        this.name = name;
        this.count = count;
        this.isSelected = false;
    }

    public String getName() { return name; }
    public int getCount() { return count; }
    public void setCount(int count) { this.count = count; }
    public boolean isSelected() { return isSelected; }
    public void setSelected(boolean selected) { isSelected = selected; }
}
