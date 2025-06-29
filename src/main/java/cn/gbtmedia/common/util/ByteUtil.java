package cn.gbtmedia.common.util;

import cn.hutool.core.codec.BCD;
import cn.hutool.core.util.NumberUtil;
import io.netty.buffer.ByteBufUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author xqs
 */
public class ByteUtil {

    /**
     * BCD数组转字符串
     */
    public static String BCDToStr(byte[] array) {
        StringBuilder str = new StringBuilder();
        for (byte b : array) {
            int highDigit = (b >> 4) & 0x0F;
            int lowDigit = b & 0x0F;
            str.append(highDigit).append(lowDigit);
        }
        return str.toString();
    }

    /**
     * 字符串转BCD数组
     */
    public static byte[] strToBCD(String str) {
        // BCD.strToBcd(str);
        byte[] bcd = new byte[str.length() / 2];
        for (int i = 0, k = 0, l = str.length(); i < l; i+=2)
        {
            char a = (char)(str.charAt(i) - '0');
            char b = (char)(str.charAt(i + 1) - '0');
            bcd[k++] = ((byte)(a << 4 | b));
        }
        return bcd;
    }

    /**
     *长度不足前面补0, 超出抛出异常
     */
    public static byte[] padCheckBytes(byte[] bytes, int targetLength) {
        if (bytes.length == targetLength) {
            return bytes;
        }
        if(bytes.length > targetLength){
            throw new RuntimeException("padCheckBytes max " + bytes.length + " "+ targetLength);
        }
        byte[] padded = new byte[targetLength];
        System.arraycopy(bytes, 0, padded, targetLength - bytes.length, bytes.length);
        return padded;
    }

    /**
     * 合并byte数组集合
     */
    public static byte[] mergeByte(List<byte[]> byteList) {
        int totalLength = 0;
        for (byte[] arr : byteList) {
            totalLength += arr.length;
        }
        byte[] mergedArray = new byte[totalLength];
        int position = 0;
        for (byte[] arr : byteList) {
            System.arraycopy(arr, 0, mergedArray, position, arr.length);
            position += arr.length;
        }
        return mergedArray;
    }

    /**
     *
     * byte2ToInt
     */
    public static int byte2ToInt(byte b1,byte b2){
        int temp1 = b1&0xff ;
        int temp2 = b2&0xff ;
        return (temp1<< 8) + temp2;
    }

    public static int byte4ToInt(byte b1,byte b2,byte b3,byte b4){
        int temp1 = b1&0xff ;
        int temp2 = b2&0xff ;
        int temp3 = b3&0xff ;
        int temp4 = b4&0xff ;
        return (temp1 << 24) + (temp2<< 16)+(temp3<< 8)+temp4;
    }

    public static boolean getBit(int val, int pos) {
        return getBit(new byte[] {
                (byte)((val >> 0) & 0xff),
                (byte)((val >> 8) & 0xff),
                (byte)((val >> 16) & 0xff),
                (byte)((val >> 24) & 0xff)
        }, pos);
    }

    public static int reverse(int val) {
        byte[] bytes = toBytes(val);
        byte[] ret = new byte[4];
        for (int i = 0; i < 4; i++) ret[i] = bytes[3 - i];
        return toInt(ret);
    }

    public static byte[] toLEBytes(int val) {
        byte[] bytes = new byte[4];
        for (int i = 0; i < 4; i++)
        {
            bytes[3 - i] = (byte)(val >> ((3 - i) * 8) & 0xff);
        }
        return bytes;
    }

    public static byte[] toLEBytes(short s) {
        byte[] bytes = new byte[2];
        bytes[0] = (byte)(s & 0xff);
        bytes[1] = (byte)((s >> 8) & 0xff);
        return bytes;
    }

    public static int toInt(byte[] bytes) {
        int val = 0;
        for (int i = 0; i < 4; i++) val |= (bytes[i] & 0xff) << ((3 - i) * 8);
        return val;
    }

    public static byte[] toBytes(int val) {
        byte[] bytes = new byte[4];
        for (int i = 0; i < 4; i++)
        {
            bytes[i] = (byte)(val >> ((3 - i) * 8) & 0xff);
        }
        return bytes;
    }

    public static byte[] toBytes(long val) {
        byte[] bytes = new byte[8];
        for (int i = 0; i < 8; i++)
        {
            bytes[i] = (byte)(val >> ((7 - i) * 8) & 0xff);
        }
        return bytes;
    }

    public static int getInt(byte[] data, int offset, int length) {
        int val = 0;
        for (int i = 0; i < length; i++) val |= (data[offset + i] & 0xff) << ((length - i - 1) * 8);
        return val;
    }

    public static long getLong(byte[] data, int offset, int length) {
        long val = 0;
        for (int i = 0; i < length; i++) val |= ((long)data[offset + i] & 0xff) << ((length - i - 1) * 8);
        return val;
    }

    public static boolean getBit(byte[] data, int pos) {
        return ((data[pos / 8] >> (pos % 8)) & 0x01) == 0x01;
    }

    public static byte[] concat(byte[]...byteArrays) {
        int len = 0, index = 0;
        for (int i = 0; i < byteArrays.length; i++) len += byteArrays[i].length;
        byte[] buff = new byte[len];
        for (int i = 0; i < byteArrays.length; i++)
        {
            System.arraycopy(byteArrays[i], 0, buff, index, byteArrays[i].length);
            index += byteArrays[i].length;
        }
        return buff;
    }

    public static boolean compare(byte[] data1, byte[] data2) {
        if (data1.length != data2.length) return false;
        for (int i = 0; i < data1.length; i++)
            if ((data1[i] & 0xff) != (data2[i] & 0xff)) return false;
        return true;
    }

    public static short[] toShortArray(byte[] src) {
        short[] dst = new short[src.length / 2];
        for (int i = 0, k = 0; i < src.length; )
        {
            dst[k++] = (short)((src[i++] & 0xff) | ((src[i++] & 0xff) << 8));
        }
        return dst;
    }

    public static byte[] toByteArray(short[] src) {
        byte[] dst = new byte[src.length * 2];
        for (int i = 0, k = 0; i < src.length; i++)
        {
            dst[k++] = (byte)(src[i] & 0xff);
            dst[k++] = (byte)((src[i] >> 8) & 0xff);
        }
        return dst;
    }

    public static void main(String[] args) {

    }

}
