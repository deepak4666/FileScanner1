package com.android.deepak.filescanner;

import android.Manifest;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Parcelable;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatTextView;
import android.support.v7.widget.SwitchCompat;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.CompoundButton;
import android.widget.ExpandableListView;
import android.widget.ProgressBar;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static com.android.deepak.filescanner.BundleKeys.EXTRA_AVG_FILESIZE;
import static com.android.deepak.filescanner.BundleKeys.EXTRA_DATA;
import static com.android.deepak.filescanner.BundleKeys.EXTRA_EXT_FREQ;
import static com.android.deepak.filescanner.BundleKeys.EXTRA_LARGE_FILES;
import static com.android.deepak.filescanner.BundleKeys.EXTRA_SCAN_PROGERESS;
import static com.android.deepak.filescanner.BundleKeys.KEY_PROGRESS;
import static com.android.deepak.filescanner.ScanService.SCAN_COMPLETED;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSIONS_READ_REQUEST_CODE = 100;
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final String LIST_STATE = "list_state";
    private ProgressBar mProgressBar;
    private ExpandableListView mFileListView;
    private AppCompatTextView mProgressTxt;
    private SwitchCompat mScanToggle;

    private ArrayList<ScanData> dataList;
    private int progress = 0;

    private boolean isScanning = false;
    private BroadcastReceiver mBroadcastReceiver;
    private Parcelable mListState;


    public static boolean isRequestPermissionAllow(@NonNull Activity pContext,
                                                   @NonNull String[] pPermissions) {
        if (!isNotLowerThan(Build.VERSION_CODES.M)) {
            return true;
        }
        List<String> listPermissionsNeeded = new ArrayList<>();
        for (String p : pPermissions) {
            if (hasPermission(pContext, p)) {
                listPermissionsNeeded.add(p);
            }
        }
        if (!listPermissionsNeeded.isEmpty()) {
            pContext.requestPermissions(
                    listPermissionsNeeded.toArray(new String[listPermissionsNeeded.size()]),
                    PERMISSIONS_READ_REQUEST_CODE);
            return false;
        }
        return true;
    }

    public static boolean isNotLowerThan(int verName) {
        return Build.VERSION.SDK_INT >= verName;
    }

    public static boolean hasPermission(Context context, String permission) {

        int res = context.checkCallingOrSelfPermission(permission);

        return PackageManager.PERMISSION_GRANTED != res;

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initVar();

        if (savedInstanceState != null) {
            progress = savedInstanceState.getInt(KEY_PROGRESS, 0);
            dataList = savedInstanceState.getParcelableArrayList(EXTRA_DATA);
            updateData();
            updateProgress();
        } else {
            onNewIntent(getIntent());
        }

        mBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.d(TAG, "onReceive:" + intent);
                if (intent.getAction() == null) {
                    return;
                }
                switch (intent.getAction()) {
                    case SCAN_COMPLETED:
                        isScanning = false;
                        progress = 100;
                        updateProgress();
                        mScanToggle.setChecked(false);
                        invalidateOptionsMenu();
                        onScanResultIntent(intent);
                        break;

                    case ScanService.SCAN_PROGRESS:
                        invalidateOptionsMenu();
                        progress = intent.getIntExtra(EXTRA_SCAN_PROGERESS, 0);
                        updateProgress();
                        break;

                    default:
                        break;
                }
            }
        };

    }


    @Override
    public void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (intent.getAction() != null && intent.getAction().equals(SCAN_COMPLETED)) {
            isScanning = false;
            onScanResultIntent(intent);
        } else {
            updateData();
        }

    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mListState != null)
            mFileListView.onRestoreInstanceState(mListState);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelableArrayList(EXTRA_DATA, dataList);

        outState.putInt(KEY_PROGRESS, progress);
        mListState = mFileListView.onSaveInstanceState();
        outState.putParcelable(LIST_STATE, mListState);
    }


    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        dataList = savedInstanceState.getParcelableArrayList(EXTRA_DATA);
        progress = savedInstanceState.getInt(KEY_PROGRESS, 0);
        mListState = savedInstanceState.getParcelable(LIST_STATE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == PERMISSIONS_READ_REQUEST_CODE) {
            if (grantResults.length <= 0) {
                Log.i(TAG, "User interaction was cancelled.");
            } else if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                mScanToggle.setChecked(true);
            } else {
                showPermissionSnackbar();
            }

        }
    }

    protected void showPermissionSnackbar() {
        showMessageDialog(getString(R.string.permission_req), getString(R.string.permission_required_explanation),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        // Build intent that displays the App settings screen.
                        Intent intent = new Intent();
                        intent.setAction(
                                Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                        Uri uri = Uri.fromParts("package",
                                BuildConfig.APPLICATION_ID, null);
                        intent.setData(uri);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                        dialogInterface.dismiss();
                    }


                });
    }

    private void onScanResultIntent(Intent intent) {
        ArrayList<FileData> fileSizeList = intent.getParcelableArrayListExtra(EXTRA_LARGE_FILES);
        ArrayList<FileData> mExtFreqList = intent.getParcelableArrayListExtra(EXTRA_EXT_FREQ);
        double avgFileSize = intent.getDoubleExtra(EXTRA_AVG_FILESIZE, 0);
        List<FileData> avgFileList = new ArrayList<>();
        if (avgFileSize > 0) {
            avgFileList.add(new FileData(getString(R.string.avg_file_size_title), avgFileSize));
        }
        dataList = new ArrayList<>();
        dataList.add(new ScanData(getString(R.string.top_10_file_size), fileSizeList));
        dataList.add(new ScanData(getString(R.string.top_5_ext_freq), mExtFreqList));
        dataList.add(new ScanData(getString(R.string.avg_file_size_title), avgFileList));
        updateData();

    }


    private void updateData() {
        mFileListView.setAdapter(new ExpandableListAdapter(MainActivity.this, dataList));


    }


    private void updateProgress() {
        mProgressBar.setProgress(progress);
        mProgressTxt.setText(String.format(Locale.getDefault(), "%d %%", progress));
    }

    private void initVar() {
        mProgressBar = findViewById(R.id.progressBar);
        mFileListView = findViewById(R.id.dataList);
        mScanToggle = findViewById(R.id.start_stop_scan);

        mProgressTxt = findViewById(R.id.progress_data);
        mScanToggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    startScan();
                } else {
                    stopScan();
                }
            }
        });
        progress = 0;
        dataList = new ArrayList<>();
    }

    private void stopScan() {
        stopService(new Intent(this, ScanService.class));
        progress = 0;
        isScanning = false;
        updateProgress();
    }

    private void startScan() {
        if (!isRequestPermissionAllow(MainActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE})) {
            return;
        }

        String sdCardState = Environment.getExternalStorageState();
        if (!sdCardState.equals(Environment.MEDIA_MOUNTED)) {
            Toast.makeText(MainActivity.this, getString(R.string.no_storage), Toast.LENGTH_LONG).show();
        } else {
            if (!isMyServiceRunning(ScanService.class.getSimpleName())) {
                Intent intent = new Intent(this, ScanService.class)
                        .setAction(ScanService.ACTION_SCAN);
                startService(intent);
                isScanning = true;
            }

        }
    }


    @Override
    public void onStart() {
        super.onStart();

        LocalBroadcastManager manager = LocalBroadcastManager.getInstance(this);
        manager.registerReceiver(mBroadcastReceiver, ScanService.getIntentFilter());
    }


    @Override
    public void onStop() {
        super.onStop();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mBroadcastReceiver);

    }

    @Override
    public void onBackPressed() {
        showMessageDialog(getString(R.string.alert), getString(R.string.app_exit_alert), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                stopService(new Intent(MainActivity.this, ScanService.class));
                finish();
            }
        });

    }

    protected void showMessageDialog(String title, String message, DialogInterface.OnClickListener clickListener) {
        AlertDialog ad = new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(getString(android.R.string.ok), clickListener)
                .setNegativeButton(getString(android.R.string.cancel), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .create();

        ad.show();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.scan_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.findItem(R.id.share).setEnabled(!isScanning);
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if (item.getItemId() == R.id.share) {
            if (dataList.size() > 0) {
                Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
                sharingIntent.setType("text/plain");
                sharingIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "ScanData");
                sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, dataList.toString());
                startActivity(Intent.createChooser(sharingIntent, "Share via"));
            } else {
                Toast.makeText(MainActivity.this, "Please scan the storage.", Toast.LENGTH_LONG).show();
            }
        }


        return super.onOptionsItemSelected(item);
    }

    public boolean isMyServiceRunning(String className) {
        ActivityManager manager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        assert manager != null;
        for (ActivityManager.RunningServiceInfo service : manager
                .getRunningServices(Integer.MAX_VALUE)) {
            if ((getPackageName() + "." + className)
                    .equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

}
