package cc.thas.http;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Map;

@Slf4j
public class DownloadUtil {

    public static void main(String[] args) throws IOException {
        String downloadUrl = "https://home.thas.cc:8443/test.tmp";
        String fileName = "test.tmp";
        long bufferLength = 1024 * 1024;

        downloadToFile(new File("").getAbsolutePath(), null, downloadUrl, bufferLength);
    }

    public static void downloadToFile(String dir, String fileName, String downloadUrl, long bufferLength) throws IOException {
        HttpResult headResult = HttpClientUtil.head(downloadUrl);
        if (fileName == null) {
            fileName = getFileName(headResult.getContentDisposition(), downloadUrl, "undefined");
        }

        long totalLength = headResult.getContentLength();

        File savedDir = new File(dir);
        if (savedDir.exists() && savedDir.isFile()) {
            throw new RuntimeException();
        }
        savedDir.mkdirs();
        File savedFile = new File(savedDir, fileName);
        if (savedFile.exists() && savedFile.isDirectory()) {
            throw new RuntimeException();
        }

        while (!downloadPartToFile(savedFile, downloadUrl, totalLength, bufferLength)) {
        }

    }

    private static boolean downloadPartToFile(File savedFile, String downloadUrl, long totalLength, long bufferLength) throws IOException {
        long start = 0L;
        if (savedFile.exists() && savedFile.length() > 0L) {
            start = savedFile.length();
        }
        if (start >= totalLength) {
            return true;
        }

        if (bufferLength <= 0L) {
            HttpResult getResult = HttpClientUtil.get(downloadUrl);
            FileUtils.writeByteArrayToFile(savedFile, getResult.getContent(), false);
        } else {
            long end = Math.min(totalLength, start + bufferLength);
            Map<String, String> rangeHeader = Collections.singletonMap("Range", "bytes=" + start + "-" + end);
            HttpResult getResult = HttpClientUtil.get(downloadUrl, rangeHeader);

            String contentRange = getResult.getHeader("Content-Range");
            if (contentRange == null) {
                log.info("不支持断点续传");
                FileUtils.writeByteArrayToFile(savedFile, getResult.getContent(), false);
                return true;
            } else {
                FileUtils.writeByteArrayToFile(savedFile, getResult.getContent(), true);
            }
        }
        return false;
    }


    private static void writeFile(InputStream inputStream, File file, boolean append) throws IOException {
        try (FileOutputStream fileOutputStream = new FileOutputStream(file, append)) {
            IOUtils.copy(inputStream, fileOutputStream);
        }
    }

    private static String getFileName(String contentDisposition, String downloadUrl, String defaultName) {
        if (contentDisposition != null) {
            int idx = contentDisposition.indexOf("filename=");
            if (idx > -1) {
                return contentDisposition.substring(idx + 9);
            }
        }
        if (downloadUrl != null) {
            int idx = downloadUrl.lastIndexOf("/");
            if (idx > -1) {
                return downloadUrl.substring(idx + 1);
            }
        }
        return defaultName;
    }
}
