package cn.migu.hasika.download;

import android.content.Context;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.text.TextUtils;

import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import cn.migu.hasika.download.task.DatabaseTask;
import cn.migu.hasika.download.task.DownloadTask;

/**
 * download file from net
 * support multi-thread multi-task partial-download
 * can config by {@link MulitDownloadConfig}
 *
 * Author: hasika
 * Time: 2018/3/21
 * Any questions can send email to lbhasika@gmail.com
 */

public class MultiDownloadManager {

    private ExecutorService mExecutorServices = Executors.newCachedThreadPool();
    private HashMap<String, DownloadTask> mUrl2DownloadTask = new HashMap<>();
    private Context mContext;


    public MultiDownloadManager(Context context){
        mContext = context.getApplicationContext();
        IntentFilter intentFilter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        context.registerReceiver(new MultiNetworkChangeReceiver(this), intentFilter);
    }

    public void getFilePathByUrl(final String url, final MultiDownloadListener listener) {
        getFilePathByUrl(url, true, listener);
    }

    public void getFilePathByUrl(final String url, final boolean download, final MultiDownloadListener listener) {
        mExecutorServices.execute(new Runnable() {
            @Override
            public void run() {
                if (mUrl2DownloadTask.containsKey(url)){
                    listener.onError(url, "downtask is running with url:"+url+", do not download again");
                    return;
                }
                getFilePathByUrlLocked(url, download, listener);
            }
        });
    }

    /**
     * get local file by url
     * <p>
     * if download is true , find file from db, if not exist. download from net then.
     * <p>
     * if download is false, find file from db only.
     *
     * @param url      http url
     * @param download can download from net or not
     * @param listener {@link MultiDownloadListener}
     */
    private void getFilePathByUrlLocked(String url, boolean download, MultiDownloadListener listener) {
        if (download) {
            String path = DatabaseTask.getFilePathByUrlAndFinished(mContext, url, true);
            if (TextUtils.isEmpty(path)) {
                DownloadTask downloadTask = new DownloadTask(mContext, mExecutorServices, url, listener);
                mUrl2DownloadTask.put(url, downloadTask);
                downloadTask.start();
            } else {
                listener.onCompleted(url, path);
            }
        } else {
            String path = DatabaseTask.getFilePathByUrlAndFinished(mContext, url, true);
            if (TextUtils.isEmpty(path)) {
                listener.onError(url, "not exist in sd");
            } else {
                listener.onCompleted(url, path);
            }
        }
    }

    public void getFilePathByPackageName(final String packageName, final MultiDownloadListener listener){
        getFilePathByPackageNameLocked(packageName, listener);
    }

    private void getFilePathByPackageNameLocked(final String packageName, final MultiDownloadListener listener){
        mExecutorServices.execute(new Runnable() {
            @Override
            public void run() {
                String path = DatabaseTask.getFilePathByPackageNameAndFinished(mContext, packageName);

                if (TextUtils.isEmpty(path)) {
                    listener.onError(packageName, "not exist in sd");
                } else {
                    listener.onCompleted(packageName, path);
                }
            }
        });
    }

    /**
     * cancelAll downloadTask if downloadTask is running
     * redownload next time
     */
    public void cancelAll(String reason) {
        for (DownloadTask downloadTask :
                mUrl2DownloadTask.values()) {
            if (downloadTask != null) {
                downloadTask.cancel(reason);
            }
        }

        mUrl2DownloadTask.clear();
    }

    public void cancel(String url, String reason) {
        DownloadTask downloadTask = findDownloadTaskByUrl(url);
        if (downloadTask != null) {
            downloadTask.cancel(reason);
            mUrl2DownloadTask.remove(url);
        }
    }

    public void pause(String url, String reason) {
        DownloadTask downloadTask = findDownloadTaskByUrl(url);
        if (downloadTask != null) {
            downloadTask.pause(reason);
            mUrl2DownloadTask.remove(url);
        }
    }

    /**
     * pauseAll download task if download task is running
     * can continue download next time
     */
    public void pauseAll(String reason) {
        for (DownloadTask downloadTask :
                mUrl2DownloadTask.values()) {
            if (downloadTask != null) {
                downloadTask.pause(reason);
            }
        }

        mUrl2DownloadTask.clear();
    }

    public void resume(String url){
        if (mUrl2DownloadTask.containsKey(url)){
            DownloadTask downloadTask = mUrl2DownloadTask.get(url);
            downloadTask.resume();
        }
    }

    public void resumeAll(){
        for (DownloadTask downlaodTask :
                mUrl2DownloadTask.values()) {
            downlaodTask.resume();
        }
    }

    private DownloadTask findDownloadTaskByUrl(String url) {
        DownloadTask downloadTask = null;
        if (mUrl2DownloadTask.containsKey(url)) {
            downloadTask = mUrl2DownloadTask.get(url);
        }

        return downloadTask;
    }

    public interface MultiDownloadListener {
        /**
         * completed!
         *
         * @param url  http url
         * @param path local path on sd
         */
        void onCompleted(String url, String path);

        /**
         * oh! error
         *
         * @param url http url
         * @param msg the reason for error
         */
        void onError(String url, String msg);

        /**
         * download change from net
         *
         * @param url         http url
         * @param currentSize already download size
         * @param totalSize   total size of the file
         */
        void onChange(String url, long currentSize, long totalSize);

        /**
         * pauseAll download task, can continue next time.
         *
         * @param url http url
         * @param msg the reason for pauseAll
         */
        void onPause(String url, String msg);

        /**
         * cancelAll download task, delete file, redownload next time.
         *
         * @param url http url
         * @param msg the reason for cancelAll
         */
        void onCancel(String url, String msg);
    }

}
