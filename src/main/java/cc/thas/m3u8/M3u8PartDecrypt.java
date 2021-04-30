package cc.thas.m3u8;

public interface M3u8PartDecrypt {

    byte[] decrypt(byte[] raw) throws Exception;

    DefaultM3u8PartDecrypt DEFAULT_DECRYPT = new DefaultM3u8PartDecrypt();

    class DefaultM3u8PartDecrypt implements M3u8PartDecrypt {
        @Override
        public byte[] decrypt(byte[] raw) throws Exception {
            return raw;
        }
    }
}
