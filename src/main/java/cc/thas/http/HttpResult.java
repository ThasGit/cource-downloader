package cc.thas.http;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.annotation.JSONField;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.Serializable;
import java.util.Map;

@AllArgsConstructor
@Slf4j
@Data
public class HttpResult implements Serializable {

    private boolean success;
    private String message;
    private int httpStatusCode;
    private Map<String, String> headers;
    private byte[] content;

    public HttpResult(int httpStatusCode, Map<String, String> headers, byte[] content) {
        this.httpStatusCode = httpStatusCode;
        this.headers = headers;
        this.content = content;
        if (httpStatusCode == 200) {
            this.success = true;
            this.message = "success";
        } else {
            this.success = false;
            this.message = String.valueOf(httpStatusCode);
        }
    }

    public HttpResult(int httpStatusCode, String error) {
        this.success = false;
        this.httpStatusCode = httpStatusCode;
        this.message = error;
    }

    public HttpResult(String error) {
        this(0, error);
    }

    public HttpResult() {
        this.success = true;
        this.httpStatusCode = 200;
        this.message = "success";
    }

    public String getHeader(String name) {
        if (headers == null) {
            return null;
        }
        return headers.get(name);
    }

    public String getHeaderOrDefault(String name, String defaultValue) {
        if (headers == null) {
            return null;
        }
        return headers.getOrDefault(name, defaultValue);
    }

    public long getContentLength() {
        String contentLength = getHeader("Content-Length");
        if (contentLength != null) {
            try {
                return Long.parseLong(contentLength);
            } catch (NumberFormatException e) {
                log.error("ContentLength format error: " + contentLength, e);
            }
        }
        return 0L;
    }

    public String getContentDisposition() {
        return getHeader("Content-Disposition");
    }

    @JSONField(serialize = false, deserialize = false)
    public String getString(String charset) throws IOException {
        return new String(getContent(), charset);
    }

    @JSONField(serialize = false, deserialize = false)
    public String getObject(Class<?> clazz) throws IOException {
        return JSON.parseObject(getContent(), clazz);
    }
}
