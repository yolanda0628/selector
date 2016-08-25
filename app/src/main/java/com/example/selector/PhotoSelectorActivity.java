package com.example.selector;


import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.GridView;
import android.widget.PopupWindow.OnDismissListener;
import android.widget.TextView;
import android.widget.Toast;

import com.example.selector.ImageDir.Type;
import com.example.selector.PhotoSelectorAdapter.onItemCheckedChangedListener;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * 照片选择和录像选择
 * Created by pbq on 2016/7/19.
 */

public class PhotoSelectorActivity extends Activity {
    /**
     * 通过键值对存储图片或视频的路径
     */
    private HashMap<String, ImageDir> imageDirsMap = new HashMap<String, ImageDir>();
    /**
     * 显示系统图片的视图
     */
    GridView gvPhotos;
    /**
     * 定义图片路径的实体
     */
    ImageDir currentDir;
    /**
     * 定义一个弹出popupwindow变量名
     */
    ImageFolderPopWindow popDir;
    /**
     * 最外层布局的变量
     */
    View lyTopBar;
    /**
     * 定义一个TextView的成员变量
     */
    TextView tvTitle;
    /**
     * 定义一个Button的成员变量
     */
    Button btnNext;
    int maxPicSize;
    /**
     * 记录图片的最大选择
     */
    private int maxCount = 3;

