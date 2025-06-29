package cn.gbtmedia.common.util;

/**
 * @author xqs
 */
public class AudioUtil {

    /**
     * adpcmToPcm
     */
    public static byte[] adpcmToPcm(byte[] adpcm){
        // 步长调整表
        int[] indexTable = {
                -1, -1, -1, -1, 2, 4, 6, 8,
                -1, -1, -1, -1, 2, 4, 6, 8
        };

        // 步长表
        int[] stepsizeTable = {
                7, 8, 9, 10, 11, 12, 13, 14, 16, 17,
                19, 21, 23, 25, 28, 31, 34, 37, 41, 45,
                50, 55, 60, 66, 73, 80, 88, 97, 107, 118,
                130, 143, 157, 173, 190, 209, 230, 253, 279, 307,
                337, 371, 408, 449, 494, 544, 598, 658, 724, 796,
                876, 963, 1060, 1166, 1282, 1411, 1552, 1707, 1878, 2066,
                2272, 2499, 2749, 3024, 3327, 3660, 4026, 4428, 4871, 5358,
                5894, 6484, 7132, 7845, 8630, 9493, 10442, 11487, 12635, 13899,
                15289, 16818, 18500, 20350, 22385, 24623, 27086, 29794, 32767
        };

        // 定义变量
        short statevalprev;
        byte stateindex;

        // 判断数据是否包含“海思头”
        int dlen = adpcm.length / 2;
        byte[] temp;
        if (adpcm[0] == 0x00 && adpcm[1] == 0x01 && (adpcm[2] & 0xff) == (adpcm.length - 4) / 2 && adpcm[3] == 0x00) {
            // 如果前四字节是海思头，跳过头部
            dlen = (adpcm.length - 8);
            temp = new byte[adpcm.length - 8];
            System.arraycopy(adpcm, 8, temp, 0, temp.length);

            // 初始化预测值和步长索引
            statevalprev = (short)(((adpcm[5] << 8) & 0xff00) | (adpcm[4] & 0xff));
            stateindex = adpcm[6];
        } else {
            // 普通ADPCM编码数据
            dlen = adpcm.length - 4;
            temp = new byte[adpcm.length - 4];
            System.arraycopy(adpcm, 4, temp, 0, temp.length);

            // 初始化预测值和步长索引
            statevalprev = (short)(((adpcm[1] << 8) & 0xff00) | (adpcm[0] & 0xff));
            stateindex = adpcm[2];
        }

        // 初始化输出数据数组
        short[] outdata = new short[dlen * 2];
        int len = dlen * 2;
        byte[] indata = temp;

        // 解码过程中的临时变量
        int sign; // 当前符号位
        int delta; // 当前ADPCM值
        int step; // 当前步长
        int valpred; // 当前预测值
        int vpdiff; // 当前预测值的变化量
        int index; // 步长索引
        int inputbuffer = 0; // 暂存4位输入值
        int bufferstep; // 输入缓冲区标志位

        // 初始化预测值和步长索引
        valpred = statevalprev;
        index = stateindex;
        if (index < 0) index = 0;
        if (index > 88) index = 88;
        step = stepsizeTable[index];
        bufferstep = 0;

        int k = 0;
        // 解码循环
        for (int i = 0; len > 0; len--) {

            // 步骤1 - 获取当前delta值
            if (bufferstep != 0) {
                delta = inputbuffer & 0xf;
            } else {
                inputbuffer = indata[i++];
                delta = (inputbuffer >> 4) & 0xf;
            }
            bufferstep = bufferstep == 0 ? 1 : 0;

            // 步骤2 - 更新步长索引
            index += indexTable[delta];
            if (index < 0) index = 0;
            if (index > 88) index = 88;

            // 步骤3 - 分离符号位和幅度
            sign = delta & 8;
            delta = delta & 7;

            // 步骤4 - 计算预测值变化量
            vpdiff = step >> 3;
            if ((delta & 4) > 0) vpdiff += step;
            if ((delta & 2) > 0) vpdiff += step >> 1;
            if ((delta & 1) > 0) vpdiff += step >> 2;

            if (sign != 0) {
                valpred -= vpdiff;
            } else {
                valpred += vpdiff;
            }

            // 步骤5 - 限制输出值范围
            if (valpred > 32767) valpred = 32767;
            else if (valpred < -32768) valpred = -32768;

            // 步骤6 - 更新步长
            step = stepsizeTable[index];

            // 步骤7 - 输出解码值
            outdata[k++] = (short)valpred;
        }

        // 将解码后的short数组转换为字节数组
        temp = new byte[dlen * 4];
        for (int i = 0, kj = 0; i < outdata.length; i++) {
            short s = outdata[i];
            temp[kj++] = (byte)(s & 0xff);
            temp[kj++] = (byte)((s >> 8) & 0xff);
        }
        return temp;
    }

