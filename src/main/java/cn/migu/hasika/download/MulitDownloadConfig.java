package cn.migu.hasika.download;

import android.os.Environment;

import java.io.File;

/**
 * Author: hasika
 * Time: 2018/3/21
 * Any questions can send email to lbhasika@gmail.com
 */

public final class MulitDownloadConfig {
    private static final String relative = "cmgame"+ File.separator+"plugin"+File.separator+"apk";

    static {
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            DOWNLOAD_DIR = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + relative;
        } else {
            DOWNLOAD_DIR = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + relative;
        }
    }

    public static final String DOWNLOAD_DIR;

    /**
     * the timeout for link
     */
    public static final int CONNECT_TIME = 10*1000;

    /**
     * the timeout for read
     */
    public static final int READ_TIME = 10*1000;

    /**
     * the num of thread to download
     */
    public static final int MULTI_DOWNLOAD_THREAD_NUM = 2;

}
