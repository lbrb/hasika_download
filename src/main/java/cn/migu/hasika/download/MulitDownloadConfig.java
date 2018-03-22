package cn.migu.hasika.download;

/**
 * Author: hasika
 * Time: 2018/3/21
 * Any questions can send email to lbhasika@gmail.com
 */

public final class MulitDownloadConfig {
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

    /**
     * if true, only wifi status can download file.
     * else download file ignore net type.
     */
    public static final boolean DOWNLOAD_ONLY_WIFI = true;

}
