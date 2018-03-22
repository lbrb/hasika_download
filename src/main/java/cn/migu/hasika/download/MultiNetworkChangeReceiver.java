package cn.migu.hasika.download;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;


/**
 * Author: hasika
 * Time: 2018/3/21
 * Any questions can send email to lbhasika@gmail.com
 */

public class MultiNetworkChangeReceiver extends BroadcastReceiver {
    private MultiDownloadManager mManager;

    public MultiNetworkChangeReceiver(MultiDownloadManager manager) {
        mManager = manager;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (ConnectivityManager.CONNECTIVITY_ACTION.equals(action)) {
            try {
                ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
                if (connectivityManager != null) {
                    NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();

                    if (networkInfo != null && networkInfo.isAvailable()) {
                        if (MulitDownloadConfig.DOWNLOAD_ONLY_WIFI && networkInfo.getType() != ConnectivityManager.TYPE_WIFI){
                            mManager.pauseAll("net not wifi");
                        } else {
                            mManager.resumeAll();
                        }
                    } else {
                        mManager.pauseAll("no net");
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
