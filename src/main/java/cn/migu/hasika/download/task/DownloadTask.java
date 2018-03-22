package cn.migu.hasika.download.task;

import android.content.Context;
import android.os.Environment;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutorService;

import cn.migu.hasika.download.MulitDownloadConfig;
import cn.migu.hasika.download.MultiDownloadManager;
import cn.migu.hasika.download.connect.ConnectThread;
import cn.migu.hasika.download.connect.DownloadThread;
import cn.migu.hasika.download.database.FileEntry;

/**
 * Author: hasika
 * Time: 2018/3/21
 * Any questions can send email to lbhasika@gmail.com
 */

public class DownloadTask implements ConnectThread.ConnectListener, DownloadThread.DownloadListener {

    private Context mContext;
    private String mUrl;
    private MultiDownloadManager.MultiDownloadListener mListener;
    private FileEntry mFileEntry;
    private ConnectThread mConnectThread;
    private List<DownloadThread> mDownloadThreads = new ArrayList<>();
    private String mPauseReason;
    private String mCancelReason;
    private String mFilePath;
    private File mFile;
    private ExecutorService mExecutorService;
    private volatile int mState;
    private int cancelThreadNum = 0;
    private int pauseThreadNum = 0;

    public static final int DOWNLOAD_TASK_STATE_NEW = 0;
    public static final int DOWNLOAD_TASK_STATE_RUNNING = 1;
    public static final int DOWNLOAD_TASK_STATE_ERROR = 2;
    public static final int DOWNLOAD_TASK_STATE_FINISHED = 3;
    public static final int DOWNLOAD_TASK_STATE_PAUSE = 4;
    public static final int DOWNLOAD_TASK_STATE_CANCEL = 5;

    public DownloadTask(Context context, ExecutorService executorService, String url, MultiDownloadManager.MultiDownloadListener listener){
        mContext = context;
        mExecutorService = executorService;
        mUrl = url;
        mFilePath = getFilePathFromUrl(url);
        mFile = new File(mFilePath);
        mListener = listener;
        mState = DOWNLOAD_TASK_STATE_NEW;
    }

    private String getFilePathFromUrl(String url){
        String dirPath;
        String relative = "cmgame"+ File.separator+"plugin"+ File.separator+"apk";
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            dirPath = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + relative;
        } else {
            dirPath = mContext.getFilesDir() + File.separator + relative;
        }

        File dirFile = new File(dirPath);

        if (!dirFile.canWrite()){
            dirPath = mContext.getFilesDir() + File.separator + relative;
        }

        dirFile = new File(dirPath);

        if (!dirFile.exists()){
            boolean ret = dirFile.mkdirs();
        }

        int lastIndex = url.lastIndexOf("/");
        String filePath = dirPath + File.separator + url.substring(lastIndex+1);

