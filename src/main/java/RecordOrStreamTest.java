import org.bytedeco.javacpp.avcodec;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.FrameRecorder.Exception;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.TargetDataLine;
import java.io.IOException;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author baojikui (bjklwr@outlook.com)
 * @date 2019/01/12
 */
public class RecordOrStreamTest {

    final static String VIDEO_DEVICE = "1";
    final static String VIDEO_NAME = "/Users/baojikui/Desktop/output.mp4";
    //    final static String VIDEO_NAME = "/code/output.mp4";
    final static String RTMP_ADDR = "rtmp://pili-publish.daishuclass.cn/daishu-video/test_1234";
    final private static String X11GRAB = ":1.0+10,10";

    final static int FRAME_RATE = 30;
    final static int GOP_LENGTH_IN_FRAMES = 60;

    static long startTime = 0;
    static long videoTS = 0;

    public static void main(String[] args) throws Exception, IOException, org.bytedeco.javacv.FrameGrabber.Exception {
        int captureWidth = 1280;
        int captureHeight = 720;

        //        FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(X11GRAB);
        //        grabber.setFormat("x11grab");
        FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(VIDEO_DEVICE);
        grabber.setFormat("avfoundation");
        grabber.setImageWidth(captureWidth);
        grabber.setImageHeight(captureHeight);
        grabber.start();

        //        final FFmpegFrameRecorder recorder = new FFmpegFrameRecorder(VIDEO_NAME, captureWidth, captureHeight, 2);
        final MyFFmpegFrameRecorder recorder1 = new MyFFmpegFrameRecorder(RTMP_ADDR, captureWidth, captureHeight, 2);
        final MyFFmpegFrameRecorder recorder = new MyFFmpegFrameRecorder(VIDEO_NAME, captureWidth, captureHeight, 2);

        configRecorder(recorder);
        recorder.start();

        configRecorder(recorder1);
        recorder1.start();

        //获取麦克风音频
        MicoAudioThread micoAudioThread = new MicoAudioThread(recorder, recorder1);
        micoAudioThread.start();

        //视频
        Frame capturedFrame = null;
        while ((capturedFrame = grabber.grab()) != null) {

            if (startTime == 0) {
                startTime = System.currentTimeMillis();
            }
            videoTS = 1000 * (System.currentTimeMillis() - startTime);
//            if (videoTS > recorder.getTimestamp()) {
//                System.out.println("时间戳纠正: " + videoTS + " : " + recorder.getTimestamp() + " -> " + (videoTS - recorder.getTimestamp()));
//                recorder.setTimestamp(videoTS);
//                recorder1.setTimestamp(videoTS);
//            }

            avcodec.AVPacket videoPacket = null;
//            avcodec.AVPacket audioPacket = null;
            int pixelFormat = -1;
            try {
                if (capturedFrame.image != null) {
                    videoPacket = recorder
                                    .getVideoPacket(capturedFrame.imageWidth, capturedFrame.imageHeight, capturedFrame.imageDepth, capturedFrame.imageChannels,
                                                    capturedFrame.imageStride, pixelFormat, capturedFrame.image);
                    writePkt(recorder, recorder1, videoPacket, 0);
                } else {
                    videoPacket = recorder.getVideoPacket(0, 0, 0, 0, 0, pixelFormat, (Buffer[]) null);
                    writePkt(recorder, recorder1, videoPacket, 0);
                }

                //                if (capturedFrame.samples != null) {
                //
                //                    audioPacket = recorder.getAudioPacket(capturedFrame.sampleRate, capturedFrame.audioChannels, capturedFrame.samples);
                //                    writePkt(recorder, recorder1, audioPacket, 1);
                //                }

            } catch (Exception e) {
                e.printStackTrace();
            }

            if(System.currentTimeMillis() - startTime > 15000){
                break;
            }
        }

        micoAudioThread.interrupt();
        recorder.stop();
        recorder1.stopCopy();
        grabber.stop();

        System.out.println("close");
    }

