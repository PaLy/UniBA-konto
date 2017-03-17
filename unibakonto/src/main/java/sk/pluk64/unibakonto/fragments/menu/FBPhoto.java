package sk.pluk64.unibakonto.fragments.menu;

import java.util.Calendar;
import java.util.Date;

public class FBPhoto {
    private String id;
    private String source;
    private int width;
    private int height;
    private Date createdTime;
    private String caption;
    private int seqNo;

    public String getId() {
        return id;
    }

    public FBPhoto setID(String id) {
        this.id = id;
        return this;
    }

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

    public boolean isCreatedToday() {
        return isToday(createdTime);
    }

    public static boolean isToday(Date time) {
        if (time == null) {
            return false;
        }
        Calendar nowTime = Calendar.getInstance();

        Calendar thenTime = Calendar.getInstance();
        thenTime.setTime(time);

        return nowTime.get(Calendar.DAY_OF_YEAR) == thenTime.get(Calendar.DAY_OF_YEAR) &&
                nowTime.get(Calendar.YEAR) == thenTime.get(Calendar.YEAR);
    }
}
