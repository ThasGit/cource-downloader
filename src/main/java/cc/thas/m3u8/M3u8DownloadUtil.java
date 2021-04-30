package cc.thas.m3u8;

import cc.thas.http.HttpClientUtil;
import cc.thas.http.HttpResult;
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import net.bramp.ffmpeg.FFmpeg;
import net.bramp.ffmpeg.FFmpegExecutor;
import net.bramp.ffmpeg.builder.FFmpegBuilder;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.concurrent.BasicThreadFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static cc.thas.m3u8.M3u8DecryptUtil.decrypt;

@Slf4j
public class M3u8DownloadUtil {

    static int CORE_SIZE = Runtime.getRuntime().availableProcessors();
    static final ExecutorService SUBMIT_TASK_EXECUTOR_SERVICE = new ThreadPoolExecutor(
            1, CORE_SIZE, 0L, TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<>(50),
            new BasicThreadFactory.Builder().namingPattern("Thread-SubmitTask-%s").build(),
            new ThreadPoolExecutor.CallerRunsPolicy());

    static final ExecutorService EXECUTOR_SERVICE = new ThreadPoolExecutor(
            CORE_SIZE * 2, CORE_SIZE * 2, 0L, TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<>(500),
            new BasicThreadFactory.Builder().namingPattern("Thread-CourceDownloader-%s").build(),
            new ThreadPoolExecutor.CallerRunsPolicy());

    static final String WORK_DIR = System.getProperty("workdir", System.getProperty("user.dir") + "/m3u8_workdir/");
    static final String FFMPEG_PATH = System.getProperty("ffmpeg", "ffmpeg");
    static final File TMP_DIR = new File(WORK_DIR, "tmp/");
    static final File PART_DIR = new File(WORK_DIR, "part/");
    static final File M3U8_DIR = new File(WORK_DIR, "m3u8/");
    static final File OUTPUT_DIR = new File(WORK_DIR, "output/");
    static final FFmpegExecutor FFMPEG_EXECUTOR;

