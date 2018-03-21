package cn.migu.hasika.download.task;

import android.content.Context;

import java.util.List;

import cn.migu.hasika.download.database.FileEntry;

/**
 * Created by hasika on 2018/3/15.
 */

public class DatabaseTask {

    public static String getFilePathByUrlAndFinished(Context context, String url, boolean isFinished){
        String isFinishedStr = isFinished? "1":"0";
        List<FileEntry> list = FileEntry.queryAllBySelection(context, FileEntry.class, "_url = ? and _is_finished = ?", new String[]{url, isFinishedStr});
        if (list != null && list.size()>0){
            FileEntry fileEntry = list.get(0);
            if (fileEntry != null){
                return fileEntry.getPath();
            }
        }

        return null;
    }
}
