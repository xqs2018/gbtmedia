package cn.gbtmedia.gbt28181.server.flv.transcode;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.ffmpeg.global.avutil;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.FFmpegFrameRecorder;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;
import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * @author xqs
 */
@Slf4j
@Data
@EqualsAndHashCode(callSuper = true)
public class TranscodeTaskJavaCv extends TranscodeTask{

    private FFmpegFrameGrabber grabber;

    private FFmpegFrameRecorder recorder;

    private Java2DFrameConverter converter = new Java2DFrameConverter();

    @Override
    protected synchronized void doStop(){
        if(grabber != null){
            try {
                grabber.stop();
                grabber.close();
            } catch (Exception e) {
                log.error("stop grabber ex",e);
            }
        }
        if(recorder != null){
            try {
                recorder.stop();
                recorder.close();
            } catch (Exception e) {
                log.error("stop recorder ex",e);
            }
        }
        if(converter != null){
            try {
                converter.close();
            } catch (Exception e) {
                log.error("close converter ex",e);
            }
        }
    }

    @Override
    protected void doStart() throws Exception{
        log.info("start transcodeJavaCv pullFlvUrl {} transcode {}",pullFlvUrl,transcode);

        // 启动拉流器
        grabber = new FFmpegFrameGrabber(pullFlvUrl);
        grabber.start();

        // 分辨率设置
        imageWidth = imageWidth == 0 ? grabber.getImageWidth() : imageWidth;
        imageHeight = imageHeight == 0 ? grabber.getImageHeight() : imageHeight;
        int audioChannels = grabber.getAudioChannels();

        // 启动录制器
        recorder = new FFmpegFrameRecorder(flvData, imageWidth, imageHeight, audioChannels);
        recorder.setFormat("flv");
        recorder.setFrameRate(25);
        recorder.setGopSize(25);
        recorder.setVideoCodec(avcodec.AV_CODEC_ID_H264);
        recorder.setPixelFormat(avutil.AV_PIX_FMT_YUV420P);
        recorder.setAudioCodec(avcodec.AV_CODEC_ID_AAC);
        recorder.start();

        // 提取flvHeader
        grabber.flush();
        flvHeader = flvData.toByteArray();
        flvData.reset();

        // 开始录制流帧
        try {
            while (!isStop){
                recorderFrame();
            }
        }catch (Exception ex){
            log.error("recorderFrame ex",ex);
        }finally {
            stop();
        }
    }

    private synchronized void recorderFrame() throws Exception{
        Frame frame = grabber.grab();
        if (frame != null) {
            if (frame.image != null) {
                BufferedImage bufferedImage = converter.getBufferedImage(frame);
                Graphics2D graphics = bufferedImage.createGraphics();
                graphics.setFont(new Font("Default", Font.PLAIN, 24));
                graphics.setColor(Color.white);
                String watermarkText = drawText;
                // 计算水印文字的位置 右下角
                FontMetrics fontMetrics = graphics.getFontMetrics();
                int textWidth = fontMetrics.stringWidth(watermarkText);
                int textHeight = fontMetrics.getHeight();
                int x = bufferedImage.getWidth() - textWidth - 10;
                int y = bufferedImage.getHeight() - textHeight + fontMetrics.getAscent() - 10;
                graphics.drawString(watermarkText, x, y);
                graphics.dispose();
                frame = converter.convert(bufferedImage);
            }
            recorder.record(frame);
        }
        if (flvData.size() > 0) {
            byte[] data = flvData.toByteArray();
            flvData.reset();
            receiveFlvData(data);
        }
    }
}
