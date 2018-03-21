package cn.migu.hasika.download.connect;

import android.util.Log;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

import cn.migu.hasika.download.MulitDownloadConfig;

/**
 * Author: hasika
 * Time: 2018/3/21
 * Any questions can send email to lbhasika@gmail.com
 */

public class ConnectThread implements Runnable {
    private static final String TAG = ConnectThread.class.getCanonicalName();

    private String mUrl;
    private ConnectListener mListener;

    public ConnectThread(String url, ConnectListener listener){
        mUrl = url;
        mListener = listener;
    }

    @Override
    public void run() {
        int responseCode = -1;
        HttpURLConnection urlConnection = null;
        try {
            urlConnection = (HttpURLConnection) new URL(mUrl).openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.setConnectTimeout(MulitDownloadConfig.CONNECT_TIME);
            urlConnection.setReadTimeout(MulitDownloadConfig.READ_TIME);

            responseCode = urlConnection.getResponseCode();
            int contentLength = urlConnection.getContentLength();
            boolean isSupportRange = false;
            if (responseCode == HttpURLConnection.HTTP_OK){
                String ranges = urlConnection.getHeaderField("Accept-Ranges");
                if ("bytes".equals(ranges)){
                    isSupportRange = true;
                }
                Log.d(TAG, "run, isSupportRange:"+isSupportRange+", contentLength:"+contentLength+", ranges:"+ranges);
                mListener.onConnectSuccess(isSupportRange, contentLength);
            }

        } catch (IOException e) {
            e.printStackTrace();
            mListener.onConnectFailure("responseCode:"+ responseCode+", msg:"+e.getMessage());
        } finally {
            if (urlConnection != null){
                urlConnection.disconnect();
            }
        }
    }


    public interface ConnectListener{
        void onConnectSuccess(boolean isSupportRange, long totalSize);
        void onConnectFailure(String msg);
    }

}
