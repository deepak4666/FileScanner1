package com.android.deepak.filescanner;

import java.io.File;
import java.io.FilenameFilter;
import java.util.HashSet;
import java.util.Set;

public class FileExtensionFilter implements FilenameFilter {
    private Set<String> filteredExtensions;

    public FileExtensionFilter() {
        filteredExtensions = new HashSet<String>();
    }

    @Override
    public boolean accept(File dir, String name) {
        boolean accept = true;
        for (String filteredExtension : filteredExtensions) {
            accept = accept && !name.endsWith(filteredExtension);
        }
        return accept;
    }

    public void addFilteredExtension(String extension) {
        filteredExtensions.add(extension);
    }
}