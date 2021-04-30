package cc.thas.http;

import com.google.common.collect.Maps;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.Header;
import org.apache.http.message.BasicHeader;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class HttpUtil {

    public static Map<String, String> headers2Map(Header[] headers) {
        if (headers == null || headers.length == 0) {
            return Collections.emptyMap();
        }
        return Stream.of(headers).collect(Collectors.toMap(Header::getName, Header::getValue, (v1, v2) -> v2));
    }

    public static Header[] map2Headers(Map<String, String> map) {
        if (map == null) {
            return new Header[0];
        }
        return map.entrySet().stream().map(item -> new BasicHeader(item.getKey(), item.getValue()))
                .collect(Collectors.toList()).toArray(new Header[0]);
    }

    public static Map<String, String> convertCookie2Map(String cookies) throws IOException {

        return Stream.of(cookies.split(";"))
                .filter(StringUtils::isNotBlank)
                .map(item -> item.split("=", 2))
                .filter(item -> item.length == 2 && item[0] != null && item[1] != null)
                .collect(Collectors.toMap(item -> item[0].trim(), item -> item[1].trim(), (v1, v2) -> v2));
    }

    public static Map<String, String> headers2Map(String headers) {
         if (headers == null || headers.isEmpty()) {
            return Collections.emptyMap();
        }
        String[] lines = headers.split("\n");
        Map<String, String> headerMap = Maps.newHashMapWithExpectedSize(lines.length);
        for (String line : lines) {
            String[] kv = line.split(":", 2);
            headerMap.put(kv[0], kv[1]);
        }
        return headerMap;
    }
}
