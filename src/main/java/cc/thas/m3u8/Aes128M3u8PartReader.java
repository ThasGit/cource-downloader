package cc.thas.m3u8;

import lombok.extern.slf4j.Slf4j;

import java.util.List;

import static cc.thas.m3u8.M3u8DecryptUtil.commonResolveKey;

@Slf4j
public class Aes128M3u8PartReader extends CommonM3u8PartReader {
    private final String keyBase64;
    private final String ivHex;

    public Aes128M3u8PartReader(String keyBase64, List<String> lines) {
        super(lines);
        this.keyBase64 = keyBase64;
        this.ivHex = null;
    }

    public Aes128M3u8PartReader(String keyBase64, String ivHex, List<String> lines) {
        super(lines);
        this.keyBase64 = keyBase64;
        this.ivHex = ivHex;
    }

    @Override
    public M3u8PartKeyInfo resolveKey(String line) {
        M3u8PartKeyInfo keyInfo = commonResolveKey(line);
        if (keyInfo == null) {
            keyInfo = new M3u8PartKeyInfo();
        }
        keyInfo.setKeyBase64(keyBase64);
        if (keyInfo.getIvHex() == null) {
            keyInfo.setIvHex(ivHex);
        }
        return keyInfo;
    }


}
