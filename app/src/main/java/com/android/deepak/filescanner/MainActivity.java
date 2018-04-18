package com.android.deepak.filescanner;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatButton;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import static com.android.deepak.filescanner.BundleKeys.EXTRA_AVG_FILESIZE;
import static com.android.deepak.filescanner.BundleKeys.EXTRA_EXT_FREQ;
import static com.android.deepak.filescanner.BundleKeys.EXTRA_LARGE_FILES;
import static com.android.deepak.filescanner.BundleKeys.EXTRA_SCAN_PROGERESS;
import static com.android.deepak.filescanner.ScanService.SCAN_COMPLETED;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private static final int PERMISSIONS_READ_REQUEST_CODE = 100;
    private static final String TAG = MainActivity.class.getSimpleName();
    private LinearLayout mDataContainer;
    private ProgressBar mProgressBar;
    private ListView mFilesListView, mExtListView;
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
        onNewIntent(getIntent());

        // Local broadcast receiver
        mBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.d(TAG, "onReceive:" + intent);

                switch (intent.getAction()) {
                    case SCAN_COMPLETED:
                        updateProgress(100);
                        isScanning= false;
                        onScanResultIntent(intent);
                        break;

                    case ScanService.SCAN_PROGRESS:
                        isScanning = true;
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
        if (intent.getAction() != null && intent.getAction().equals(SCAN_COMPLETED)) {
            isScanning = false;
            onScanResultIntent(intent);
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == PERMISSIONS_READ_REQUEST_CODE) {
            if (grantResults.length <= 0) {
                Log.i(TAG, "User interaction was cancelled.");
            } else if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {


            } else {

                showPermissionSnackbar();
            }

        }
    }

    protected void showPermissionSnackbar() {
        showMessageDialog(getString(R.string.permission_req),getString(R.string.permission_required_explanation),
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

        ArrayList<FileData> mFileSizeList = intent.getParcelableArrayListExtra(EXTRA_LARGE_FILES);
        ArrayList<ExtnFrequency> mExtFreqList = intent.getParcelableArrayListExtra(EXTRA_EXT_FREQ);
        long avgFileSize = intent.getLongExtra(EXTRA_AVG_FILESIZE, 0);

        updateTop10Files(mFileSizeList);
        updateTop5FreqExtn(mExtFreqList);
        updateAvgFileSize(avgFileSize);
    }

    private void updateAvgFileSize(long avgFileSize) {

    }

    private void updateTop5FreqExtn(ArrayList<ExtnFrequency> extFreqList) {
        mExtListView.setAdapter(new FreqAdapter(extFreqList));
    }

    private void updateTop10Files(ArrayList<FileData> fileSizeList) {
        mFilesListView.setAdapter(new FileSizeAdapter(fileSizeList));
    }


    private void updateProgress(int progress) {
        mProgressBar.setProgress(progress);
    }

    private void initVar() {
        mDataContainer = findViewById(R.id.scan_data_container);
        mProgressBar = findViewById(R.id.progressBar);
        mFilesListView = findViewById(R.id.large_files_table);
        mExtListView = findViewById(R.id.files_ext_table);
        mScanStopBtn = findViewById(R.id.start_stop_scan);
        mScanStopBtn.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {

        switch (v.getId()) {

            case R.id.start_stop_scan:
                Intent intent;
                isScanning = !isScanning;
                if (isScanning) {
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

    private class FileSizeAdapter extends ArrayAdapter<FileData> {

        Context mContext;
        private int lastPosition = -1;

        public FileSizeAdapter(ArrayList<FileData> data) {
            super(MainActivity.this, R.layout.list_item, data);


        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            FileData fileData = getItem(position);

            ViewHolder viewHolder;

            final View result;

            if (convertView == null) {

                viewHolder = new ViewHolder();
                LayoutInflater inflater = LayoutInflater.from(getContext());
                convertView = inflater.inflate(R.layout.list_item, parent, false);
                viewHolder.txtName = convertView.findViewById(R.id.name);
                viewHolder.txtValue = convertView.findViewById(R.id.value);

                result = convertView;

                convertView.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) convertView.getTag();
                result = convertView;
            }

            Animation animation = AnimationUtils.loadAnimation(mContext, (position > lastPosition) ? R.anim.up_from_bottom : R.anim.down_from_top);
            result.startAnimation(animation);
            lastPosition = position;

            viewHolder.txtName.setText(fileData.getFilename());
            viewHolder.txtValue.setText(String.valueOf(fileData.getFilesize()));
            // Return the completed view to render on screen
            return convertView;
        }

        // View lookup cache
        private class ViewHolder {
            TextView txtName;
            TextView txtValue;
        }
    }

    private class FreqAdapter extends ArrayAdapter<ExtnFrequency> {

        Context mContext;
        private int lastPosition = -1;

        public FreqAdapter(ArrayList<ExtnFrequency> data) {
            super(MainActivity.this, R.layout.list_item, data);


        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            ExtnFrequency extFreq = getItem(position);

            ViewHolder viewHolder;

            final View result;

            if (convertView == null) {

                viewHolder = new ViewHolder();
                LayoutInflater inflater = LayoutInflater.from(getContext());
                convertView = inflater.inflate(R.layout.list_item, parent, false);
                viewHolder.txtName = convertView.findViewById(R.id.name);
                viewHolder.txtValue = convertView.findViewById(R.id.value);

                result = convertView;

                convertView.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) convertView.getTag();
                result = convertView;
            }

            Animation animation = AnimationUtils.loadAnimation(mContext, (position > lastPosition) ? R.anim.up_from_bottom : R.anim.down_from_top);
            result.startAnimation(animation);
            lastPosition = position;

            viewHolder.txtName.setText(extFreq.getExtension());
            viewHolder.txtValue.setText(String.valueOf(extFreq.getFrequency()));
            // Return the completed view to render on screen
            return convertView;
        }

        // View lookup cache
        private class ViewHolder {
            TextView txtName;
            TextView txtValue;
        }
    }
}
