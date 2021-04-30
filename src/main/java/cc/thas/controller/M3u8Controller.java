package cc.thas.controller;

import cc.thas.http.HttpUtil;
import cc.thas.m3u8.M3u8DownloadUtil;
import org.apache.commons.text.StringEscapeUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Map;

@Controller
@CrossOrigin
public class M3u8Controller {

    @RequestMapping("/")
    public String index() {
        return "index";
    }

    @ResponseBody
    @RequestMapping("/m3u8/submit")
    public String submitM3u8Download(@RequestParam(name = "url") String url, @RequestParam(name = "key") String key,
                                     @RequestParam(name = "iv", required = false) String iv,
                                     @RequestParam(name = "name") String name,
                                     @RequestParam(name = "headers", required = false) String headers) {
        url = StringEscapeUtils.unescapeHtml4(url);
        key = StringEscapeUtils.unescapeHtml4(key);
        iv = StringEscapeUtils.unescapeHtml4(iv);
        name = StringEscapeUtils.unescapeHtml4(name);
        headers = StringEscapeUtils.unescapeHtml4(headers);
        Map<String, String> headerMap = HttpUtil.headers2Map(headers);
        M3u8DownloadUtil.submitDownloadM3u8(url, key, iv, name, headerMap);
        return "success";
    }
}
