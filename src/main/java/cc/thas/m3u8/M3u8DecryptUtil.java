package cc.thas.m3u8;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.AlgorithmParameters;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidParameterSpecException;
import java.util.Base64;

public class M3u8DecryptUtil {

    public static byte[] decrypt(M3u8PartKeyInfo keyInfo, byte[] raw) throws DecoderException, NoSuchPaddingException, InvalidAlgorithmParameterException, NoSuchAlgorithmException, IllegalBlockSizeException, BadPaddingException, InvalidKeyException, InvalidParameterSpecException {
        if (keyInfo == null || keyInfo.getKeyBase64() == null) {
            return raw;
        }
        if ("AES-128".equals(keyInfo.getKeyMethod())) {
            final byte[] keyBytes = Base64.getDecoder().decode(keyInfo.getKeyBase64());
            final byte[] ivBytes = keyInfo.getIvHex() != null ? Hex.decodeHex(keyInfo.getIvHex()) : null;
            return decryptAes128(raw, keyBytes, ivBytes);
        }
        return raw;
    }

    public static byte[] decryptAes128(byte[] raw, byte[] key, byte[] iv) throws NoSuchPaddingException, NoSuchAlgorithmException,
            BadPaddingException, IllegalBlockSizeException, InvalidKeyException,
            InvalidAlgorithmParameterException, DecoderException, InvalidParameterSpecException {
        SecretKeySpec spec = new SecretKeySpec(key, "AES");
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        if (iv == null) {
            cipher.init(Cipher.DECRYPT_MODE, spec);
        } else {
            cipher.init(Cipher.DECRYPT_MODE, spec, new IvParameterSpec(iv));
        }

        return cipher.doFinal(raw);
    }

    public static AlgorithmParameters generateIv(byte[] iv) throws NoSuchAlgorithmException, InvalidParameterSpecException {
        AlgorithmParameters params = AlgorithmParameters.getInstance("AES");
        params.init(new IvParameterSpec(iv));
        return params;
    }

    public static M3u8PartKeyInfo commonResolveKey(String keyLine) {
        M3u8PartKeyInfo key = new M3u8PartKeyInfo();
        if (keyLine == null) {
            return key;
        }
        String[] split = keyLine.split(":", 2);
        if (split.length != 2 || split[1] == null) {
            return null;
        }

        split = split[1].split(",");
        for (String s : split) {
            if (s == null) {
                continue;
            }
            String[] kv = s.split("=", 2);
            if (kv.length != 2 || kv[0] == null || kv[1] == null) {
                continue;
            }
            String value = removeQuota(kv[1]);
            switch (kv[0]) {
                case "METHOD":
                    key.setKeyMethod(value);
                    break;
                case "URI":
                    key.setKeyUrl(value);
                    break;
                case "IV":
                    key.setIvHex(remove0x(value));
                    break;
                default:
                    break;
            }
        }
        return key;
    }

    public static String removeQuota(String str) {
        if (str == null || str.length() < 2) {
            return str;
        }
        if (str.startsWith("\"") && str.endsWith("\"")) {
            return str.substring(1, str.length() - 1);
        } else if (str.startsWith("\'") && str.endsWith("\'")) {
            return str.substring(1, str.length() - 1);
        }
        return str;
    }

    public static String remove0x(String s) {
        if (s == null || !s.startsWith("0x")) {
            return s;
        }
        return s.substring(2);
    }
}
