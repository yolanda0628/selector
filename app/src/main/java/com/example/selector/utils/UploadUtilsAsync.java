package com.example.selector.utils;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.HttpVersion;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.protocol.HTTP;

import java.io.File;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Map;

/**
 * 异步AsyncTask+HttpClient上传文件,支持多文件上传,并显示上传进度
 * Created by pbq on 2016/7/17.
 */
public class UploadUtilsAsync extends AsyncTask<String, Integer, String> {
    /**
     * 服务器路径
     */
    private String url;
    /**
     * 上传的参数
     */
    private Map<String,String> paramMap;
    /**
     * 要上传的文件
     */
    private ArrayList<File> files;
    private long totalSize;
    private Context context;
    private ProgressDialog progressDialog;
    public UploadUtilsAsync(Context context,String url,Map<String, String> paramMap,ArrayList<File>files) {
        this.context=context;
        this.url=url;
        this.paramMap=paramMap;
        this.files=files;
    }

    /**
     * 界面的初始化（主线程）。显示一个进度对话框
     */
    @Override
    protected void onPreExecute() {//执行前的初始化
        // TODO Auto-generated method stub
        progressDialog=new ProgressDialog(context);
        progressDialog.setTitle("上传中...");
        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progressDialog.setCancelable(true);
        progressDialog.show();
        super.onPreExecute();
    }

    /**
     * 后台任务下载：（子线程）
     * @param params  在执行AsyncTask时传入的参数，用于在后台任务中使用
     * @return  执行结果
     */
    @Override
    protected String doInBackground(String... params) {//执行任务
        //MultipartEntityBuilder这个类主要用于创建HttpEntity
        //create() 创建一个MultipartEntityBuilder对象
        MultipartEntityBuilder builder = MultipartEntityBuilder.create();
        //设置请求的编码格式
        builder.setCharset(Charset.forName(HTTP.UTF_8));
        //设置浏览器兼容模式
        builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
        int count=0;
        for (File file:files) {
//			FileBody fileBody = new FileBody(file);//把文件转换成流对象FileBody
//			builder.addPart("file"+count, fileBody);
            //将文件以二进制的形式添加数据。
            builder.addBinaryBody("file"+count, file);
            count++;
        }
        //添加文本数据，设置请求参数
//        builder.addTextBody("method", paramMap.get("method"));
        builder.addTextBody("fileTypes", paramMap.get("fileTypes"));
        // 生成 HTTP POST 实体
        HttpEntity entity = builder.build();
        //获取上传文件的大小
        totalSize = entity.getContentLength();
        ProgressOutHttpEntity progressHttpEntity = new ProgressOutHttpEntity(
                entity, new ProgressOutHttpEntity.ProgressListener() {
            public void transferred(long transferedBytes) {
                //更新UI进度从子线程切换到UI线程
                publishProgress((int) (100 * transferedBytes / totalSize));
            }
        });
        return uploadFile(url, progressHttpEntity);
    }

    /**
     * 进度条更新
     * @param values 接收publishProgress中传来的进度值
     */
    @Override
    protected void onProgressUpdate(Integer... values) {//执行进度
        // TODO Auto-generated method stub
        Log.i("info", "values:" + values[0]);
        //对界面元素进行更新，更新进度条
        progressDialog.setProgress((int)values[0]);
        super.onProgressUpdate(values);
    }

    /**
     * 后台任务return执行完毕后调用
     * @param result 将后台的return返回的参数传入
     */
    @Override
    protected void onPostExecute(String result) {//执行结果
        // TODO Auto-generated method stub
        Log.i("info", result);
        //提醒任务执行的结果
        Toast.makeText(context, result, Toast.LENGTH_LONG).show();
        //关闭对话框
        progressDialog.dismiss();
        super.onPostExecute(result);
    }
    /**
     * 向服务器上传文件
     * @param url
     * @param entity
     * @return
     */
    public String uploadFile(String url, ProgressOutHttpEntity entity) {
        // 开启一个客户端 HTTP 请求
        HttpClient httpClient=new DefaultHttpClient();
        httpClient.getParams().setParameter(CoreProtocolPNames.PROTOCOL_VERSION, HttpVersion.HTTP_1_1);
        // 设置连接超时时间
        httpClient.getParams().setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, 5000);
        //创建 HTTP POST 请求
        HttpPost httpPost = new HttpPost(url);
        //设置请求参数
        httpPost.setEntity(entity);
        try {
            // 发起请求 并返回请求的响应
            HttpResponse httpResponse = httpClient.execute(httpPost);
            if (httpResponse.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                return "文件上传成功";
            }
        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (ConnectTimeoutException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (httpClient != null && httpClient.getConnectionManager() != null) {
                httpClient.getConnectionManager().shutdown();
            }
        }
        return "文件上传失败";
    }
}
