package cn.gbtmedia.test;

import java.nio.ByteBuffer;

public class H264Test {

    // 构造SPS（假设分辨率为1920x1080，High Profile Level 4.1）
    public static byte[] buildSPS(int width, int height, int profileIdc, int levelIdc) {
        ByteBuffer sps = ByteBuffer.allocate(256);
        // SPS起始码（通常为0x00 0x00 0x00 0x01，但在FLV中不需要）
        sps.put((byte) 0x67); // NAL头：0x67表示SPS（类型7，重要性最高）

        // --- SPS内容 ---
        // 基础参数
        sps.put((byte) profileIdc);   // Profile IDC (e.g., 100 = High Profile)
        sps.put((byte) 0x00);        // Compatibility flags
        sps.put((byte) levelIdc);    // Level IDC (e.g., 41 = Level 4.1)

        // seq_parameter_set_id (Exp-Golomb编码，通常为0)
        putExpGolomb(sps, 0);

        // log2_max_frame_num_minus4 (假设4，即max_frame_num=16)
        putExpGolomb(sps, 4);

        // pic_order_cnt_type (假设0)
        putExpGolomb(sps, 0);

        // log2_max_pic_order_cnt_lsb_minus4 (假设4)
        putExpGolomb(sps, 4);

        // num_ref_frames (假设1)
        putExpGolomb(sps, 1);

        // gaps_in_frame_num_value_allowed_flag (0)
        sps.put((byte) 0x80); // 二进制10000000，高位开始：1（gap不允许） + 保留位

        // 分辨率计算（以宏块为单位）
        int widthInMB = (width + 15) / 16 - 1;
        int heightInMB = (height + 15) / 16 - 1;
        putExpGolomb(sps, widthInMB);  // pic_width_in_mbs_minus1
        putExpGolomb(sps, heightInMB); // pic_height_in_map_units_minus1

        // frame_mbs_only_flag (1表示仅逐行扫描)
        sps.put((byte) 0x80); // 二进制10000000，高位开始：1（仅逐行） + 保留位

        // direct_8x8_inference_flag (1)
        sps.put((byte) 0x80); // 二进制10000000，高位开始：1（启用） + 保留位

        // 填充剩余参数（简化处理）
        sps.put((byte) 0x00); // frame_cropping_flag (0)
        sps.put((byte) 0x00); // vui_parameters_present_flag (0)

        // 转换为字节数组
        byte[] result = new byte[sps.position()];
        System.arraycopy(sps.array(), 0, result, 0, result.length);
        return result;
    }

    // 构造PPS
    public static byte[] buildPPS() {
        ByteBuffer pps = ByteBuffer.allocate(16);
        pps.put((byte) 0x68); // NAL头：0x68表示PPS（类型8）

        // --- PPS内容 ---
        // pic_parameter_set_id (Exp-Golomb编码，0)
        putExpGolomb(pps, 0);

        // seq_parameter_set_id (Exp-Golomb编码，0)
        putExpGolomb(pps, 0);

        // entropy_coding_mode_flag (0表示CAVLC，1表示CABAC)
        pps.put((byte) 0x80); // 二进制10000000，高位开始：1（CABAC） + 保留位

        // pic_order_present_flag (0)
        pps.put((byte) 0x00);

        // num_slice_groups_minus1 (0)
        putExpGolomb(pps, 0);

        // 转换为字节数组
        byte[] result = new byte[pps.position()];
        System.arraycopy(pps.array(), 0, result, 0, result.length);
        return result;
    }

    // 写入指数哥伦布编码（无符号整数）
    private static void putExpGolomb(ByteBuffer buffer, int value) {
        int size = 1;
        value++; // Exp-Golomb编码需要+1
        int mask = 0xFFFF;
        while ((value & mask) != 0) {
            mask <<= 1;
            size++;
        }
        for (int i = 0; i < size; i++) {
            buffer.put((byte) 0x80); // 假设全部为1（简化实现，实际需按位填充）
        }
        buffer.put((byte) 0x00); // 分隔符
    }

    public static void main(String[] args) {
        // 示例：构造1920x1080、High Profile Level 4.1的SPS和PPS
        byte[] sps = buildSPS(1920, 1080, 100, 41); // 100=High Profile, 41=Level4.1
        byte[] pps = buildPPS();

        System.out.println("SPS (Hex): " + bytesToHex(sps));
        System.out.println("PPS (Hex): " + bytesToHex(pps));
    }

    // 辅助方法：字节数组转Hex字符串
    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X ", b));
        }
        return sb.toString();
    }
}
