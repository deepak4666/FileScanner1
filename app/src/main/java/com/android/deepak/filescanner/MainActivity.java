package com.android.deepak.filescanner;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatButton;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TableLayout;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import static com.android.deepak.filescanner.BundleKeys.EXTRA_SCAN_PROGERESS;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private static final int PERMISSIONS_READ_REQUEST_CODE = 100;
    private static final String TAG = MainActivity.class.getSimpleName();
    private LinearLayout mDataContainer;
    private ProgressBar mProgressBar;
    private TableLayout mLargeFilesTable, mFileExtTable;
    private AppCompatButton mScanStopBtn;
    private boolean isScanning = false;
    private BroadcastReceiver mBroadcastReceiver;

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

        }

        onNewIntent(getIntent());

        // Local broadcast receiver
        mBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.d(TAG, "onReceive:" + intent);

                switch (intent.getAction()) {
                    case ScanService.SCAN_COMPLETED:
                        updateProgress(100);
                        break;
                    case ScanService.SCAN_ABORT:
                        updateProgress(0);
                        break;
                    case ScanService.SCAN_PROGRESS:
                        int progress = intent.getIntExtra(EXTRA_SCAN_PROGERESS, 0);
                        updateProgress(progress);
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

        // Check if this Activity was launched by clicking on an upload notification
        if (intent.hasExtra(ScanService.EXTRA_DOWNLOAD_URL)) {
            onUploadResultIntent(intent);
        }

    }

    private void updateProgress(int progress) {
        mProgressBar.setProgress(progress);
    }

    private void initVar() {
        mDataContainer = findViewById(R.id.scan_data_container);
        mProgressBar = findViewById(R.id.progressBar);
        mLargeFilesTable = findViewById(R.id.large_files_table);
        mFileExtTable = findViewById(R.id.files_ext_table);
        mScanStopBtn = findViewById(R.id.start_stop_scan);
        mScanStopBtn.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {

        switch (v.getId()) {

            case R.id.start_stop_scan:
                Intent intent;
                if (!isScanning) {


                    String sdCardState = Environment.getExternalStorageState();
                    if (!sdCardState.equals(Environment.MEDIA_MOUNTED)) {
                        Toast.makeText(MainActivity.this, getString(R.string.no_storage), Toast.LENGTH_LONG).show();
                        return;
                    } else {
                        intent = new Intent(this, ScanService.class)
                                .setAction(ScanService.ACTION_SCAN);
                        startService(intent);
                    }
                } else {
                    stopService(new Intent(this, ScanService.class));
                }
                break;

            default:
                break;

        }

    }

    @Override
    public void onBackPressed() {
        showMessageDialog(getString(R.string.alert), getString(R.string.app_exit_alert), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
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
}
