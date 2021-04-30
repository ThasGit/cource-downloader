package cc.thas.m3u8;

import lombok.Data;

@Data
public class M3u8PartKeyInfo {
    private String keyMethod;
    private String keyUrl;
    private String ivHex;
    private String keyBase64;
}
