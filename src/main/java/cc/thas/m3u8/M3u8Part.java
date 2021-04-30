package cc.thas.m3u8;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class M3u8Part {

    private int num;
    private String url;
    private M3u8PartKeyInfo keyInfo;
}
