package fi.ha.service;

import com.dropbox.core.DbxException;
import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.*;
import fi.ha.util.FileMetadataWrapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
public class LoadingService {

    @Value("${db.accessToken:default}")
    private String ACCESS_TOKEN;
    @Value("${db.path_to_files:default}")
    private String PATH_TO_FILES;
    private final boolean IMMUTABLE_FOLDER = true;

    private DbxClientV2 client = null;
    private FileMetadataWrapper currentMetadata = null;
    private long currentMetadataSetTime = 0;
    private FileMetadataWrapper latestDownloadedMetadata = null;
    private List<FileMetadataWrapper> currentSortedFileList = null;
    private boolean autoUpdateStopped = false;
    private boolean downloadInProgress = false;

    public FileMetadata getFileMetadata(boolean previous) {
        try {
            FileMetadataWrapper old = null;
            if (this.currentMetadata == null) {
                CompletableFuture<Boolean> downloadFile = this.downloadFile(this.getNextMetadata());
                CompletableFuture.allOf(downloadFile).join();
                this.currentMetadata = this.latestDownloadedMetadata;
                this.currentMetadataSetTime = System.currentTimeMillis();
            } else if (previous) {
                CompletableFuture<Boolean> downloadFile = this.downloadFile(this.getPreviousMetadata());
                CompletableFuture.allOf(downloadFile).join();
                old = this.currentMetadata;
                this.currentMetadata = this.latestDownloadedMetadata;
                this.currentMetadataSetTime = System.currentTimeMillis();
            } else if (System.currentTimeMillis() - this.currentMetadataSetTime > 10000
                    && !autoUpdateStopped) {
                if (!this.currentMetadata.equals(this.latestDownloadedMetadata)) {
                    old = this.currentMetadata;
                    this.currentMetadata = this.latestDownloadedMetadata;
                    this.currentMetadataSetTime = System.currentTimeMillis();
                }
            }

            if (old != null) {
                this.deleteOldFile(old.getFileMetadata());
            }
            if (this.currentMetadata.equals(this.latestDownloadedMetadata)) {
                downloadFile(this.getNextMetadata());
            }

            return this.currentMetadata.getFileMetadata();

        } catch (Exception e) {
            System.out.println(e.toString());
        }
        return null;
    }

    public FileMetadata getCurrentMetadata() {
        return this.currentMetadata.getFileMetadata();
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

    public boolean rotateImage(FileMetadata fileMetadata) {
        boolean rotate = false;
        if (fileMetadata.getMediaInfo().getMetadataValue().getDimensions().getHeight() >
                fileMetadata.getMediaInfo().getMetadataValue().getDimensions().getWidth()) {
            rotate = true;
        }
        return rotate;
    }

    @Async
    private CompletableFuture<Boolean> downloadFile(FileMetadataWrapper file) throws Exception {
        boolean ok = false;
        if (!downloadInProgress) {
            downloadInProgress = true;
            ok = true;
            try {
                Path pathToImages = FileSystems.getDefault().getPath("images");
                if (!Files.exists(pathToImages)) {
                    Files.createDirectory(pathToImages);
                }
                OutputStream out = new FileOutputStream("images/" + file.getFileMetadata().getName());
                FileMetadata fmd = this.getClient().files().downloadBuilder(file.getFileMetadata().getPathLower()).download(out);

                FileMetadataWrapper newMetadata = new FileMetadataWrapper(fmd);
                this.latestDownloadedMetadata = newMetadata;
            } catch (Exception e) {
                System.out.println(e.toString());
                ok = false;
            }
            downloadInProgress = false;
        }
        return CompletableFuture.completedFuture(ok);
    }

    @Async
    private void deleteOldFile(Metadata metadata) {
        if (metadata != null) {
            try {
                Files.delete(FileSystems.getDefault().getPath("images/" + metadata.getName()));
            } catch (Exception e) {
                System.out.println(e.toString());
            }
        }
    }

    private FileMetadataWrapper getPreviousMetadata() throws DbxException {
        List<FileMetadataWrapper> metadataList = this.getFileList();
        if (metadataList.isEmpty()) {return null;}

        int nextIndex = 0;
        if (this.currentMetadata != null && metadataList.contains(this.currentMetadata)) {
            int index = metadataList.indexOf(this.currentMetadata);
            index--;
            nextIndex = (index < 0) ? metadataList.size() + index : index;
        }
        return metadataList.get(nextIndex);
    }

    private FileMetadataWrapper getNextMetadata() throws DbxException {
        List<FileMetadataWrapper> metadataList = this.getFileList();
        if (metadataList.isEmpty()) {return null;}

        int nextIndex = 0;
        if (this.currentMetadata != null && metadataList.contains(this.currentMetadata)) {
            int index = metadataList.indexOf(this.currentMetadata);
            index++;
            nextIndex = (index < metadataList.size()) ? index : 0;
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