    private static void configRecorder(MyFFmpegFrameRecorder recorder) {
        recorder.setInterleaved(true);

        //video 参数
        recorder.setVideoOption("tune", "zerolatency");
        recorder.setVideoOption("preset", "ultrafast");
        recorder.setVideoOption("crf", "28");
        recorder.setVideoBitrate(2000000);
        recorder.setVideoCodec(avcodec.AV_CODEC_ID_H264);
        recorder.setFormat("flv");
        recorder.setFrameRate(FRAME_RATE);
        recorder.setGopSize(GOP_LENGTH_IN_FRAMES);
        //audio 参数
        recorder.setAudioOption("crf", "0");
        recorder.setAudioQuality(0);
        recorder.setAudioBitrate(192000);
        recorder.setSampleRate(44100);
        recorder.setAudioChannels(2);
        recorder.setAudioCodec(avcodec.AV_CODEC_ID_AAC);
    }

    public static void writePkt(MyFFmpegFrameRecorder recorder, MyFFmpegFrameRecorder recorder1, avcodec.AVPacket packet, int mediaType) throws Exception {
        if (packet != null) {
            avcodec.AVPacket copy = avcodec.av_packet_clone(packet);
            recorder.writePacket(mediaType, packet);
            recorder1.writePacket(mediaType, copy);
        }
    }
}

class MicoAudioThread extends Thread {

    private MyFFmpegFrameRecorder recorder;
    private MyFFmpegFrameRecorder recorder1;

    public MicoAudioThread(MyFFmpegFrameRecorder recorder, MyFFmpegFrameRecorder recorder1) {
        this.recorder = recorder;
        this.recorder1 = recorder1;
    }

    @Override
    public void run() {
        AudioFormat audioFormat = new AudioFormat(44100.0F, 16, 2, true, false);
        DataLine.Info dataLineInfo = new DataLine.Info(TargetDataLine.class, audioFormat);

        try {
            final TargetDataLine line = (TargetDataLine) AudioSystem.getLine(dataLineInfo);
            line.open(audioFormat);
            line.start();

            final int sampleRate = (int) audioFormat.getSampleRate();
            final int numChannels = audioFormat.getChannels();

            int audioBufferSize = sampleRate * numChannels;
            final byte[] audioBytes = new byte[audioBufferSize];

            ScheduledThreadPoolExecutor exec = new ScheduledThreadPoolExecutor(1);
            exec.scheduleAtFixedRate(new Runnable() {
                public void run() {
                    try {
                        int nBytesRead = 0;
                        while (nBytesRead == 0) {
                            nBytesRead = line.read(audioBytes, 0, line.available());
                        }
                        int nSamplesRead = nBytesRead / 2;
                        short[] samples = new short[nSamplesRead];
                        ByteBuffer.wrap(audioBytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(samples);
                        ShortBuffer sBuff = ShortBuffer.wrap(samples, 0, nSamplesRead);

                        avcodec.AVPacket audioPacket = recorder.getAudioPacket(sampleRate, numChannels, sBuff);
                        //                                avcodec.AVPacket copy = new avcodec.AVPacket();
                        //                                avcodec.av_init_packet(copy);
                        //                                copy.size(audioPacket.size());
                        //                                avcodec.av_packet_copy_props(copy, audioPacket);
                        //                                recorder.writePacket(1, audioPacket);
                        //                                recorder1.writePacket(1, copy);
                        RecordOrStreamTest.writePkt(recorder, recorder1, audioPacket, 1);

                    } catch (org.bytedeco.javacv.FrameRecorder.Exception e) {
                        e.printStackTrace();
                    }
                }
            }, 0, (long) 1000 / (5*RecordOrStreamTest.FRAME_RATE), TimeUnit.MILLISECONDS);
        } catch (LineUnavailableException e1) {
            e1.printStackTrace();
        }
    }
}