    private File cameraFile;
    /**
     * 在视频选择中录像的请求码
     */
    public static final int REQUEST_CODE_CAMERA = 1000;
    /**
     * 在图片选择中拍照的请求码
     */
    public static final int REQUEST_CODE_VEDIO = 2000;
    /**
     * 在图片选择点击后请求码
     */
    public static final int REQUEST_CODE_IMAGE_SWITCHER = 2000;
    private ArrayList<String> selectedFath;
    /**
     * 上传类型
     */
    private Type loadType = Type.IMAGE;
    /**
     * 上传大小
     */
    private long sizeLimit=5*1024*1024;//5m
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo_selector);
        initData();
        init();
    }
    /**
     * 初始化数据
     */
    private void initData() {
        //接收从DemoActivity传来的图片选择和录像选择路径
        selectedFath = getIntent().getStringArrayListExtra("selectedPaths");
        //判断是否存在loadType和sizeLimit，如果有，取出值  （图片没有，视频有）
        if (getIntent().hasExtra("loadType")) {
            loadType = Type.valueOf(getIntent().getStringExtra("loadType"));
        }
        if(getIntent().hasExtra("sizeLimit")){
            sizeLimit=getIntent().getIntExtra("sizeLimit", 1024);
        }
    }

    /**
     * 判断类型为图像
     */
    private boolean isImageType(){
        return loadType==Type.IMAGE;
    }
    /**
     * 判断类型为视频
     */
    private boolean isVedioType(){
        return loadType==Type.VEDIO;
    }

    /**
     * 初始化控件
     */
    private void init() {
        gvPhotos = (GridView) findViewById(R.id.gv_photos);
        tvTitle = (TextView) findViewById(R.id.tv_top_bar_title);
        btnNext = (Button) findViewById(R.id.btn_next);
        //如果是图片类型，显示图片列表，否则显示视频列表
        if(isImageType()){
            loadImagesList();
        }

        if(isVedioType()){
            loadVedioImagesList();
        }
        //传入到ImageFolderPopWindow构造方法中 loadType 加载的类型（图片或视频）
        popDir = new ImageFolderPopWindow(this,
                PhoneStateUtils.getScreenWidth(this),PhoneStateUtils.getScreenHeight(this) / 2);
        //设置外部可触摸
        popDir.setOutsideTouchable(true);
        lyTopBar = findViewById(R.id.ly_top_bar);
        popDir.setOnDismissListener(new OnDismissListener() {

            @Override
            public void onDismiss() {
                tvTitle.setSelected(false);
            }
        });
        //下拉框的点击事件
        popDir.setOnPopClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                //取出路径Tag
                ImageDir dir = (ImageDir) v.getTag();
                currentDir = dir;
                tvTitle.setText(dir.dirName);
                loadImages(currentDir);
                popDir.dismiss();
            }
        });
    }

    /**
     * 开启一个子线程加载下拉框图片列表
     */
    private void loadImagesList() {
        new Thread(new Runnable() {

            @Override
            public void run() {
                //查询到图片的地址
                Cursor cursor = getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        new String[] { MediaStore.Images.Media.DATA, MediaStore.Images.Media.MIME_TYPE },
                        "" + MediaStore.Images.Media.MIME_TYPE + "=? or " + MediaStore.Images.Media.MIME_TYPE + "=?",
                        new String[] { "image/jpeg", "image/png" }, MediaStore.Images.Media.DATE_MODIFIED);
                while (cursor.moveToNext()) {
                    //获取到图片地址
                    String filePath = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA));
                    File imageFile = new File(filePath);
                    ImageDir dir = addToDir(imageFile);
                    // 文件中图片的长度
                    if (dir.files.size() > maxPicSize) {
                        maxPicSize = dir.files.size();
                        currentDir = dir;
                    }

                    if (selectedFath.contains(filePath)) {
                        dir.selectedFiles.add(filePath);
                    }
                }

                runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        //加载图片列表
                        loadImages(currentDir);
                    }
                });
            }
        }).start();
    }

    /**
     * 加载下拉框视频列表
     */
    private void loadVedioImagesList() {
        new Thread(new Runnable() {

            @Override
            public void run() {
                Cursor cursor = getContentResolver().query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                        new String[] { MediaStore.Video.Media.DATA, MediaStore.Video.Media.MIME_TYPE,MediaStore.Video.Media._ID },
                       null,null,
                        MediaStore.Images.Media.DATE_MODIFIED);
                while (cursor.moveToNext()) {
                    String filePath = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA));
                    String id=cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media._ID));
                    File imageFile = new File(filePath);
                    ImageDir dir = addToDir(imageFile);
                    //dir.ids.add(id+"");
                    dir.setType(Type.VEDIO);
                    if (dir.files.size() > maxPicSize) {
                        maxPicSize = dir.files.size();
                        currentDir = dir;
                    }

                    if (selectedFath.contains(filePath)) {
                        dir.selectedFiles.add(filePath);
                    }
                }

                runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        //loadImages(currentDir);
                        loadVedioImages(currentDir);
                    }
                });
            }
        }).start();
    }



    /**
     * 添加图片或视频地址
     *
     * @param imageFile
     */
    private ImageDir addToDir(File imageFile) {
        ImageDir imageDir;
        File parentDirFile = imageFile.getParentFile();
        String parentFilePath = parentDirFile.getPath();
        if (!imageDirsMap.containsKey(parentFilePath)) {
            imageDir = new ImageDir(parentFilePath);

            imageDir.dirName = parentDirFile.getName();
            imageDirsMap.put(parentFilePath, imageDir);
            imageDir.firstImagePath = imageFile.getPath();
//            Log.i("PhotoSelectorActivity",imageDir.firstImagePath);
            imageDir.addFile(imageFile.toString());
        } else {
            imageDir = imageDirsMap.get(parentFilePath);
            imageDir.addFile(imageFile.toString());
        }

        return imageDir;
    }

    /**
     * 加载图片
     *
     * @param imageDir 图片路径实体
     */
    private void loadImages(final ImageDir imageDir) {
        PhotoSelectorAdapter adapter = new PhotoSelectorAdapter(PhotoSelectorActivity.this, imageDir);
        gvPhotos.setAdapter(adapter);
        //选择图片事件
        adapter.setOnItemCheckdedChangedListener(new onItemCheckedChangedListener() {

            @Override
            public void onItemCheckChanged(CompoundButton chBox, boolean isCheced, ImageDir imageDir, String path) {
                //判断是否可选，如果选择大于3张，则不能再选了，否则可以选择
                if (isCheced) {
                    if (getSelectedPictureCont() >= maxCount) {
                        Toast.makeText(PhotoSelectorActivity.this, "不能选择超过" + maxCount, Toast.LENGTH_SHORT).show();
                        chBox.setChecked(false);
                        imageDir.selectedFiles.remove(path);
                    } else {
                        imageDir.selectedFiles.add(path);
                    }
                } else {
                    imageDir.selectedFiles.remove(path);
                }

                updateNext();

            }

            @Override
            public void onTakePicture(ImageDir imageDir) {
                //在图片选择中拍照
                takePicture(imageDir);
            }

            @Override
            public void onShowPicture(String path) {
                //显示点击的具体图片
            }
        });
    }

    //加载视频
    private void loadVedioImages(final ImageDir imageDir) {
        PhotoSelectorAdapter adapter = new PhotoSelectorAdapter(PhotoSelectorActivity.this, imageDir);
        gvPhotos.setAdapter(adapter);

//        BitmapUtils bitmapUtils = new BitmapUtils(this);
//        gvPhotos.setOnScrollListener(new PauseOnScrollListener(bitmapUtils, false, true));
        //视频选择事件
        adapter.setOnItemCheckdedChangedListener(new onItemCheckedChangedListener() {

            @Override
            public void onItemCheckChanged(CompoundButton chBox, boolean isCheced, ImageDir imageDir, String path) {
                //判断是否可选，如果选择大于3张，则不能再选了，否则可以选择
                if (isCheced) {
                    if (getSelectedPictureCont() >= maxCount) {
                        Toast.makeText(PhotoSelectorActivity.this, "不能选择超过" + maxCount, Toast.LENGTH_SHORT).show();
                        chBox.setChecked(false);
                        imageDir.selectedFiles.remove(path);
                    } else {
                        imageDir.selectedFiles.add(path);
                    }
                } else {
                    imageDir.selectedFiles.remove(path);
                }

                updateNext();

            }

            @Override
            public void onTakePicture(ImageDir imageDir) {
                //在录像选择中录像
                takeVedio(imageDir);
            }

            @Override
            public void onShowPicture(String path) {
                //显示具体的图片
//                showImage(path);
            }
        });
    }


    /**
     * 更新下一步
     */
    public void updateNext() {
        //如果选择了图片,btnNext可以选择，记录选择的数量，设置字体为白色。否则不能选择，设置字体为黑色
        if (getSelectedPictureCont() > 0) {
            btnNext.setSelected(true);
            btnNext.setText("下一步(" + getSelectedPictureCont() + ")");
            btnNext.setTextColor(Color.WHITE);
        } else {
            btnNext.setSelected(false);
            btnNext.setText("下一步");
            btnNext.setTextColor(Color.BLACK);
        }
    }

    /**
     * 选择按钮
     * @param view
     */
    public void popImageDir(View view) {
        if (popDir.isShowing()) {
            popDir.dismiss();
            view.setSelected(false);
        } else {
            popDir.popWindow(imageDirsMap, lyTopBar);
            view.setSelected(true);
        }
    }

    /**
     * 下一步按钮
     * @param view
     */
    public void goNext(View view) {
        Log.i("zll",getSelectedPicture()+"");
        Intent intent = new Intent();
        //传入图片选择路径的集合
        intent.putExtra("selectPaths", getSelectedPicture());
        //返回成功给DemoActivity界面
        setResult(RESULT_OK, intent);
        finish();
    }

    /**
     * 取消按钮
     * @param view
     */
    public void goBack(View view) {
        finish();
    }

    /**
     * 发广播更新图册
     * @param path
     */
    public void updateGalleray(String path){
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        File f = new File(path);
        Uri contentUri = Uri.fromFile(f);
        mediaScanIntent.setData(contentUri);
        this.sendBroadcast(mediaScanIntent);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK) {
            return;
        }
        //在相册选择中拍照返回事件
        if (requestCode == REQUEST_CODE_CAMERA) {
            if (cameraFile != null && cameraFile.exists()) {
                updateGalleray(cameraFile.getPath());
                currentDir.selectedFiles.add(cameraFile.getPath());
                currentDir.files.add(0, cameraFile.getPath());
                loadImages(currentDir);
                updateNext();
            }
        }
        //在视频选择中录像返回事件
        if (requestCode == REQUEST_CODE_VEDIO) {
            if (cameraFile != null && cameraFile.exists()) {
                updateGalleray(cameraFile.getPath());
                currentDir.selectedFiles.add(cameraFile.getPath());
                currentDir.files.add(0, cameraFile.getPath());
                //loadVedioImages(currentDir);
                loadVedioImagesList();
                updateNext();
            }
        }
        //在图片选择点击后返回事件
        if (requestCode == REQUEST_CODE_IMAGE_SWITCHER) {
            String[] paths = data.getStringArrayExtra("selectPaths");
            for (int i = 0; i < paths.length; i++) {
                currentDir.selectedFiles.add(paths[i]);
            }
            loadImages(currentDir);
            updateNext();
        }
    }

    /**
     * 在图片选择中拍照
     *
     * @param imageDir
     */
    public void takePicture(ImageDir imageDir) {
        if (getSelectedPictureCont() >= maxCount) {
            Toast.makeText(this, "拍照前被选中照片张数不能大于" + maxCount, Toast.LENGTH_LONG).show();
            return;
        }
        cameraFile = new File(imageDir.dirPath, System.currentTimeMillis() + ".jpg");
        startActivityForResult(
                new Intent(MediaStore.ACTION_IMAGE_CAPTURE).putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(cameraFile)),
                REQUEST_CODE_CAMERA);
    }


    /**
     * 在录像选择中录像
     *
     * @param imageDir
     */
    public void takeVedio(ImageDir imageDir){
        cameraFile=new File(imageDir.dirPath, System.currentTimeMillis() + ".mp4");
        Intent intent=new Intent(MediaStore.ACTION_VIDEO_CAPTURE).putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(cameraFile));
        //intent.putExtra(MediaStore.EXTRA_SIZE_LIMIT, sizeLimit);
        //intent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 1);

        startActivityForResult(intent,REQUEST_CODE_CAMERA);
    }

    /**
     * 获取现在的图片数
     * @return 选择数
     */
    public int getSelectedPictureCont() {
        int count = 0;
        for (String name : imageDirsMap.keySet()) {
            count += imageDirsMap.get(name).selectedFiles.size();
        }

        return count;
    }

    /**
     * 获取图片的选择路径
     * @return 路径集合
     */
    public ArrayList<String> getSelectedPicture() {
        ArrayList<String> paths = new ArrayList<String>();
        for (String name : imageDirsMap.keySet()) {
            paths.addAll(imageDirsMap.get(name).selectedFiles);
        }

        return paths;
    }

}

