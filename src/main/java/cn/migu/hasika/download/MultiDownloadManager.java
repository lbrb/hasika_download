package cn.migu.hasika.download;

import android.content.Context;
import android.text.TextUtils;

import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import cn.migu.hasika.download.task.DatabaseTask;
import cn.migu.hasika.download.task.DownloadTask;

/**
 * Created by hasika on 2018/3/15.
 *
 * download file from net
 *
 * support multi-thread multi-task partial-download
 *
 * can config by {@link MulitDownloadConfig}
 *
 * good luck
 *
 */

public class MultiDownloadManager {

    private ExecutorService mExecutorServices = Executors.newCachedThreadPool();
    private HashMap<String, DownloadTask> mUrl2DownloadTask = new HashMap<>();

    public void getFilePathByUrl(final Context context, final String url, final DownloadListener listener) {
        getFilePathByUrl(context, url, true, listener);
    }

    public void getFilePathByUrl(final Context context, final String url, final boolean download, final DownloadListener listener) {
        mExecutorServices.execute(new Runnable() {
            @Override
            public void run() {
                if (mUrl2DownloadTask.containsKey(url)){
                    listener.onError(url, "downtask is running with url:"+url+", do not download again");
                    return;
                }
                getFilePathByUrlLocked(context, url, download, listener);
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
     * @param context  context
     * @param url      http url
     * @param download can download from net or not
     * @param listener {@link DownloadListener}
     */
    private void getFilePathByUrlLocked(Context context, String url, boolean download, DownloadListener listener) {
        if (download) {
            String path = DatabaseTask.getFilePathByUrlAndFinished(context, url, true);
            if (TextUtils.isEmpty(path)) {
                DownloadTask downloadTask = new DownloadTask(context, mExecutorServices, url, listener);
                mUrl2DownloadTask.put(url, downloadTask);
                downloadTask.start();
            } else {
                listener.onCompleted(url, path);
            }
        } else {
            String path = DatabaseTask.getFilePathByUrlAndFinished(context, url, true);
            if (TextUtils.isEmpty(path)) {
                listener.onError(url, "not exist in sd");
            } else {
                listener.onCompleted(url, path);
            }
        }
    }


    /**
     * cancelAll downloadTask if downloadTask is running
     * redownload next time
     */
    public void cancelAll() {
        for (DownloadTask downloadTask :
                mUrl2DownloadTask.values()) {
            if (downloadTask != null) {
                downloadTask.cancel();
            }
        }
    }

    public void cancel(String url) {
        DownloadTask downloadTask = findDownloadTaskByUrl(url);
        if (downloadTask != null) {
            downloadTask.cancel();
        }
    }

    public void pause(String url) {
        DownloadTask downloadTask = findDownloadTaskByUrl(url);
        if (downloadTask != null) {
            downloadTask.pause();
        }
    }

    private DownloadTask findDownloadTaskByUrl(String url) {
        DownloadTask downloadTask = null;
        if (mUrl2DownloadTask.containsKey(url)) {
            downloadTask = mUrl2DownloadTask.get(url);
        }

        return downloadTask;
    }

    /**
     * pauseAll download task if download task is running
     * can continue download next time
     */
    public void pauseAll() {
        for (DownloadTask downloadTask :
                mUrl2DownloadTask.values()) {
            if (downloadTask != null) {
                downloadTask.pause();
            }
        }
    }


    public interface DownloadListener {
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
