package com.android.deepak.filescanner;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;
import android.os.StatFs;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import static com.android.deepak.filescanner.BundleKeys.EXTRA_AVG_FILESIZE;
import static com.android.deepak.filescanner.BundleKeys.EXTRA_EXT_FREQ;
import static com.android.deepak.filescanner.BundleKeys.EXTRA_LARGE_FILES;
import static com.android.deepak.filescanner.BundleKeys.EXTRA_SCAN_PROGERESS;
import static com.android.deepak.filescanner.BundleKeys.KEY_MSG;

public class ScanService extends Service {

    /**
     * Actions
     **/
    public static final String ACTION_SCAN = "action_scan";
    public static final String SCAN_COMPLETED = "scan_completed";
    public static final String SCAN_PROGRESS = "scan_progress";
    private static final String TAG = ScanService.class.getSimpleName();
    private static final int PROGRESS_NOTIFICATION_ID = 0;
    private static final int FINISHED_NOTIFICATION_ID = 1;
    private static final String CHANNEL_ID_DEFAULT = "default";
    private List<FileData> fileList;
    private List<String> extensionList;
    private List<ExtnFrequency> extnList;
    private long totalSize = 0;
    private long scanSize = 0;
    private Set<String> extensionSet;

    public static IntentFilter getIntentFilter() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(SCAN_COMPLETED);

