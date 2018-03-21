package cn.migu.hasika.download.database;

import android.content.Context;

import java.util.HashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import cn.migu.hasika.database.DatabaseColumnInfo;
import cn.migu.hasika.database.DatabaseEntry;
import cn.migu.hasika.database.annotation.DatabaseField;
import cn.migu.hasika.database.annotation.DatabaseTable;

/**
 * Author: hasika
 * Time: 2018/3/21
 * Any questions can send email to lbhasika@gmail.com
 */

@DatabaseTable("files")
public class FileEntry extends DatabaseEntry {

    public FileEntry(Context context){
        super(context);
    }

    public FileEntry(Context context, String url) {
        super(context);
        this.url = url;
    }

    @DatabaseField("_url")
    private String url;

    @DatabaseField("_path")
    private String path;

    @DatabaseField("_md5")
    private String md5;

    @DatabaseField("_package_name")
    private String packageName;

    @DatabaseField(value = "_total_size", type = DatabaseColumnInfo.ColumnTypeConstant.LONG)
    private long totalSize;

    @DatabaseField(value = "_current_location", type = DatabaseColumnInfo.ColumnTypeConstant.LONG)
    private long currentLocation;

    @DatabaseField("_is_finished")
    private String isFinished;

    @DatabaseField("_is_support_range")
    private String isSupportRange;

    @DatabaseField(value = "_range", type = DatabaseColumnInfo.ColumnTypeConstant.HASH_MAP)
    private HashMap<Integer, Long> range;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getMd5() {
        return md5;
    }

    public void setMd5(String md5) {
        this.md5 = md5;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public long getTotalSize() {
        return totalSize;
    }

    public void setTotalSize(long totalSize) {
        this.totalSize = totalSize;
    }

    public long getCurrentLocation() {
        return currentLocation;
    }

    public void setCurrentLocation(long currentLocation) {
        this.currentLocation = currentLocation;
    }

    public String isFinished() {
        return isFinished;
    }

    public void setFinished(String finished) {
        isFinished = finished;
    }

    public String isSupportRange() {
        return isSupportRange;
    }

    public void setSupportRange(String isSupportRange) {
        this.isSupportRange = isSupportRange;
    }

    public HashMap<Integer, Long> getRange(){
        return range;
    }

    public void setRange(HashMap<Integer, Long> hashMap){
        range = hashMap;
    }
}
