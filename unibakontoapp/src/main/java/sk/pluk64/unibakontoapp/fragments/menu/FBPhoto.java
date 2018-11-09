package sk.pluk64.unibakontoapp.fragments.menu;

import java.util.Date;

public class FBPhoto {
    private String source = "";
    private int width = 0;
    private int height = 0;
    private Date createdTime;
    private String caption = "";
    private int seqNo;
    private String fbUrl = "";

    public FBPhoto setSource(String source) {
        this.source = source;
        return this;
    }

    public String getSource() {
        return source;
    }

    public FBPhoto setWidth(int width) {
        this.width = width;
        return this;
    }

    public int getWidth() {
        return width;
    }

    public FBPhoto setHeight(int height) {
        this.height = height;
        return this;
    }

    public int getHeight() {
        return height;
    }

    public FBPhoto setCreatedTime(Date createdTime) {
        this.createdTime = createdTime;
        return this;
    }

    public Date getCreatedTime() {
        return createdTime;
    }

    public FBPhoto setCaption(String caption) {
        this.caption = caption;
        return this;
    }

    public String getCaption() {
        return caption;
    }

    public FBPhoto setSeqNo(int seqNo) {
        this.seqNo = seqNo;
        return this;
    }

    public int getSeqNo() {
        return seqNo;
    }

    public FBPhoto setFbUrl(String fbUrl) {
        this.fbUrl = fbUrl;
        return this;
    }

    public String getFbUrl() {
        return fbUrl;
    }
}