        return filter;
    }

    private void createDefaultChannel() {
        // Since android Oreo notification channel is needed.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

            NotificationChannel channel = new NotificationChannel(CHANNEL_ID_DEFAULT,
                    "Default",
                    NotificationManager.IMPORTANCE_DEFAULT);
            if (nm != null) {
                nm.createNotificationChannel(channel);
            }
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();

    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand:" + intent + ":" + startId);

        if (ACTION_SCAN.equals(intent.getAction())) {
            fileList = new ArrayList<>();
            extensionSet = new HashSet<>();
            extnList = new ArrayList<>();
            extensionList = new ArrayList<>();
            if (startScan()) {
                Collections.sort(fileList, new Comparator<FileData>() {
                    @Override
                    public int compare(FileData o1, FileData o2) {
                        return (int) (o2.getFilesize() - o1.getFilesize());
                    }
                });
                Log.e("FileList", fileList.toString());

                extensionSet = new HashSet<>(extensionList);
                int freq;
                for (String ext : extensionSet) {
                    freq = Collections.frequency(extensionList, ext);
                    extnList.add(new ExtnFrequency(ext, freq));
                }
                Collections.sort(extnList, new Comparator<ExtnFrequency>() {
                    @Override
                    public int compare(ExtnFrequency ef1, ExtnFrequency ef2) {
                        return ef2.getFrequency() - ef1.getFrequency();
                    }
                });
            }
        }

        return START_REDELIVER_INTENT;
    }

    private boolean startScan() {
        fileList = new ArrayList<>();
        File root = Environment.getExternalStorageDirectory();
        totalSize = busyMemory(root);
        return listFilesAndFilesSubDirectories(root);

    }

    public long busyMemory(File file) {
        StatFs statFs = new StatFs(file.getAbsolutePath());
        long total = (statFs.getBlockCount() * statFs.getBlockSize());
        long free = (statFs.getAvailableBlocks() * statFs.getBlockSize());
        return total - free;
    }

    public boolean listFilesAndFilesSubDirectories(File dir) {
        if (dir != null && dir.canRead()) {
            if (dir.isDirectory()) {
                //noinspection ConstantConditions
                for (File file : dir.listFiles()) {
                    if (file.isFile()) {
                        readFileData(file);
                    } else {
                        listFilesAndFilesSubDirectories(file);
                    }
                }
            } else if (dir.isFile()) {
                readFileData(dir);
            }
        }

        return true;
    }


    public String getExt(String filePath) {
        int strLength = filePath.lastIndexOf(".");
        if (strLength > 0)
            return filePath.substring(strLength + 1).toLowerCase();
        return null;
    }

    public void readFileData(File file) {
        scanSize += busyMemory(file);
        fileList.add(new FileData(file.getName(), file.length()));
        extensionList.add(getExt(file.getAbsolutePath()));
        broadcastScanProgress(scanSize, totalSize);
        showProgressNotification(String.format(Locale.getDefault(), "Scanning %d Files from %d", scanSize, totalSize), scanSize, totalSize);
    }

    /**
     * Show notification with a progress bar.
     */
    protected void showProgressNotification(String caption, long completedUnits, long totalUnits) {
        int percentComplete = 0;
        if (totalUnits > 0) {
            percentComplete = (int) (100 * completedUnits / totalUnits);
        }

        createDefaultChannel();
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID_DEFAULT)
                .setSmallIcon(R.drawable.ic_sd_storage)
                .setContentTitle(getString(R.string.app_name))
                .setContentText(caption)
                .setProgress(100, percentComplete, false)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setAutoCancel(false);

        NotificationManager manager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        if (manager != null) {
            manager.notify(PROGRESS_NOTIFICATION_ID, builder.build());
        }
    }

    protected void showFinishedNotification(String caption, Intent intent) {
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0 /* requestCode */, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        createDefaultChannel();
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID_DEFAULT)
                .setSmallIcon(R.drawable.ic_check_white_24)
                .setContentTitle(getString(R.string.app_name))
                .setContentText(caption)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        NotificationManager manager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        if (manager != null) {
            manager.notify(FINISHED_NOTIFICATION_ID, builder.build());
        }
    }

    private boolean broadcastScanProgress(long completedUnits, long totalUnits) {
        int percentComplete = 0;
        if (totalUnits > 0) {
            percentComplete = (int) (100 * completedUnits / totalUnits);
        }
        Intent broadcast = new Intent(SCAN_PROGRESS)
                .putExtra(EXTRA_SCAN_PROGERESS, percentComplete);
        return LocalBroadcastManager.getInstance(getApplicationContext())
                .sendBroadcast(broadcast);
    }

    private boolean broadcastScanFinished() {
        Intent broadcast = new Intent(SCAN_COMPLETED)
                .putParcelableArrayListExtra(EXTRA_LARGE_FILES, getTop10FileBySize())
                .putParcelableArrayListExtra(EXTRA_EXT_FREQ, getTop5FreqExt())
                .putExtra(EXTRA_AVG_FILESIZE, getAvgFileSize())
                .putExtra(KEY_MSG, getString(R.string.scan_success));
        return LocalBroadcastManager.getInstance(getApplicationContext())
                .sendBroadcast(broadcast);
    }

    private long getAvgFileSize() {
        return totalSize / fileList.size();
    }

    private void showScanFinishedNotification() {
        dismissProgressNotification();
        Intent intent = new Intent(this, MainActivity.class)
                .setAction(SCAN_COMPLETED)
                .putParcelableArrayListExtra(EXTRA_LARGE_FILES, getTop10FileBySize())
                .putParcelableArrayListExtra(EXTRA_EXT_FREQ, getTop5FreqExt())
                .putExtra(EXTRA_AVG_FILESIZE, getAvgFileSize())
                .putExtra(KEY_MSG, getString(R.string.scan_success));
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        showFinishedNotification(getString(R.string.scan_success), intent);
    }

    private ArrayList<FileData> getTop10FileBySize() {
        if (fileList.size() > 10) {
            return new ArrayList<>(fileList.subList(0, 9));
        } else {
            return new ArrayList<>(fileList);
        }
    }

    private ArrayList<ExtnFrequency> getTop5FreqExt() {
        if (extnList.size() > 5) {
            return new ArrayList<>(extnList.subList(0, 4));
        } else {
            return new ArrayList<>(extnList);
        }
    }

    protected void dismissProgressNotification() {
        NotificationManager manager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        if (manager != null) {
            manager.cancel(PROGRESS_NOTIFICATION_ID);
        }
    }

    @Override
    public void onDestroy() {
        dismissProgressNotification();
        super.onDestroy();

    }
}
