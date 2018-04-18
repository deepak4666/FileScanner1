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
import java.util.Locale;
import java.util.Set;

import static com.android.deepak.filescanner.BundleKeys.EXTRA_AVG_FILESIZE;
import static com.android.deepak.filescanner.BundleKeys.EXTRA_EXT_FREQ;
import static com.android.deepak.filescanner.BundleKeys.EXTRA_LARGE_FILES;
import static com.android.deepak.filescanner.BundleKeys.EXTRA_SCAN_PROGERESS;

public class ScanService extends Service {

    /**
     * Actions
     **/
    public static final String ACTION_SCAN = "action_scan";
    public static final String SCAN_COMPLETED = "scan_completed";
    public static final String SCAN_ABORT = "scan_abort";
    public static final String SCAN_PROGRESS = "scan_progress";
    private static final String TAG = ScanService.class.getSimpleName();
    private static final int PROGRESS_NOTIFICATION_ID = 0;
    private static final int FINISHED_NOTIFICATION_ID = 1;
    private static final String CHANNEL_ID_DEFAULT = "default";
    private ArrayList<File> fileList;
    private long totalSize = 0;
    private long fileNo = 0;
    private long scanSize = 0;
    private FileExtensionFilter fileExtensionFilter;

    public static IntentFilter getIntentFilter() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(SCAN_COMPLETED);
        filter.addAction(SCAN_ABORT);

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
        fileList = new ArrayList<>();
        fileExtensionFilter = new FileExtensionFilter();
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
            if (startScan()) {
                Collections.sort(fileList, new Comparator<File>() {
                    @Override
                    public int compare(File o1, File o2) {
                        return (int) (o1.length() - o2.length());
                    }
                });
                Log.e("FileList", fileList.toString());
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
        Set<String> extensions = new HashSet<String>();
        if (dir.isDirectory()) {
            for (File file : dir.listFiles(fileExtensionFilter)) {
                if (file.isFile()) {
                    fileNo++;
                    scanSize += busyMemory(file);
                    fileList.add(file);
                    broadcastScanProgress(scanSize, totalSize);
                    showProgressNotification(String.format(Locale.getDefault(), "Scanning %d Files from %d", scanSize, totalSize), scanSize, totalSize);
                } else {
                    listFilesAndFilesSubDirectories(file);
                }
            }
        } else if (dir.isFile()) {
            fileNo++;
            scanSize += busyMemory(dir);
            fileList.add(dir);
            broadcastScanProgress(scanSize, totalSize);
            showProgressNotification(String.format(Locale.getDefault(), "Scanning %d Files from %d", scanSize, totalSize), scanSize, totalSize);
        }

        return true;
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
                .setAutoCancel(false);

        NotificationManager manager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        if (manager != null) {
            manager.notify(PROGRESS_NOTIFICATION_ID, builder.build());
        }
    }

    protected void showFinishedNotification(String caption, Intent intent, boolean success) {
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0 /* requestCode */, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        int icon = success ? R.drawable.ic_check_white_24 : R.drawable.ic_error_white_24dp;

        createDefaultChannel();
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID_DEFAULT)
                .setSmallIcon(icon)
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

    private boolean broadcastScanFinished(boolean success) {
        String action = success ? SCAN_COMPLETED : SCAN_ABORT;

        Intent broadcast = new Intent(action)
                .putParcelableArrayListExtra(EXTRA_LARGE_FILES, )
                .putParcelableArrayListExtra(EXTRA_EXT_FREQ, )
                .putExtra(EXTRA_AVG_FILESIZE, getAvgFileSize());
        return LocalBroadcastManager.getInstance(getApplicationContext())
                .sendBroadcast(broadcast);
    }

    private long getAvgFileSize() {
        return totalSize / fileNo;
    }

    private void showScanFinishedNotification(boolean success) {
        dismissProgressNotification();

        Intent intent = new Intent(this, MainActivity.class)
                /*.putParcelableArrayListExtra(EXTRA_LARGE_FILES, )
                .putParcelableArrayListExtra(EXTRA_EXT_FREQ, )
                .putExtra(EXTRA_AVG_FILESIZE, )*/
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        String caption = success ? getString(R.string.scan_success) : getString(R.string.scan_abort);
        showFinishedNotification(caption, intent, true);
    }

    /**
     * Dismiss the progress notification.
     */
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