    static {
        Runtime.getRuntime().addShutdownHook(new Thread(EXECUTOR_SERVICE::shutdown));
        try {
            FFMPEG_EXECUTOR = new FFmpegExecutor(new FFmpeg(FFMPEG_PATH));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) throws Exception {
        String m3u8Url = "https://d01zu8.cdnlab.live/hls/WjGXNkhGXIl2edvZMroAnw/1619255751/10000/10991/10991.m3u8";//args[0];
        String key = "aDvNBXTlGnaHElQPuCF4dA==";//args[1];
        String iv = "3d9c3eb0ac489fce7421e36df997e3cd";
        String name = "2";//args[2];

        Map<String, String> headers = new HashMap<>();
        headers.put("user-agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/88.0.4324.182 Safari/537.36");

        downloadM3u8(m3u8Url, key, iv, name, headers);
    }

    public static void submitDownloadM3u8(String m3u8Url, String key, String iv, String name, Map<String, String> headers) {
        log.info("submit downloadM3u8:name[{}]-url[{}]-key[{}]-iv[{}]-headers{},", name, m3u8Url, key, iv, headers);
        SUBMIT_TASK_EXECUTOR_SERVICE.execute(() -> {
            try {
                downloadM3u8(m3u8Url, key, iv, name, headers);
            } catch (IOException e) {
                log.error("downloadM3u8:{}-{} error.", name, m3u8Url, e);
            }
        });
    }

    public static void downloadM3u8(String m3u8Url, String key, String iv, String name, Map<String, String> headers)
            throws IOException {
        final Map<String, String> fHeaders = Maps.newHashMapWithExpectedSize(headers == null ? 1 : 1 + headers.size());
        if (headers != null) {
            fHeaders.putAll(headers);
        }
        fHeaders.putIfAbsent("user-agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/88.0.4324.182 Safari/537.36");

        final String urlPrefix = getUrlPrefix(m3u8Url);
        String m3u8Md5 = DigestUtils.md5Hex(m3u8Url);
        String m3u8FileName = m3u8Md5 + ".m3u8";
        String fullFileName = name + ".mp4";
        File tmpDir = new File(TMP_DIR, m3u8Md5);
        File partDir = new File(PART_DIR, m3u8Md5);
        File m3u8File = new File(M3U8_DIR, m3u8FileName);
        File fullFile = new File(OUTPUT_DIR, fullFileName);
        if (fullFile.exists()) {
            log.info("file {} is ok", fullFile.getAbsolutePath());
            return;
        }

        if (!m3u8File.exists()) {
            HttpResult httpResult = HttpClientUtil.get(m3u8Url, fHeaders);
            if (!httpResult.isSuccess()) {
                log.error("name {} http get m3u8 url:{} failed.", name, m3u8Url);
                return;
            }
            FileUtils.writeByteArrayToFile(m3u8File, httpResult.getContent());
            log.info("name {} m3u8 file download ok.", name);
        }
        log.info("name {} m3u8 file: {}", name, m3u8File.getAbsolutePath());
        List<String> lines = FileUtils.readLines(m3u8File, "UTF-8");
        List<M3u8Part> m3u8Parts = new Aes128M3u8PartReader(key, lines).toList();
        log.info("name {} total part: {}", name, m3u8Parts.size());
        List<Future<Boolean>> futures = new ArrayList<>(m3u8Parts.size());
        for (final M3u8Part m3u8Part : m3u8Parts) {
            String url = m3u8Part.getUrl();
            String urlMd5 = DigestUtils.md5Hex(url);
            final File partFile = new File(partDir, m3u8Part.getNum() + "." + urlMd5 + ".ts");
            if (partFile.exists()) {
                log.info("name {} part {}/{} exist", name, m3u8Part.getNum(), m3u8Parts.size());
                continue;
            }
            log.info("name {} part {}/{} start", name, m3u8Part.getNum(), m3u8Parts.size());
            final File tmpPartFile = new File(tmpDir, urlMd5 + ".ts");
            futures.add(EXECUTOR_SERVICE.submit((SafeCallable<Boolean>) () -> {
                final byte[] tmpPartBytes;
                if (!tmpPartFile.exists()) {
                    String tmpUrl = urlPrefix + m3u8Part.getUrl();
                    HttpResult httpResult = HttpClientUtil.get(tmpUrl, fHeaders, 15);
                    if (!httpResult.isSuccess()) {
                        log.error("name {} http get part{}/{}:{} failed.", name, m3u8Part.getNum(), m3u8Parts.size(), tmpUrl);
                        return false;
                    }
                    tmpPartBytes = httpResult.getContent();
                    FileUtils.writeByteArrayToFile(tmpPartFile, tmpPartBytes);
                } else {
                    tmpPartBytes = FileUtils.readFileToByteArray(tmpPartFile);
                }

                FileUtils.writeByteArrayToFile(partFile, decrypt(m3u8Part.getKeyInfo(), tmpPartBytes));
                log.info("name {} part {}/{} complete", name, m3u8Part.getNum(), m3u8Parts.size());
                return true;
            }));
        }
        long failedCount = futures.stream().map(M3u8DownloadUtil::safeGetFuture).filter(Boolean.FALSE::equals).count();
        if (failedCount > 0) {
            log.error("name {} m3u8 download not completely. failed: {}", name, failedCount);
            return;
        }
        File[] partFiles = partDir.listFiles();
        if (partFiles == null) {
            throw new RuntimeException("partFiles is null.");
        }
        if (partFiles.length != m3u8Parts.size()) {
            log.error("name {} m3u8 download not completely. expect {} but {}", name, m3u8Parts.size(), partFiles.length);
            return;
        }
        List<String> fileNames = Stream.of(partFiles)
                .filter(item -> item.getName().endsWith(".ts"))
                .sorted(M3u8DownloadUtil::sortFile)
                .map(File::getAbsolutePath)
                .map(p -> "file " + "'" + p + "'")
                .collect(Collectors.toList());

        File fileManifest = new File(tmpDir, "file.manifest");
        FileUtils.writeLines(fileManifest, "UTF-8", fileNames);
        if (fullFile.getParentFile() != null && !fullFile.exists()) {
            fullFile.getParentFile().mkdirs();
        }
        FFmpegBuilder builder = new FFmpegBuilder()
                .addExtraArgs("-safe", "0")
                .setFormat("concat")
                .setInput(fileManifest.getAbsolutePath())
                .addOutput(fullFile.getAbsolutePath())
                .setAudioCodec("copy")
                .setVideoCodec("copy")
                .done();


        FFMPEG_EXECUTOR.createJob(builder).run();
        log.info("name {} m3u8 download complete at {}", name, fullFile.getAbsolutePath());
        FileUtils.deleteDirectory(tmpDir);
//        FileUtils.deleteDirectory(partDir);
        FileUtils.deleteQuietly(m3u8File);
    }

    private static String getUrlPrefix(String url) {
        if (url == null) {
            return null;
        }
        int idx = url.indexOf("?");
        if (idx > 0) {
            url = url.substring(0, idx);
        }
        idx = url.lastIndexOf("/");
        if (idx > -1) {
            return url.substring(0, idx + 1);
        }
        return url;
    }

    private static byte[] getAesKey(String keyUrl, String cookies, String token) throws IOException {
        Map<String, String> headers = Maps.newHashMapWithExpectedSize(3);
        headers.put("User-Agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/88.0.4324.182 Safari/537.36");
        headers.put("Referer", "https://ke.qq.com/webcourse/287404/102880600");
        headers.put("Cookie", cookies);
        keyUrl = keyUrl + "&token=" + token;
        HttpResult httpResult = HttpClientUtil.get(keyUrl, headers);
        return httpResult.getContent();
    }

    /**
     * int bkn = getBkn(cookies.get("p_lskey"));
     */
    private static int getBkn(String skey) {
        if (skey == null) {
            return 0;
        }
        int t = 5381;
        for (int i = 0; i < skey.length(); i++) {
            t += (t << 5) + skey.charAt(i);
        }
        return 2147483647 & t;
    }

    private static <T> T safeGetFuture(Future<T> future) {
        try {
            return future.get();
        } catch (Exception e) {
            log.error("safeGetFuture error.", e);
        }
        return null;
    }

    private static int sortFile(File a, File b) {
        int idxB = b.getName().indexOf(".");
        if (idxB < 1) {
            return 1;
        }
        int idxA = a.getName().indexOf(".");
        if (idxA < 1) {
            return -1;
        }
        int numA = Integer.parseInt(a.getName().substring(0, idxA));
        int numB = Integer.parseInt(b.getName().substring(0, idxB));
        if (numA < numB) {
            return -1;
        } else if (numA == numB) {
            return 0;
        }
        return 1;
    }

}
