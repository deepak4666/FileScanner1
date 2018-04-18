package com.android.deepak.filescanner;

import android.os.Parcel;
import android.os.Parcelable;

public class ExtnFrequency implements Parcelable {

    public final static Parcelable.Creator<ExtnFrequency> CREATOR = new Creator<ExtnFrequency>() {


        @SuppressWarnings({
                "unchecked"
        })
        public ExtnFrequency createFromParcel(Parcel in) {
            return new ExtnFrequency(in);
        }

        public ExtnFrequency[] newArray(int size) {
            return (new ExtnFrequency[size]);
        }

    };
    private String extension;
    private int frequency;

    protected ExtnFrequency(Parcel in) {
        this.extension = ((String) in.readValue((String.class.getClassLoader())));
        this.frequency = ((int) in.readValue((int.class.getClassLoader())));
    }

    /**
     * No args constructor for use in serialization
     */
    public ExtnFrequency() {
    }

    /**
     * @param extension
     * @param frequency
     */
    public ExtnFrequency(String extension, int frequency) {
        super();
        this.extension = extension;
        this.frequency = frequency;
    }

    public String getExtension() {
        return extension;
    }

    public void setExtension(String extension) {
        this.extension = extension;
    }

    public int getFrequency() {
        return frequency;
    }

    public void setFrequency(int frequency) {
        this.frequency = frequency;
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeValue(extension);
        dest.writeValue(frequency);
    }

    public int describeContents() {
        return 0;
    }

}