    /**
     *
     * g711aToPcm
     */
    public static byte[] g711aToPcm(byte[] g711a){
        byte[] g711data;
        // 如果前四字节是00 01 52 00，则是海思头，需要去掉
        if (g711a[0] == 0x00 && g711a[1] == 0x01 && (g711a[2] & 0xff) == (g711a.length - 4) / 2 && g711a[3] == 0x00) {
            g711data = new byte[g711a.length - 4];
            System.arraycopy(g711a, 4, g711data, 0, g711data.length);
        } else {
            g711data = g711a;
        }
        byte[] pcmdata = new byte[g711data.length * 2];
        for (int i = 0, k = 0; i < g711data.length; i++) {
            byte a_val = g711data[i];
            short t;
            short seg;
            a_val ^= 0x55;
            t = (short) ((a_val & 0xf) << 4);
            seg = (short) ((a_val & 0x70) >> 4);
            switch (seg) {
                case 0:
                    t += 8;
                    break;
                case 1:
                    t += 0x108;
                    break;
                default:
                    t += 0x108;
                    t <<= seg - 1;
            }
            short v =  (a_val & 0x80) != 0 ? t : (short) -t;
            pcmdata[k++] = (byte) (v & 0xff);
            pcmdata[k++] = (byte) ((v >> 8) & 0xff);
        }
        return pcmdata;
    }

    /**
     * pcmToG711a
     */
    public static byte[] pcmToG711a(byte[] pcm){
       short cClip = 32635;
       byte[] aLawCompressTable = new byte[]{1, 1, 2, 2, 3, 3, 3,
                3, 4, 4, 4, 4, 4, 4, 4, 4, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5,
                5, 5, 5, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6,
                6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 7, 7, 7, 7, 7, 7, 7, 7, 7,
                7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7,
                7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7,
                7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7};

        int j = 0;
        int len = pcm.length;
        int count = len / 2;
        byte[] res = new byte[count];
        short sample = 0;
        for (int i = 0; i < count; i++) {
            sample = (short) (((pcm[j++] & 0xff) | (pcm[j++]) << 8));
            int sign;
            int exponent;
            int mantissa;
            int s;

            sign = ((~sample) >> 8) & 0x80;
            if (!(sign == 0x80)) {
                sample = (short) -sample;
            }
            if (sample > cClip) {
                sample = cClip;
            }
            if (sample >= 256) {
                exponent = aLawCompressTable[(sample >> 8) & 0x7F];
                mantissa = (sample >> (exponent + 3)) & 0x0F;
                s = (exponent << 4) | mantissa;
            } else {
                s = sample >> 4;
            }
            s ^= (sign ^ 0x55);
            res[i] = (byte) s;
        }
        return res;
    }

    public static byte[] pcmToPcma(byte[] pcm){
        // 等同
        return pcmToG711a(pcm);
    }

    public static void main(String[] args) {

    }
}
