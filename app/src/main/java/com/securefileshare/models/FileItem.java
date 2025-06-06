package com.securefileshare.models;

public class FileItem {
    public static final int TYPE_IMAGE = 1;
    public static final int TYPE_VIDEO = 2;
    public static final int TYPE_DOCUMENT = 3;

    private long id;
    private String name;
    private long size;
    private String path;
    private int type;

    public FileItem(long id, String name, long size, String path, int type) {
        this.id = id;
        this.name = name;
        this.size = size;
        this.path = path;
        this.type = type;
    }

    // getters and setters below
    public long getId() { return id; }
    public String getName() { return name; }
    public long getSize() { return size; }
    public String getPath() { return path; }
    public int getType() { return type; }
}
