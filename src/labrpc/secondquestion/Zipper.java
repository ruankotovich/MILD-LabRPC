package labrpc.secondquestion;

import labrpc.secondquestion.model.ProgressListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

final class ZipUtils {

    static void unzip(File fileZip, File baseDirectory, ProgressListener progress) throws FileNotFoundException, IOException {
        byte[] buffer = new byte[4096];
        progress.onStart("Extracting received file...");
        ZipInputStream zis = new ZipInputStream(new FileInputStream(fileZip));
        ZipEntry zipEntry = zis.getNextEntry();

        int accumulator = 0;
        while (zipEntry != null) {
            String fileName = zipEntry.getName();
            File newFile = new File(baseDirectory.getAbsolutePath() + "/" + fileName);
            newFile.getParentFile().mkdirs();

            FileOutputStream fos = new FileOutputStream(newFile);
            int len;
            while ((len = zis.read(buffer)) > 0) {

                fos.write(buffer, 0, len);
                accumulator += len;
            }

            progress.notifyProgress("File Extracted: " + zipEntry, (int) ((accumulator / (float) fileZip.length()) * (100)));
            fos.close();
            zipEntry = zis.getNextEntry();
        }
        progress.then("Finished extraction");

        zis.closeEntry();
    }
}

public class Zipper {

    private final List<String> fileList;
    private final String SOURCE_FOLDER;
    private final String OUTPUT_ZIP_FILE;
    private final String FANTASY_NAME;
    private int progress = 0;
    private File curFile = null;
    private ProgressListener progressListener;

    public Zipper(String source, String fantasyName, String output) {
        fileList = new ArrayList<>();
        SOURCE_FOLDER = source;
        OUTPUT_ZIP_FILE = output;
        FANTASY_NAME = fantasyName;
    }

    /**
     *
     * @param handler
     * @return
     */
    public Thread zip(ProgressListener handler) {
        Thread tr = new Thread(() -> {
            this.progressListener = handler;
            handler.onStart("Ziping folder " + FANTASY_NAME + "...");
            this.generateFileList(new File(SOURCE_FOLDER));
            this.zipIt(OUTPUT_ZIP_FILE);
            handler.then("Finished zip.");
        });

        return tr;
    }

    public File getCurFile() throws Exception {
        File f = curFile;
        if (curFile == null) {
            throw new Exception("There's no file to get");
        } else {
            curFile = null;
        }
        return f;
    }

    public synchronized int getProgress() {
        return progress;
    }

    public synchronized void setProgress(int progress) {
        this.progress = progress;
    }

    private boolean zipIt(String zipFile) {
        boolean successfull = false;
        byte[] buffer = new byte[1024];
        String source = new File(SOURCE_FOLDER).getName();
        FileOutputStream fos = null;
        ZipOutputStream zos = null;
        int count = 0;

        try {
            fos = new FileOutputStream(zipFile);
            zos = new ZipOutputStream(fos);

            FileInputStream in = null;

            for (String file : this.fileList) {
                setProgress((int) ((++count / (float) fileList.size()) * 100));
                progressListener.notifyProgress("File Added: " + file, progress);
                ZipEntry ze = new ZipEntry(source + File.separator + file);
                zos.putNextEntry(ze);
                try {
                    in = new FileInputStream(SOURCE_FOLDER + File.separator + file);
                    int len;
                    while ((len = in.read(buffer)) > 0) {
                        zos.write(buffer, 0, len);
                    }
                } finally {
                    if (in != null) {
                        in.close();
                    }
                }
            }

            zos.closeEntry();
            System.out.println("Folder successfully compressed");
            successfull = true;

        } catch (IOException ex) {
            System.err.println(ex);
        } finally {
            try {
                if (zos != null) {
                    zos.close();
                }
            } catch (IOException e) {
                System.err.println(e);
            }
        }

        return successfull;

    }

    private void generateFileList(File node) {

        if (node.isFile()) {
            fileList.add(generateZipEntry(node.toString()));
        }

        if (node.isDirectory()) {
            String[] subNote = node.list();
            for (String filename : subNote) {
                generateFileList(new File(node, filename));
            }
        }
    }

    private String generateZipEntry(String file) {
        return file.substring(SOURCE_FOLDER.length() + 1, file.length());
    }
}
