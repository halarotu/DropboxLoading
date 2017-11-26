package fi.ha.service;

import com.dropbox.core.DbxException;
import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.*;
import fi.ha.util.FileMetadataWrapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class LoadingService {

    @Value("${db.accessToken:default}")
    private String ACCESS_TOKEN;
    @Value("${db.path_to_files:default}")
    private String PATH_TO_FILES;
    private final boolean IMMUTABLE_FOLDER = true;

    private DbxClientV2 client = null;
    private FileMetadataWrapper currentMetadata = null;
    private List<FileMetadataWrapper> currentSortedFileList = null;
    private boolean autoUpdateStopped = false;

    public FileMetadata getFile() {
        try {
            FileMetadataWrapper next = this.getNextMetadata();
            if (this.currentMetadata != null) {
                this.deleteOldFile(this.currentMetadata.getFileMetadata());
            }
            this.currentMetadata = next;

            Path pathToImages = FileSystems.getDefault().getPath("images");
            if (!Files.exists(pathToImages)) {
                Files.createDirectory(pathToImages);
            }
            OutputStream out = new FileOutputStream("images/" + next.getFileMetadata().getName());
            FileMetadata fmd = this.getClient().files().downloadBuilder(next.getFileMetadata().getPathLower()).download(out);
            this.currentMetadata.setFileMetadata(fmd);

            return fmd;
        } catch (Exception e) {
            System.out.println(e.toString());
        }
        return null;
    }

    public FileMetadata getCurrentMetadata() {
        if (this.currentMetadata == null) {
            return getFile();
        } else {
            return this.currentMetadata.getFileMetadata();
        }
    }

    public void setAutoUpdateStopped(boolean stopped) {
        this.autoUpdateStopped = stopped;
    }

    public boolean isAutoUpdateStopped() {
        return this.autoUpdateStopped;
    }

    public String getPath_To_Files() {
        return PATH_TO_FILES;
    }

    private void deleteOldFile(Metadata metadata) {
        if (metadata != null) {
            try {
                Files.delete(FileSystems.getDefault().getPath("images/" + metadata.getName()));
            } catch (Exception e) {
                System.out.println(e.toString());
            }
        }
    }

    private FileMetadataWrapper getNextMetadata() throws DbxException {
        List<FileMetadataWrapper> metadataList = this.getFileList();
        if (metadataList.isEmpty()) {return null;}

        int nextIndex = 0;
        if (this.currentMetadata != null && metadataList.contains(this.currentMetadata)) {
            int index = metadataList.indexOf(this.currentMetadata);
            index++;
            if (index < metadataList.size()) {
                nextIndex = index;
            }
        }

        return metadataList.get(nextIndex);
    }

    private DbxClientV2 getClient() {
        if (this.client == null) {
            DbxRequestConfig config = new DbxRequestConfig("dropbox-file-loading");
            this.client = new DbxClientV2(config, ACCESS_TOKEN);
        }
        return this.client;
    }

    private List<FileMetadataWrapper> getFileList() throws DbxException {
        if (!IMMUTABLE_FOLDER || this.currentSortedFileList == null) {
            ListFolderResult result = this.getClient().files().listFolder(PATH_TO_FILES);
            List<Metadata> list = result.getEntries();
            List<FileMetadataWrapper> fileMetadataList = new ArrayList<>();
            for (Metadata m : list) {
                if (m != null && m instanceof FileMetadata) {
                    fileMetadataList.add(new FileMetadataWrapper((FileMetadata) m));
                }
            }
            Collections.sort(fileMetadataList);
            this.currentSortedFileList = fileMetadataList;
            System.out.println("File list retrieved with " + fileMetadataList.size() + " files.");
        }

        return this.currentSortedFileList;
    }

}
