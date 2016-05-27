package me.buryinmind.android.app.model;

import java.util.ArrayList;
import java.util.List;

public class ImageFolder {
    /**
     * 图片的文件夹路径
     */
    private String dir;

    /**
     * 文件夹的名称
     */
    private String name;

    private List<String> images = new ArrayList<String>();

    public ImageFolder(String dir) {
        this.dir = dir;
        int lastIndexOf = this.dir.lastIndexOf("/");
        this.name = this.dir.substring(lastIndexOf);
    }

    public ImageFolder(String dir, String name) {
        this.dir = dir;
        this.name = name;
    }

    public void addImage(String path) {
        images.add(path);
    }

    public String getDir() {
        return dir;
    }

    public String getFirstImagePath() {
        return images.size() > 0 ? images.get(0) : null;
    }

    public String getName() {
        return name;
    }

    public int getCount() {
        return images.size();
    }

    public List<String> getImages() {
        return images;
    }
}
