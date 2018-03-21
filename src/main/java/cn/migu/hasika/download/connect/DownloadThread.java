package cn.migu.hasika.download.connect;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;

import cn.migu.hasika.download.MulitDownloadConfig;

/**
 * Created by hasika on 2018/3/13.
 */

public class DownloadThread implements Runnable {
    private static final String TAG = DownloadThread.class.getCanonicalName();

    private int mIndex;
    private String mUrl;
    private long mStartP;
    private long mEndP;
    private String mPath;
    private boolean mIsSupportRange;
    private DownloadListener mListener;

    private volatile boolean mIsCanceled = false;
    private volatile boolean mIsPaused = false;
    private volatile boolean mIsErrored = false;


    public DownloadThread(int index, String url, String path, boolean isSupportRange, long startP, long endP, DownloadListener listener){
        mIndex = index;
        mUrl = url;
        mStartP = startP;
        mEndP = endP;
        mIsSupportRange = isSupportRange;
        mPath = path;
        mListener = listener;
    }

    @Override
    public void run() {
        HttpURLConnection httpURLConnection = null;
        RandomAccessFile randomAccessFile = null;
        FileOutputStream fileOutputStream = null;
        InputStream inputStream = null;
        try {
            httpURLConnection = (HttpURLConnection) new URL(mUrl).openConnection();
            httpURLConnection.setRequestMethod("GET");
            httpURLConnection.setConnectTimeout(MulitDownloadConfig.CONNECT_TIME);
            httpURLConnection.setReadTimeout(MulitDownloadConfig.READ_TIME);
            if (mIsSupportRange){
                httpURLConnection.setRequestProperty("Range", "bytes="+mStartP+"-"+mEndP);
            }

            int responseCode = httpURLConnection.getResponseCode();
            int contentLenght = httpURLConnection.getContentLength();

            if (responseCode == HttpURLConnection.HTTP_PARTIAL){
                File file = new File(mPath);

                randomAccessFile = new RandomAccessFile(file, "rw");
                inputStream = httpURLConnection.getInputStream();
                randomAccessFile.seek(mStartP);

                byte[] buf = new byte[2048];
                int len;
                while ((len = inputStream.read(buf, 0, buf.length)) != -1){
                    if (mIsCanceled || mIsPaused || mIsErrored){
                        break;
                    }

                    randomAccessFile.write(buf, 0, len);

                    mListener.onProgressChanged(mIndex, len);
                }

                randomAccessFile.close();
                inputStream.close();

            } else if (responseCode == HttpURLConnection.HTTP_OK){
                File file = new File(mPath);
                fileOutputStream = new FileOutputStream(file);
                inputStream = httpURLConnection.getInputStream();

                byte[] buf = new byte[2048];
                int len;
                while ((len = inputStream.read(buf, 0, buf.length)) != -1){
                    if (mIsCanceled){
                        break;
                    }

                    fileOutputStream.write(buf, 0, len);

                    mListener.onProgressChanged(mIndex, len);
                }

                fileOutputStream.close();
                inputStream.close();

            } else {
                mListener.onDownloadError("responseCode:"+responseCode);
            }

            if (mIsCanceled){
                mListener.onDownloadCanceled();
            }

            if (mIsPaused){
                mListener.onDownloadPaused();
            }
        } catch (Exception e){

            mListener.onDownloadError(e.getMessage());
        } finally {
            if (httpURLConnection != null){
                httpURLConnection.disconnect();
            }
        }
    }

    public void pause(){
        mIsPaused = true;
    }

    public void cancel(){
        mIsCanceled = true;
    }

    public void error(){
        mIsErrored = true;
    }

    public interface DownloadListener{
        void onProgressChanged(int index, long changeSizez);
        void onDownloadPaused();
        void onDownloadCanceled();
        void onDownloadError(String msg);
    }
}
