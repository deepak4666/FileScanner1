package com.android.deepak.filescanner;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.List;

public class ScanData implements Parcelable {

    public final static Parcelable.Creator<ScanData> CREATOR = new Creator<ScanData>() {


        @SuppressWarnings({
                "unchecked"
        })
        public ScanData createFromParcel(Parcel in) {
            return new ScanData(in);
        }

        public ScanData[] newArray(int size) {
            return (new ScanData[size]);
        }

    };
    private String title;
    private List<FileData> fileData = null;

    protected ScanData(Parcel in) {
        this.title = ((String) in.readValue((String.class.getClassLoader())));
        in.readList(this.fileData, (FileData.class.getClassLoader()));
    }

    /**
     * No args constructor for use in serialization
     */
    public ScanData() {
    }

    /**
     * @param title
     * @param fileData
     */
    public ScanData(String title, List<FileData> fileData) {
        super();
        this.title = title;
        this.fileData = fileData;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public List<FileData> getFileData() {
        return fileData;
    }


    public void setFileData(List<FileData> fileData) {
        this.fileData = fileData;
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeValue(title);
        dest.writeList(fileData);
    }

    public int describeContents() {
        return 0;
    }


    @Override
    public String toString() {
        return "ScanData{" +
                "title='" + title + '\'' +
                ", fileData=" + fileData +
                '}';
    }
}