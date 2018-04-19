package com.android.deepak.filescanner;

import android.os.Parcel;
import android.os.Parcelable;

public class FileData implements Parcelable {

    public final static Parcelable.Creator<FileData> CREATOR = new Creator<FileData>() {


        @SuppressWarnings({
                "unchecked"
        })
        public FileData createFromParcel(Parcel in) {
            return new FileData(in);
        }

        public FileData[] newArray(int size) {
            return (new FileData[size]);
        }

    };
    private String name;
    private double value;

    protected FileData(Parcel in) {
        this.name = ((String) in.readValue((String.class.getClassLoader())));
        this.value = ((double) in.readValue((double.class.getClassLoader())));
    }

    /**
     * No args constructor for use in serialization
     */
    public FileData() {
    }

    /**
     * @param name
     * @param value
     */
    public FileData(String name, double value) {
        super();
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getValue() {
        return value;
    }

    public void setValue(long value) {
        this.value = value;
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeValue(name);
        dest.writeValue(value);
    }

    public int describeContents() {
        return 0;
    }

    @Override
    public String toString() {
        return "{" +
                "name='" + name + '\'' +
                ", value=" + value +
                '}';
    }
}