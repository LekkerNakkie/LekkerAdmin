package me.lekkernakkie.lekkeradmin.util;

import java.io.File;

public final class FileUtil {

    private FileUtil() {
    }

    public static boolean ensureFolderExists(File folder) {
        if (folder == null) {
            return false;
        }

        return folder.exists() || folder.mkdirs();
    }
}