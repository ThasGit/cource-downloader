package cc.thas.http;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;

import static cc.thas.constant.ErrorCode.INTERNAL_ERROR;
import static cc.thas.http.HttpUtil.headers2Map;
import static cc.thas.http.HttpUtil.map2Headers;

@Slf4j
public class HttpClientUtil {

    public static final int DEFAULT_TIME_OUT = 120000;
    public static final CloseableHttpClient HTTP_CLIENT;

    static {
        RequestConfig requestConfig = RequestConfig.custom()
                .setRedirectsEnabled(false)
                .setConnectionRequestTimeout(DEFAULT_TIME_OUT)
                .setConnectTimeout(DEFAULT_TIME_OUT)
                .setSocketTimeout(DEFAULT_TIME_OUT)
                .build();

        HTTP_CLIENT = HttpClients.custom()
                .setDefaultRequestConfig(requestConfig)
                .disableAutomaticRetries()
                .build();
    }

    public static CloseableHttpResponse doExecute(HttpUriRequest request) throws IOException {
        return HTTP_CLIENT.execute(request);
    }

    public static HttpResult execute(HttpUriRequest request) throws IOException {
        try (CloseableHttpResponse response = doExecute(request)) {
            return response2httpResult(response);
        }
    }

    public static HttpResult head(String url) {
        try {
            return execute(new HttpHead(url));
        } catch (IOException e) {
            log.error("HttpClient head error.", e);
            return new HttpResult(INTERNAL_ERROR.getMessage());
        }
    }

    public static HttpResult get(String url, Map<String, String> headers, int retryCount) {
        HttpGet request = new HttpGet(url);
        request.setHeaders(map2Headers(headers));
        try {
            return execute(request);
        } catch (IOException e) {
            log.error("HttpClient get error.", e);
            while (retryCount-- > 0) {
                log.error("HttpClient get error. retry left:{}", retryCount, e);
                return get(url, headers, retryCount);
            }
            return new HttpResult(INTERNAL_ERROR.getMessage());
        }
    }

    public static HttpResult get(String url, Map<String, String> headers) {
        return get(url, headers, 0);
    }

    public static HttpResult get(String url, int retryCount) {
        return get(url, Collections.emptyMap(), retryCount);
    }

    public static HttpResult get(String url) {
        return get(url, 0);
    }

    private static HttpResult response2httpResult(CloseableHttpResponse response) throws IOException {
        int httpStatusCode = response.getStatusLine() != null ? response.getStatusLine().getStatusCode() : 0;
        byte[] content = response.getEntity() != null && response.getEntity().getContent() != null
                ? IOUtils.toByteArray(response.getEntity().getContent()) : null;
        Map<String, String> headers = headers2Map(response.getAllHeaders());
        return new HttpResult(httpStatusCode, headers, content);
    }


}
