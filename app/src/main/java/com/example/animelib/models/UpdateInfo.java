package com.example.animelib.models;

import java.io.Serializable;

public class UpdateInfo implements Serializable {
    private String tagName;
    private String releaseName;
    private String changelog;
    private String publishedAt;
    private String htmlUrl;
    private String apkUrl;
    private long apkSize;

    public UpdateInfo() {}

    public UpdateInfo(String tagName, String releaseName, String changelog, String publishedAt, String htmlUrl, String apkUrl, long apkSize) {
        this.tagName = tagName;
        this.releaseName = releaseName;
        this.changelog = changelog;
        this.publishedAt = publishedAt;
        this.htmlUrl = htmlUrl;
        this.apkUrl = apkUrl;
        this.apkSize = apkSize;
    }

    public String getTagName() {
        return tagName;
    }

    public void setTagName(String tagName) {
        this.tagName = tagName;
    }

    public String getReleaseName() {
        return releaseName;
    }

    public void setReleaseName(String releaseName) {
        this.releaseName = releaseName;
    }

    public String getChangelog() {
        return changelog;
    }

    public void setChangelog(String changelog) {
        this.changelog = changelog;
    }

    public String getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(String publishedAt) {
        this.publishedAt = publishedAt;
    }

    public String getHtmlUrl() {
        return htmlUrl;
    }

    public void setHtmlUrl(String htmlUrl) {
        this.htmlUrl = htmlUrl;
    }

    public String getApkUrl() {
        return apkUrl;
    }

    public void setApkUrl(String apkUrl) {
        this.apkUrl = apkUrl;
    }

    public long getApkSize() {
        return apkSize;
    }

    public void setApkSize(long apkSize) {
        this.apkSize = apkSize;
    }
}