        return filePath;
    }

    public void start(){
        startLocked();
    }

    private void startLocked(){
        mFileEntry = FileEntry.queryBySelection(mContext, FileEntry.class, "_url = ?", new String[]{mUrl});

        if (mFileEntry == null) {//no download, download now
            mFileEntry = new FileEntry(mContext, mUrl);
            startConnectLocked();
        } else { //download befor
            if (mFileEntry.isFinished().equals("1")){//already download finished, invoke listener
                mListener.onCompleted(mUrl, mFilePath);

            } else {//download not finished, continue.
                startDownloadLocked();
            }
        }
    }

    private void startConnectLocked(){
        mConnectThread = new ConnectThread(mUrl, this);
        mConnectThread.run();
        mState = DOWNLOAD_TASK_STATE_RUNNING;
    }

    private void startDownloadLocked(){
        long currentLocationL = mFileEntry.getCurrentLocation();
        long totalSizeL = mFileEntry.getTotalSize();
        HashMap<Integer, Long> range = mFileEntry.getRange();

        int threadNum = MulitDownloadConfig.MULTI_DOWNLOAD_THREAD_NUM;
        long block = totalSizeL/threadNum;

        if (range == null || range.size() == 0){
            range = new HashMap<>();
            for (int i = 0; i < MulitDownloadConfig.MULTI_DOWNLOAD_THREAD_NUM; i++) {
                range.put(i, i*block);
            }
            mFileEntry.setRange(range);
        }

        long startP;
        long endP;
        for (int i = 0; i < threadNum; i++) {
            startP = range.get(i);

            if (i == threadNum-1){
                endP = totalSizeL;
            } else {
                endP = (i+1)*block - 1;
            }

            addWorker(i, startP, endP);
        }

        mState = DOWNLOAD_TASK_STATE_RUNNING;
    }

    private void addWorker(int index, long startP, long endP){
        boolean isSupportRange = "1".equals(mFileEntry.isSupportRange());
        DownloadThread downloadThread = new DownloadThread(index, mUrl, mFilePath, isSupportRange,startP, endP, this);
        mDownloadThreads.add(downloadThread);
        mExecutorService.execute(downloadThread);
    }

    public void cancel(String reason){
        mCancelReason = reason;
        if (mState <= DOWNLOAD_TASK_STATE_RUNNING){
            for (DownloadThread downladThread :
                    mDownloadThreads) {
                downladThread.cancel();
            }
        }
    }

    public void pause(String reason){
        mPauseReason = reason;
        if (mFileEntry != null && "1".equals(mFileEntry.isSupportRange())&&mState <= DOWNLOAD_TASK_STATE_RUNNING){
            for (DownloadThread downloadThread :
                    mDownloadThreads) {
                downloadThread.pause();
            }
        }
    }

    public void resume(){
        startLocked();
    }

    /*****************connect listener********************/
    @Override
    public void onConnectSuccess(boolean isSupportRange, long totalSize) {
        mFileEntry.setSupportRange("1");
        mFileEntry.setTotalSize(totalSize);
        mFileEntry.setFinished("0");
        mFileEntry.setCurrentLocation(0);
        mFileEntry.setPath(mFilePath);
        long idex = mFileEntry.save();
        mFileEntry.setId(idex);
        startDownloadLocked();
    }

    @Override
    public void onConnectFailure(String msg) {
        mListener.onError(mUrl, msg);
    }

    /*******************download listener********************/
    @Override
    public void onProgressChanged(int index, long changeSize) {
        HashMap<Integer, Long> range = mFileEntry.getRange();
        long blockL = range.get(index);
        range.put(index, blockL+changeSize);

        long currentLocationL = mFileEntry.getCurrentLocation() + changeSize;
        long totalSizeL = mFileEntry.getTotalSize();

        mFileEntry.setCurrentLocation(currentLocationL);

        mListener.onChange(mUrl, currentLocationL, totalSizeL);

        if (currentLocationL == totalSizeL){
            mFileEntry.setFinished("1");
            mState = DOWNLOAD_TASK_STATE_FINISHED;
            mListener.onCompleted(mUrl, mFilePath);
        }

        mFileEntry.save();
    }

    @Override
    public void onDownloadPaused() {
        pauseThreadNum++;
        if (pauseThreadNum == MulitDownloadConfig.MULTI_DOWNLOAD_THREAD_NUM){
            mState = DOWNLOAD_TASK_STATE_PAUSE;
            mListener.onPause(mUrl, mPauseReason);
        }

    }

    @Override
    public void onDownloadCanceled() {
        cancelThreadNum++;
        if (cancelThreadNum == MulitDownloadConfig.MULTI_DOWNLOAD_THREAD_NUM){
            mState = DOWNLOAD_TASK_STATE_CANCEL;
            clearData();
            mListener.onCancel(mUrl, mCancelReason);
        }
    }
    
    @Override
    public void onDownloadError(String msg) {
        //if one thread invoke error, all thread pauseAll
        for (DownloadThread downloadThread :
                mDownloadThreads) {
            downloadThread.error();
        }

        mState = DOWNLOAD_TASK_STATE_ERROR;
        clearData();
        mListener.onError(mUrl, msg);
    }

    private void clearData(){
        mFileEntry.delete();
        mFile.delete();
    }
}
