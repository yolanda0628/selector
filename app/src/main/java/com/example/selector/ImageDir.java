package com.example.selector;


import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * 图片（视频）路径的实体类
 */
public class ImageDir {
    /**
     * 定义一个枚举类型，将相关常量放入，其它地方能够直接取到
     */
    public enum Type {
        IMAGE, VEDIO, AUDIO
    }

    /**
     * 路径名
     */
    String dirName;
    /**
     * 路径地址
     */
    String dirPath;
    /**
     * 图片文件的集合
     */
    List<String> files = new ArrayList<String>();
    /**
     * 已选图片或视频文件的集合
     */
    HashSet<String> selectedFiles = new HashSet<String>();

    /**
     * 存储每个文件夹的图片路径或文件的视频路径
     */
    String firstImagePath;
    /**
     * 类型为图片类型
     */
    Type type=Type.IMAGE;

    public ImageDir(String dirPath) {
        super();
        this.dirPath = dirPath;
    }

    public void addFile(String file) {
        files.add(file);
    }

    public String getDirName() {
        return dirName;
    }

    public List<String> getFiles() {
        return files;
    }

    public void setFiles(List<String> files) {
        this.files = files;
    }

    public Type getType() {
        return type;
    }

        public void setType(Type type) {
            this.type = type;
    }

	/*public List<String> getIds() {
		return ids;
	}

	public void setIds(List<String> ids) {
		this.ids = ids;
	}*/


}
