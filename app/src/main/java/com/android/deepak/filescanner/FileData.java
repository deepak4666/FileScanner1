package com.android.deepak.filescanner;

import android.os.Parcel;
import android.os.Parcelable;

public class FileData implements Parcelable
{

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

}
;
private String filename;
private long filesize;
private String extension;

protected FileData(Parcel in) {
this.filename = ((String) in.readValue((String.class.getClassLoader())));
this.filesize = ((long) in.readValue((long.class.getClassLoader())));
this.extension = ((String) in.readValue((String.class.getClassLoader())));
}

/**
* No args constructor for use in serialization
* 
*/
public FileData() {
}

/**
* 
* @param extension
* @param filesize
* @param filename
*/
public FileData(String filename, long filesize, String extension) {
super();
this.filename = filename;
this.filesize = filesize;
this.extension = extension;
}

public String getFilename() {
return filename;
}

public void setFilename(String filename) {
this.filename = filename;
}

public long getFilesize() {
return filesize;
}

public void setFilesize(long filesize) {
this.filesize = filesize;
}

public String getExtension() {
return extension;
}

public void setExtension(String extension) {
this.extension = extension;
}

public void writeToParcel(Parcel dest, int flags) {
dest.writeValue(filename);
dest.writeValue(filesize);
dest.writeValue(extension);
}

public int describeContents() {
return 0;
}

}