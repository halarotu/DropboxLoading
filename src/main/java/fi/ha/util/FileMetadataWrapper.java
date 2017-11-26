package fi.ha.util;

import com.dropbox.core.v2.files.FileMetadata;

import java.util.Date;

public class FileMetadataWrapper implements Comparable<FileMetadataWrapper> {

    private FileMetadata fileMetadata;

    public FileMetadataWrapper(FileMetadata fileMetadata) {
        this.fileMetadata = fileMetadata;
    }

    public FileMetadata getFileMetadata() {
        return this.fileMetadata;
    }

    public void setFileMetadata(FileMetadata metadata) {
        this.fileMetadata = metadata;
    }

    @Override
    public int compareTo(FileMetadataWrapper o) {
        Date date1 = this.fileMetadata.getClientModified();
        Date date2 = o.getFileMetadata().getClientModified();
        return date1.compareTo(date2);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj != null && obj instanceof FileMetadataWrapper) {
            return this.fileMetadata.equals(((FileMetadataWrapper) obj).getFileMetadata());
        }
        return false;
    }
}
