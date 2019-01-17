import org.bytedeco.javacpp.*;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.FFmpegFrameRecorder;
import org.bytedeco.javacv.FFmpegLockCallback;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.FrameRecorder;
import org.bytedeco.javacv.FrameRecorder.Exception;

import java.io.IOException;
import java.nio.ByteOrder;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;

/**
 * @author baojikui (bjklwr@outlook.com)
 * @date 2019/01/12
 */
public class Test1 {

    final private static String VIDEO_NAME = "/Users/baojikui/Desktop/output.mp4";
    final private static String RTMP_ADDR = "rtmp://pili-publish.daishuclass.cn/daishu-video/test_1234";

    public static void main(String[] args) throws Exception, IOException, org.bytedeco.javacv.FrameGrabber.Exception {

        int captureWidth = 1280;
        int captureHeight = 720;

        //测试packet
        FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(VIDEO_NAME);
        final FFmpegFrameRecorder recorder = new FFmpegFrameRecorder(RTMP_ADDR, captureWidth, captureHeight, 2);
        grabber.start();

        recorder.setInterleaved(true);

        //video 参数
        recorder.setVideoOption("tune", "zerolatency");
        recorder.setVideoOption("preset", "ultrafast");
        recorder.setVideoOption("crf", "28");
        recorder.setVideoBitrate(2000000);
        recorder.setVideoCodec(avcodec.AV_CODEC_ID_H264);
        recorder.setFormat("flv");
        recorder.setFrameRate(30);
        recorder.setGopSize(60);

        //audio 参数
        recorder.setAudioOption("crf", "0");
        recorder.setAudioQuality(0);
        recorder.setAudioBitrate(192000);
        recorder.setSampleRate(44100);
        recorder.setAudioChannels(2);
        recorder.setAudioCodec(avcodec.AV_CODEC_ID_AAC);
        recorder.start(grabber.getFormatContext());

        avcodec.AVPacket packet;
        long t1 = System.currentTimeMillis();
        while ((packet = grabber.grabPacket()) != null) {
            recorder.recordPacket(packet);
            if ((System.currentTimeMillis() - t1) > 20000) {
                break;
            }
        }

        recorder.stop();
        grabber.stop();
    }
}
//
//
//import java.io.File;
//                import java.io.IOException;
//                import java.io.OutputStream;
//                import java.nio.Buffer;
//                import java.nio.ByteBuffer;
//                import java.nio.ByteOrder;
//                import java.nio.DoubleBuffer;
//                import java.nio.FloatBuffer;
//                import java.nio.IntBuffer;
//                import java.nio.ShortBuffer;
//                import java.util.Collections;
//                import java.util.HashMap;
//                import java.util.Iterator;
//                import java.util.Map;
//                import java.util.Map.Entry;
//
//                import org.bytedeco.javacpp.BytePointer;
//                import org.bytedeco.javacpp.DoublePointer;
//                import org.bytedeco.javacpp.FloatPointer;
//                import org.bytedeco.javacpp.IntPointer;
//                import org.bytedeco.javacpp.Loader;
//                import org.bytedeco.javacpp.Pointer;
//                import org.bytedeco.javacpp.PointerPointer;
//                import org.bytedeco.javacpp.PointerScope;
//                import org.bytedeco.javacpp.ShortPointer;
//                import org.bytedeco.javacpp.avcodec;
//                import org.bytedeco.javacpp.avdevice;
//                import org.bytedeco.javacpp.avformat;
//                import org.bytedeco.javacpp.avutil;
//                import org.bytedeco.javacpp.swresample;
//                import org.bytedeco.javacpp.swscale;
//                import org.bytedeco.javacpp.avcodec.AVCodec;
//                import org.bytedeco.javacpp.avcodec.AVCodecContext;
//                import org.bytedeco.javacpp.avcodec.AVPacket;
//                import org.bytedeco.javacpp.avformat.AVFormatContext;
//                import org.bytedeco.javacpp.avformat.AVIOContext;
//                import org.bytedeco.javacpp.avformat.AVIOInterruptCB;
//                import org.bytedeco.javacpp.avformat.AVOutputFormat;
//                import org.bytedeco.javacpp.avformat.AVStream;
//                import org.bytedeco.javacpp.avformat.Read_packet_Pointer_BytePointer_int;
//                import org.bytedeco.javacpp.avformat.Seek_Pointer_long_int;
//                import org.bytedeco.javacpp.avformat.Write_packet_Pointer_BytePointer_int;
//                import org.bytedeco.javacpp.avutil.AVDictionary;
//                import org.bytedeco.javacpp.avutil.AVFrame;
//                import org.bytedeco.javacpp.avutil.AVRational;
//                import org.bytedeco.javacpp.swresample.SwrContext;
//                import org.bytedeco.javacpp.swscale.SwsContext;
//                import org.bytedeco.javacpp.swscale.SwsFilter;
//                import org.bytedeco.javacv.FFmpegLockCallback;
//                import org.bytedeco.javacv.Frame;
//                import org.bytedeco.javacv.FrameRecorder;
//
//public class MyFFmpegFrameRecorder extends FrameRecorder {
//    private static Exception loadingException = null;
//    static Map<Pointer, OutputStream> outputStreams;
//    static MyFFmpegFrameRecorder.WriteCallback writeCallback;
//    private OutputStream outputStream;
//    private AVIOContext avio;
//    private String filename;
//    private AVFrame picture;
//    private AVFrame tmp_picture;
//    private BytePointer picture_buf;
//    private BytePointer video_outbuf;
//    private int video_outbuf_size;
//    private AVFrame frame;
//    private Pointer[] samples_in;
//    private BytePointer[] samples_out;
//    private PointerPointer samples_in_ptr;
//    private PointerPointer samples_out_ptr;
//    private BytePointer audio_outbuf;
//    private int audio_outbuf_size;
//    private int audio_input_frame_size;
//    private AVOutputFormat oformat;
//    private AVFormatContext oc;
//    private AVCodec video_codec;
//    private AVCodec audio_codec;
//    private AVCodecContext video_c;
//    private AVCodecContext audio_c;
//    private AVStream video_st;
//    private AVStream audio_st;
//    private SwsContext img_convert_ctx;
//    private SwrContext samples_convert_ctx;
//    private int samples_channels;
//    private int samples_format;
//    private int samples_rate;
//    private AVPacket video_pkt;
//    private AVPacket audio_pkt;
//    private int[] got_video_packet;
//    private int[] got_audio_packet;
//    private AVFormatContext ifmt_ctx;
//
//
//
//    private AVFormatContext oc_c;
//    private String filename_c = "/Users/baojikui/Desktop/output.mp4";
//    private AVFrame picture_c;
//    private AVFrame tmp_picture_c;
//    private BytePointer picture_buf_c;
//    private AVFrame frame_c;
//    private BytePointer video_outbuf_c;
//    private BytePointer audio_outbuf_c;
//    private AVCodecContext video_c_c;
//    private AVCodecContext audio_c_c;
//    private AVStream video_st_c;
//    private AVStream audio_st_c;
//    private int[] got_video_packet_c;
//    private int[] got_audio_packet_c;
//    private AVOutputFormat oformat_c;
//
//
//    public static MyFFmpegFrameRecorder createDefault(File f, int w, int h) throws Exception {
//        return new MyFFmpegFrameRecorder(f, w, h);
//    }
//
//    public static MyFFmpegFrameRecorder createDefault(String f, int w, int h) throws Exception {
//        return new MyFFmpegFrameRecorder(f, w, h);
//    }
//
//    public static void tryLoad() throws Exception {
//        if (loadingException != null) {
//            throw loadingException;
//        } else {
//            try {
//                Loader.load(avutil.class);
//                Loader.load(swresample.class);
//                Loader.load(avcodec.class);
//                Loader.load(avformat.class);
//                Loader.load(swscale.class);
//                avcodec.av_jni_set_java_vm(Loader.getJavaVM(), (Pointer) null);
//                avcodec.avcodec_register_all();
//                avformat.av_register_all();
//                avformat.avformat_network_init();
//                Loader.load(avdevice.class);
//                avdevice.avdevice_register_all();
//            } catch (Throwable var1) {
//                if (var1 instanceof Exception) {
//                    throw loadingException = (Exception) var1;
//                } else {
//                    throw loadingException = new Exception("Failed to load " + MyFFmpegFrameRecorder.class, var1);
//                }
//            }
//        }
//    }
//
//    public MyFFmpegFrameRecorder(File file, int audioChannels) {
//        this((File) file, 0, 0, audioChannels);
//    }
//
//    public MyFFmpegFrameRecorder(String filename, int audioChannels) {
//        this((String) filename, 0, 0, audioChannels);
//    }
//
//    public MyFFmpegFrameRecorder(File file, int imageWidth, int imageHeight) {
//        this((File) file, imageWidth, imageHeight, 0);
//    }
//
//    public MyFFmpegFrameRecorder(String filename, int imageWidth, int imageHeight) {
//        this((String) filename, imageWidth, imageHeight, 0);
//    }
//
//    public MyFFmpegFrameRecorder(File file, int imageWidth, int imageHeight, int audioChannels) {
//        this(file.getAbsolutePath(), imageWidth, imageHeight, audioChannels);
//    }
//
//    public MyFFmpegFrameRecorder(String filename, int imageWidth, int imageHeight, int audioChannels) {
//        this.filename = filename;
//        this.imageWidth = imageWidth;
//        this.imageHeight = imageHeight;
//        this.audioChannels = audioChannels;
//        this.pixelFormat = -1;
//        this.videoCodec = 0;
//        this.videoBitrate = 400000;
//        this.frameRate = 30.0D;
//        this.sampleFormat = -1;
//        this.audioCodec = 0;
//        this.audioBitrate = 64000;
//        this.sampleRate = 44100;
//        this.interleaved = true;
//        this.video_pkt = new AVPacket();
//        this.audio_pkt = new AVPacket();
//    }
//
//    public MyFFmpegFrameRecorder(OutputStream outputStream, int audioChannels) {
//        this(outputStream.toString(), audioChannels);
//        this.outputStream = outputStream;
//    }
//
//    public MyFFmpegFrameRecorder(OutputStream outputStream, int imageWidth, int imageHeight) {
//        this(outputStream.toString(), imageWidth, imageHeight);
//        this.outputStream = outputStream;
//    }
//
//    public MyFFmpegFrameRecorder(OutputStream outputStream, int imageWidth, int imageHeight, int audioChannels) {
//        this(outputStream.toString(), imageWidth, imageHeight, audioChannels);
//        this.outputStream = outputStream;
//    }
//
//    @Override
//    public void release() throws Exception {
//        this.releaseUnsafe();
//    }
//
//    void releaseUnsafe() throws Exception {
//        if (this.video_c != null) {
//            avcodec.avcodec_free_context(this.video_c);
//            this.video_c = null;
//        }
//
//        if (this.audio_c != null) {
//            avcodec.avcodec_free_context(this.audio_c);
//            this.audio_c = null;
//        }
//
//        if (this.picture_buf != null) {
//            avutil.av_free(this.picture_buf);
//            this.picture_buf = null;
//        }
//
//        if (this.picture != null) {
//            avutil.av_frame_free(this.picture);
//            this.picture = null;
//        }
//
//        if (this.tmp_picture != null) {
//            avutil.av_frame_free(this.tmp_picture);
//            this.tmp_picture = null;
//        }
//
//        if (this.video_outbuf != null) {
//            avutil.av_free(this.video_outbuf);
//            this.video_outbuf = null;
//        }
//
//        if (this.frame != null) {
//            avutil.av_frame_free(this.frame);
//            this.frame = null;
//        }
//
//        if (this.samples_out != null) {
//            for (int i = 0; i < this.samples_out.length; ++i) {
//                avutil.av_free(this.samples_out[i].position(0L));
//            }
//
//            this.samples_out = null;
//        }
//
//        if (this.audio_outbuf != null) {
//            avutil.av_free(this.audio_outbuf);
//            this.audio_outbuf = null;
//        }
//
//        if (this.video_st != null && this.video_st.metadata() != null) {
//            avutil.av_dict_free(this.video_st.metadata());
//            this.video_st.metadata((AVDictionary) null);
//        }
//
//        if (this.audio_st != null && this.audio_st.metadata() != null) {
//            avutil.av_dict_free(this.audio_st.metadata());
//            this.audio_st.metadata((AVDictionary) null);
//        }
//
//        this.video_st = null;
//        this.audio_st = null;
//        this.filename = null;
//        AVFormatContext outputStreamKey = this.oc;
//        if (this.oc != null && !this.oc.isNull()) {
//            if (this.outputStream == null && (this.oformat.flags() & 1) == 0) {
//                avformat.avio_close(this.oc.pb());
//            }
//
//            int nb_streams = this.oc.nb_streams();
//
//            for (int i = 0; i < nb_streams; ++i) {
//                avutil.av_free(this.oc.streams(i).codec());
//                avutil.av_free(this.oc.streams(i));
//            }
//
//            if (this.oc.metadata() != null) {
//                avutil.av_dict_free(this.oc.metadata());
//                this.oc.metadata((AVDictionary) null);
//            }
//
//            avutil.av_free(this.oc);
//            this.oc = null;
//        }
//
//        if (this.img_convert_ctx != null) {
//            swscale.sws_freeContext(this.img_convert_ctx);
//            this.img_convert_ctx = null;
//        }
//
//        if (this.samples_convert_ctx != null) {
//            swresample.swr_free(this.samples_convert_ctx);
//            this.samples_convert_ctx = null;
//        }
//
//        if (this.outputStream != null) {
//            try {
//                this.outputStream.close();
//            } catch (IOException var7) {
//                throw new Exception("Error on OutputStream.close(): ", var7);
//            } finally {
//                this.outputStream = null;
//                outputStreams.remove(outputStreamKey);
//                if (this.avio != null) {
//                    if (this.avio.buffer() != null) {
//                        avutil.av_free(this.avio.buffer());
//                        this.avio.buffer((BytePointer) null);
//                    }
//
//                    avutil.av_free(this.avio);
//                    this.avio = null;
//                }
//
//            }
//        }
//
//    }
//
//    @Override
//    protected void finalize() throws Throwable {
//        super.finalize();
//        this.release();
//    }
//
//    @Override
//    public int getFrameNumber() {
//        return this.picture == null ? super.getFrameNumber() : (int) this.picture.pts();
//    }
//
//    @Override
//    public void setFrameNumber(int frameNumber) {
//        if (this.picture == null) {
//            super.setFrameNumber(frameNumber);
//        } else {
//            this.picture.pts((long) frameNumber);
//        }
//
//    }
//
//    @Override
//    public long getTimestamp() {
//        return Math.round((double) ((long) this.getFrameNumber() * 1000000L) / this.getFrameRate());
//    }
//
//    @Override
//    public void setTimestamp(long timestamp) {
//        this.setFrameNumber((int) Math.round((double) timestamp * this.getFrameRate() / 1000000.0D));
//    }
//
//    public void start(AVFormatContext ifmt_ctx) throws Exception {
//        this.ifmt_ctx = ifmt_ctx;
//        this.start();
//    }
//
//    @Override
//    public void start() throws Exception {
//        this.startUnsafe(this.picture,this.tmp_picture,this.picture_buf,this.frame,this.video_outbuf,this.audio_outbuf
//                        ,this.oc,this.video_c,this.audio_c,this.video_st,this.audio_st,this.got_video_packet,this.got_audio_packet,this.filename,this.oformat);
//
//        this.startUnsafe(this.picture_c,this.tmp_picture_c,this.picture_buf_c,this.frame_c,this.video_outbuf_c,this.audio_outbuf_c
//                        ,this.oc_c,this.video_c_c,this.audio_c_c,this.video_st_c,this.audio_st_c,this.got_video_packet_c,this.got_audio_packet_c,this.filename_c,this.oformat_c);
//    }
//
//    void startUnsafe(AVFrame picture,
//                     AVFrame tmp_picture,
//                     BytePointer picture_buf,
//                     AVFrame frame,
//                     BytePointer video_outbuf,
//                     BytePointer audio_outbuf,
//                     AVFormatContext oc,
//                     AVCodecContext video_c,
//                     AVCodecContext audio_c,
//                     AVStream video_st,
//                     AVStream audio_st,
//                     int[] got_video_packet,
//                     int[] got_audio_packet,
//                     String filename,
//                     AVOutputFormat oformat) throws Exception {
//
//        picture = null;
//        tmp_picture = null;
//        picture_buf = null;
//        frame = null;
//        video_outbuf = null;
//        audio_outbuf = null;
//        oc = new AVFormatContext((Pointer) null);
//        video_c = null;
//        audio_c = null;
//        video_st = null;
//        audio_st = null;
//        got_video_packet = new int[1];
//        got_audio_packet = new int[1];
//
//        //输出文件 格式
//        String format_name = format != null && format.length() != 0 ? format : null;
//        if ((oformat = avformat.av_guess_format(format_name, filename, (String) null)) == null) {
//            int proto = filename.indexOf("://");
//            if (proto > 0) {
//                format_name = filename.substring(0, proto);
//            }
//
//            if ((oformat = avformat.av_guess_format(format_name, filename, (String) null)) == null) {
//                throw new Exception("av_guess_format() error: Could not guess output format for \"" + filename + "\" and " + format + " format.");
//            }
//        }
//
//        format_name = oformat.name().getString();
//
//        // AVFormatContext oc 编码器 配置
//        if (avformat.avformat_alloc_output_context2(oc, (AVOutputFormat) null, format_name, filename) < 0) {
//            throw new Exception("avformat_alloc_context2() error:\tCould not allocate format context");
//        } else {
//            if (outputStream != null) {
//                avio = avformat.avio_alloc_context(new BytePointer(avutil.av_malloc(4096L)), 4096, 1, oc, (Read_packet_Pointer_BytePointer_int) null,
//                                                   writeCallback, (Seek_Pointer_long_int) null);
//                oc.pb(avio);
//                filename = outputStream.toString();
//                outputStreams.put(oc, outputStream);
//            }
//
//            oc.oformat(oformat);
//            oc.filename().putString(filename);
//            oc.max_delay(maxDelay);
//            AVStream inpVideoStream = null;
//            AVStream inpAudioStream = null;
//
//            //AVFormatContext ifmt_ctx 由start方法带入，暂未知
//            if (ifmt_ctx != null) {
//                for (int idx = 0; idx < ifmt_ctx.nb_streams(); ++idx) {
//                    AVStream inputStream = ifmt_ctx.streams(idx);
//                    if (inputStream.codec().codec_type() == 0) {
//                        inpVideoStream = inputStream;
//                        videoCodec = inputStream.codec().codec_id();
//                        if ((long) inputStream.r_frame_rate().num() != avutil.AV_NOPTS_VALUE && inputStream.r_frame_rate().den() != 0) {
//                            frameRate = (double) (inputStream.r_frame_rate().num() / inputStream.r_frame_rate().den());
//                        }
//                    } else if (inputStream.codec().codec_type() == 1) {
//                        inpAudioStream = inputStream;
//                        audioCodec = inputStream.codec().codec_id();
//                    }
//                }
//            }
//
//            // 编码器 视频编码格式
//            int data_size;
//            if (imageWidth > 0 && imageHeight > 0) {
//                if (videoCodec != 0) {
//                    oformat.video_codec(videoCodec);
//                } else if ("flv".equals(format_name)) {
//                    oformat.video_codec(21);
//                } else if ("mp4".equals(format_name)) {
//                    oformat.video_codec(12);
//                } else if ("3gp".equals(format_name)) {
//                    oformat.video_codec(4);
//                } else if ("avi".equals(format_name)) {
//                    oformat.video_codec(25);
//                }
//
//                if ((video_codec = avcodec.avcodec_find_encoder_by_name(videoCodecName)) == null
//                                && (video_codec = avcodec.avcodec_find_encoder(oformat.video_codec())) == null) {
//                    release();
//                    throw new Exception("avcodec_find_encoder() error: Video codec not found.");
//                }
//
//                oformat.video_codec(video_codec.id());
//                AVRational frame_rate = avutil.av_d2q(frameRate, 1001000);
//                AVRational supported_framerates = video_codec.supported_framerates();
//                if (supported_framerates != null) {
//                    data_size = avutil.av_find_nearest_q_idx(frame_rate, supported_framerates);
//                    frame_rate = supported_framerates.position((long) data_size);
//                }
//
//                //AVStream oc的输出视频流
//                if ((video_st = avformat.avformat_new_stream(oc, (AVCodec) null)) == null) {
//                    release();
//                    throw new Exception("avformat_new_stream() error: Could not allocate video stream.");
//                }
//                //AVStream oc的输出视频流 编码
//                if ((video_c = avcodec.avcodec_alloc_context3(video_codec)) == null) {
//                    release();
//                    throw new Exception("avcodec_alloc_context3() error: Could not allocate video encoding context.");
//                }
//
//                //同 ifmt_ctx，start带入的流
//                if (inpVideoStream != null) {
//                    if (avcodec.avcodec_copy_context(video_st.codec(), inpVideoStream.codec()) < 0) {
//                        release();
//                        throw new Exception("avcodec_copy_context() error:\tFailed to copy context from input to output stream codec context");
//                    }
//
//                    videoBitrate = (int) inpVideoStream.codec().bit_rate();
//                    pixelFormat = inpVideoStream.codec().pix_fmt();
//                    aspectRatio =
//                                    (double) (inpVideoStream.codec().sample_aspect_ratio().den() / inpVideoStream.codec().sample_aspect_ratio().den()) * 1.0D;
//                    videoQuality = (double) inpVideoStream.codec().global_quality();
//                    video_c.codec_tag(0);
//                }
//
//                //设置 oc编码器的视频编码
//                video_c.codec_id(oformat.video_codec());
//                video_c.codec_type(0);
//                video_c.bit_rate((long) videoBitrate);
//                if (imageWidth % 2 == 1) {
//                    data_size = imageWidth + 1;
//                    imageHeight = (data_size * imageHeight + imageWidth / 2) / imageWidth;
//                    imageWidth = data_size;
//                }
//
//                video_c.width(imageWidth);
//                video_c.height(imageHeight);
//                if (aspectRatio > 0.0D) {
//                    AVRational r = avutil.av_d2q(aspectRatio, 255);
//                    video_c.sample_aspect_ratio(r);
//                    video_st.sample_aspect_ratio(r);
//                }
//
//                video_c.time_base(avutil.av_inv_q(frame_rate));
//                video_st.time_base(avutil.av_inv_q(frame_rate));
//                if (gopSize >= 0) {
//                    video_c.gop_size(gopSize);
//                }
//
//                if (videoQuality >= 0.0D) {
//                    video_c.flags(video_c.flags() | 2);
//                    video_c.global_quality((int) Math.round(118.0D * videoQuality));
//                }
//
//                if (pixelFormat != -1) {
//                    video_c.pix_fmt(pixelFormat);
//                } else if (video_c.codec_id() != 13 && video_c.codec_id() != 61 && video_c.codec_id() != 25 && video_c.codec_id() != 33) {
//                    if (video_c.codec_id() == 11) {
//                        video_c.pix_fmt(3);
//                    } else if (video_c.codec_id() != 7 && video_c.codec_id() != 8) {
//                        video_c.pix_fmt(0);
//                    } else {
//                        video_c.pix_fmt(12);
//                    }
//                } else {
//                    video_c.pix_fmt(avutil.AV_PIX_FMT_RGB32);
//                }
//
//                if (video_c.codec_id() == 2) {
//                    video_c.max_b_frames(2);
//                } else if (video_c.codec_id() == 1) {
//                    video_c.mb_decision(2);
//                } else if (video_c.codec_id() == 4) {
//                    if (imageWidth <= 128 && imageHeight <= 96) {
//                        video_c.width(128).height(96);
//                    } else if (imageWidth <= 176 && imageHeight <= 144) {
//                        video_c.width(176).height(144);
//                    } else if (imageWidth <= 352 && imageHeight <= 288) {
//                        video_c.width(352).height(288);
//                    } else if (imageWidth <= 704 && imageHeight <= 576) {
//                        video_c.width(704).height(576);
//                    } else {
//                        video_c.width(1408).height(1152);
//                    }
//                } else if (video_c.codec_id() == 27) {
//                    video_c.profile(578);
//                }
//
//                if ((oformat.flags() & 64) != 0) {
//                    video_c.flags(video_c.flags() | 4194304);
//                }
//
//                if ((video_codec.capabilities() & 512) != 0) {
//                    video_c.strict_std_compliance(-2);
//                }
//
//                if (maxBFrames >= 0) {
//                    video_c.max_b_frames(maxBFrames);
//                    video_c.has_b_frames(maxBFrames == 0 ? 0 : 1);
//                }
//
//                if (trellis >= 0) {
//                    video_c.trellis(trellis);
//                }
//            }
//
//            //音频编码
//            int i;
//            if (audioChannels > 0 && audioBitrate > 0 && sampleRate > 0) {
//                if (audioCodec != 0) {
//                    oformat.audio_codec(audioCodec);
//                } else if (!"flv".equals(format_name) && !"mp4".equals(format_name) && !"3gp".equals(format_name)) {
//                    if ("avi".equals(format_name)) {
//                        oformat.audio_codec(65536);
//                    }
//                } else {
//                    oformat.audio_codec(86018);
//                }
//
//                if ((audio_codec = avcodec.avcodec_find_encoder_by_name(audioCodecName)) == null
//                                && (audio_codec = avcodec.avcodec_find_encoder(oformat.audio_codec())) == null) {
//                    release();
//                    throw new Exception("avcodec_find_encoder() error: Audio codec not found.");
//                }
//
//                oformat.audio_codec(audio_codec.id());
//                if ((audio_st = avformat.avformat_new_stream(oc, (AVCodec) null)) == null) {
//                    release();
//                    throw new Exception("avformat_new_stream() error: Could not allocate audio stream.");
//                }
//
//                if ((audio_c = avcodec.avcodec_alloc_context3(audio_codec)) == null) {
//                    release();
//                    throw new Exception("avcodec_alloc_context3() error: Could not allocate audio encoding context.");
//                }
//
//                if (inpAudioStream != null && audioChannels > 0) {
//                    if (avcodec.avcodec_copy_context(audio_st.codec(), inpAudioStream.codec()) < 0) {
//                        throw new Exception("avcodec_copy_context() error:\tFailed to copy context from input audio to output audio stream codec context\n");
//                    }
//
//                    audioBitrate = (int) inpAudioStream.codec().bit_rate();
//                    sampleRate = inpAudioStream.codec().sample_rate();
//                    audioChannels = inpAudioStream.codec().channels();
//                    sampleFormat = inpAudioStream.codec().sample_fmt();
//                    audioQuality = (double) inpAudioStream.codec().global_quality();
//                    audio_c.codec_tag(0);
//                    audio_st.duration(inpAudioStream.duration());
//                    audio_st.time_base().num(inpAudioStream.time_base().num());
//                    audio_st.time_base().den(inpAudioStream.time_base().den());
//                }
//
//                audio_c.codec_id(oformat.audio_codec());
//                audio_c.codec_type(1);
//                audio_c.bit_rate((long) audioBitrate);
//                audio_c.sample_rate(sampleRate);
//                audio_c.channels(audioChannels);
//                audio_c.channel_layout(avutil.av_get_default_channel_layout(audioChannels));
//                if (sampleFormat != -1) {
//                    audio_c.sample_fmt(sampleFormat);
//                } else {
//                    audio_c.sample_fmt(8);
//                    IntPointer formats = audio_c.codec().sample_fmts();
//
//                    for (i = 0; formats.get((long) i) != -1; ++i) {
//                        if (formats.get((long) i) == 1) {
//                            audio_c.sample_fmt(1);
//                            break;
//                        }
//                    }
//                }
//
//                audio_c.time_base().num(1).den(sampleRate);
//                audio_st.time_base().num(1).den(sampleRate);
//                switch (audio_c.sample_fmt()) {
//                    case 0:
//                    case 5:
//                        audio_c.bits_per_raw_sample(8);
//                        break;
//                    case 1:
//                    case 6:
//                        audio_c.bits_per_raw_sample(16);
//                        break;
//                    case 2:
//                    case 7:
//                        audio_c.bits_per_raw_sample(32);
//                        break;
//                    case 3:
//                    case 8:
//                        audio_c.bits_per_raw_sample(32);
//                        break;
//                    case 4:
//                    case 9:
//                        audio_c.bits_per_raw_sample(64);
//                        break;
//                    default:
//                        assert false;
//                }
//
//                if (audioQuality >= 0.0D) {
//                    audio_c.flags(audio_c.flags() | 2);
//                    audio_c.global_quality((int) Math.round(118.0D * audioQuality));
//                }
//
//                if ((oformat.flags() & 64) != 0) {
//                    audio_c.flags(audio_c.flags() | 4194304);
//                }
//
//                if ((audio_codec.capabilities() & 512) != 0) {
//                    audio_c.strict_std_compliance(-2);
//                }
//            }
//
//            int ret;
//            AVDictionary options;
//            Iterator var17;
//            Entry e;
//            //通过无参start方法调用的情况 设置AVDictionary参数
//            if (video_st != null && inpVideoStream == null) {
//                options = new AVDictionary((Pointer) null);
//                if (videoQuality >= 0.0D) {
//                    avutil.av_dict_set(options, "crf", "" + videoQuality, 0);
//                }
//
//                var17 = videoOptions.entrySet().iterator();
//
//                while (var17.hasNext()) {
//                    e = (Entry) var17.next();
//                    avutil.av_dict_set(options, (String) e.getKey(), (String) e.getValue(), 0);
//                }
//
//                if ((ret = avcodec.avcodec_open2(video_c, video_codec, options)) < 0) {
//                    release();
//                    avutil.av_dict_free(options);
//                    throw new Exception("avcodec_open2() error " + ret + ": Could not open video codec.");
//                }
//
//                avutil.av_dict_free(options);
//                video_outbuf = null;
//                if ((picture = avutil.av_frame_alloc()) == null) {
//                    release();
//                    throw new Exception("av_frame_alloc() error: Could not allocate picture.");
//                }
//
//                picture.pts(0L);
//                i = avutil.av_image_get_buffer_size(video_c.pix_fmt(), video_c.width(), video_c.height(), 1);
//                if ((picture_buf = new BytePointer(avutil.av_malloc((long) i))).isNull()) {
//                    release();
//                    throw new Exception("av_malloc() error: Could not allocate picture buffer.");
//                }
//
//                if ((tmp_picture = avutil.av_frame_alloc()) == null) {
//                    release();
//                    throw new Exception("av_frame_alloc() error: Could not allocate temporary picture.");
//                }
//
//                if (avcodec.avcodec_parameters_from_context(video_st.codecpar(), video_c) < 0) {
//                    release();
//                    throw new Exception("avcodec_parameters_from_context() error: Could not copy the video stream parameters.");
//                }
//
//                AVDictionary metadata = new AVDictionary((Pointer) null);
//                Iterator var8 = videoMetadata.entrySet().iterator();
//
//                while (var8.hasNext()) {
//                    //                    Entry<String, String>
//                    e = (Entry) var8.next();
//                    avutil.av_dict_set(metadata, (String) e.getKey(), (String) e.getValue(), 0);
//                }
//
//                video_st.metadata(metadata);
//            }
//
//            if (audio_st != null && inpAudioStream == null) {
//                options = new AVDictionary((Pointer) null);
//                if (audioQuality >= 0.0D) {
//                    avutil.av_dict_set(options, "crf", "" + audioQuality, 0);
//                }
//
//                var17 = audioOptions.entrySet().iterator();
//
//                while (var17.hasNext()) {
//                    e = (Entry) var17.next();
//                    avutil.av_dict_set(options, (String) e.getKey(), (String) e.getValue(), 0);
//                }
//
//                if ((ret = avcodec.avcodec_open2(audio_c, audio_codec, options)) < 0) {
//                    release();
//                    avutil.av_dict_free(options);
//                    throw new Exception("avcodec_open2() error " + ret + ": Could not open audio codec.");
//                }
//
//                avutil.av_dict_free(options);
//                audio_outbuf_size = 262144;
//                audio_outbuf = new BytePointer(avutil.av_malloc((long) audio_outbuf_size));
//                if (audio_c.frame_size() <= 1) {
//                    audio_outbuf_size = 16384;
//                    audio_input_frame_size = audio_outbuf_size / audio_c.channels();
//                    switch (audio_c.codec_id()) {
//                        case 65536:
//                        case 65537:
//                        case 65538:
//                        case 65539:
//                            audio_input_frame_size >>= 1;
//                    }
//                } else {
//                    audio_input_frame_size = audio_c.frame_size();
//                }
//
//                i = avutil.av_sample_fmt_is_planar(audio_c.sample_fmt()) != 0 ? audio_c.channels() : 1;
//                data_size = avutil
//                                .av_samples_get_buffer_size((IntPointer) null, audio_c.channels(), audio_input_frame_size, audio_c.sample_fmt(),
//                                                            1) / i;
//                samples_out = new BytePointer[i];
//
//                for (i = 0; i < samples_out.length; ++i) {
//                    samples_out[i] = (new BytePointer(avutil.av_malloc((long) data_size))).capacity((long) data_size);
//                }
//
//                samples_in = new Pointer[8];
//                samples_in_ptr = new PointerPointer(8L);
//                samples_out_ptr = new PointerPointer(8L);
//                if ((frame = avutil.av_frame_alloc()) == null) {
//                    release();
//                    throw new Exception("av_frame_alloc() error: Could not allocate audio frame.");
//                }
//
//                frame.pts(0L);
//                if (avcodec.avcodec_parameters_from_context(audio_st.codecpar(), audio_c) < 0) {
//                    release();
//                    throw new Exception("avcodec_parameters_from_context() error: Could not copy the audio stream parameters.");
//                }
//
//                AVDictionary metadata = new AVDictionary((Pointer) null);
//                Iterator var26 = audioMetadata.entrySet().iterator();
//
//                while (var26.hasNext()) {
//                    //                    Entry<String, String>
//                    e = (Entry) var26.next();
//                    avutil.av_dict_set(metadata, (String) e.getKey(), (String) e.getValue(), 0);
//                }
//
//                audio_st.metadata(metadata);
//            }
//
//            options = new AVDictionary((Pointer) null);
//            var17 = this.options.entrySet().iterator();
//
//            while (var17.hasNext()) {
//                e = (Entry) var17.next();
//                avutil.av_dict_set(options, (String) e.getKey(), (String) e.getValue(), 0);
//            }
//
//            //AVIOContext pb
//            if (outputStream == null && (oformat.flags() & 1) == 0) {
//                AVIOContext pb = new AVIOContext((Pointer) null);
//                if ((ret = avformat.avio_open2(pb, filename, 2, (AVIOInterruptCB) null, options)) < 0) {
//                    release();
//                    avutil.av_dict_free(options);
//                    throw new Exception("avio_open2 error() error " + ret + ": Could not open '" + filename + "'");
//                }
//
//                oc.pb(pb);
//            }
//
//            AVDictionary metadata = new AVDictionary((Pointer) null);
//            Iterator var27 = this.metadata.entrySet().iterator();
//
//            while (var27.hasNext()) {
//                //                Entry<String, String>
//                e = (Entry) var27.next();
//                avutil.av_dict_set(metadata, (String) e.getKey(), (String) e.getValue(), 0);
//            }
//
//            avformat.avformat_write_header(oc.metadata(metadata), options);
//            avutil.av_dict_free(options);
//            if (avutil.av_log_get_level() >= 32) {
//                avformat.av_dump_format(oc, 0, filename, 1);
//            }
//
//        }
//    }
//
//    @Override
//    public void stop() throws Exception {
//        if (this.oc != null) {
//            try {
//                AVFormatContext var1 = this.oc;
//                synchronized (this.oc) {
//                    while (this.video_st != null && this.ifmt_ctx == null && this.recordImage(0, 0, 0, 0, 0, -1, (Buffer[]) null)) {
//                        ;
//                    }
//
//                    while (this.audio_st != null && this.ifmt_ctx == null && this.recordSamples(0, 0, (Buffer[]) null)) {
//                        ;
//                    }
//
//                    if (this.interleaved && this.video_st != null && this.audio_st != null) {
//                        avformat.av_interleaved_write_frame(this.oc, (AVPacket) null);
//                    } else {
//                        avformat.av_write_frame(this.oc, (AVPacket) null);
//                    }
//
//                    avformat.av_write_trailer(this.oc);
//                }
//            } finally {
//                this.release();
//            }
//        }
//
//    }
//
//    @Override
//    public void record(Frame frame) throws Exception {
//        this.record(frame, -1);
//    }
//
//    public void record(Frame frame, int pixelFormat) throws Exception {
//        if (frame != null && (frame.image != null || frame.samples != null)) {
//            if (frame.image != null) {
//                frame.keyFrame = this.recordImage(frame.imageWidth, frame.imageHeight, frame.imageDepth, frame.imageChannels, frame.imageStride, pixelFormat,
//                                                  frame.image);
//            }
//
//            if (frame.samples != null) {
//                frame.keyFrame = this.recordSamples(frame.sampleRate, frame.audioChannels, frame.samples);
//            }
//        } else {
//            this.recordImage(0, 0, 0, 0, 0, pixelFormat, (Buffer[]) null);
//        }
//
//    }
//
//    public boolean recordImage(int width, int height, int depth, int channels, int stride, int pixelFormat, Buffer... image) throws Exception {
//        if (this.video_st == null) {
//            throw new Exception("No video output stream (Is imageWidth > 0 && imageHeight > 0 and has start() been called?)");
//        } else {
//            if (image != null && image.length != 0) {
//                int step = stride * Math.abs(depth) / 8;
//                BytePointer data = image[0] instanceof ByteBuffer ?
//                                new BytePointer((ByteBuffer) image[0].position(0)) :
//                                new BytePointer(new Pointer(image[0].position(0)));
//                if (pixelFormat == -1) {
//                    if ((depth == 8 || depth == -8) && channels == 3) {
//                        pixelFormat = 3;
//                    } else if ((depth == 8 || depth == -8) && channels == 1) {
//                        pixelFormat = 8;
//                    } else if ((depth == 16 || depth == -16) && channels == 1) {
//                        pixelFormat = ByteOrder.nativeOrder().equals(ByteOrder.BIG_ENDIAN) ? 29 : 30;
//                    } else if ((depth == 8 || depth == -8) && channels == 4) {
//                        pixelFormat = 26;
//                    } else {
//                        if (depth != 8 && depth != -8 || channels != 2) {
//                            throw new Exception("Could not guess pixel format of image: depth=" + depth + ", channels=" + channels);
//                        }
//
//                        pixelFormat = 24;
//                    }
//                }
//
//                if (pixelFormat == 24) {
//                    step = width;
//                }
//
//                if (this.video_c.pix_fmt() == pixelFormat && this.video_c.width() == width && this.video_c.height() == height) {
//                    avutil.av_image_fill_arrays(new PointerPointer(this.picture), this.picture.linesize(), data, pixelFormat, width, height, 1);
//                    this.picture.linesize(0, step);
//                    this.picture.format(pixelFormat);
//                    this.picture.width(width);
//                    this.picture.height(height);
//                } else {
//                    this.img_convert_ctx = swscale
//                                    .sws_getCachedContext(this.img_convert_ctx, width, height, pixelFormat, this.video_c.width(), this.video_c.height(),
//                                                          this.video_c.pix_fmt(), this.imageScalingFlags != 0 ? this.imageScalingFlags : 2, (SwsFilter) null,
//                                                          (SwsFilter) null, (DoublePointer) null);
//                    if (this.img_convert_ctx == null) {
//                        throw new Exception("sws_getCachedContext() error: Cannot initialize the conversion context.");
//                    }
//
//                    avutil.av_image_fill_arrays(new PointerPointer(this.tmp_picture), this.tmp_picture.linesize(), data, pixelFormat, width, height, 1);
//                    avutil.av_image_fill_arrays(new PointerPointer(this.picture), this.picture.linesize(), this.picture_buf, this.video_c.pix_fmt(),
//                                                this.video_c.width(), this.video_c.height(), 1);
//                    this.tmp_picture.linesize(0, step);
//                    this.tmp_picture.format(pixelFormat);
//                    this.tmp_picture.width(width);
//                    this.tmp_picture.height(height);
//                    this.picture.format(this.video_c.pix_fmt());
//                    this.picture.width(this.video_c.width());
//                    this.picture.height(this.video_c.height());
//                    swscale.sws_scale(this.img_convert_ctx, new PointerPointer(this.tmp_picture), this.tmp_picture.linesize(), 0, height,
//                                      new PointerPointer(this.picture), this.picture.linesize());
//                }
//            }
//
//            avcodec.av_init_packet(this.video_pkt);
//            this.video_pkt.data(this.video_outbuf);
//            this.video_pkt.size(this.video_outbuf_size);
//            this.picture.quality(this.video_c.global_quality());
//            int ret;
//            if ((ret = avcodec.avcodec_encode_video2(this.video_c, this.video_pkt, image != null && image.length != 0 ? this.picture : null,
//                                                     this.got_video_packet)) < 0) {
//                throw new Exception("avcodec_encode_video2() error " + ret + ": Could not encode video packet.");
//            } else {
//                this.picture.pts(this.picture.pts() + 1L);
//                if (this.got_video_packet[0] != 0) {
//                    if (this.video_pkt.pts() != avutil.AV_NOPTS_VALUE) {
//                        this.video_pkt.pts(avutil.av_rescale_q(this.video_pkt.pts(), this.video_c.time_base(), this.video_st.time_base()));
//                    }
//
//                    if (this.video_pkt.dts() != avutil.AV_NOPTS_VALUE) {
//                        this.video_pkt.dts(avutil.av_rescale_q(this.video_pkt.dts(), this.video_c.time_base(), this.video_st.time_base()));
//                    }
//
//                    this.video_pkt.stream_index(this.video_st.index());
//
//                    AVPacket test = new AVPacket();
//                    avcodec.av_init_packet(test);
//                    test.data(this.video_outbuf);
//                    test.size(this.video_outbuf_size);
//
//                    this.writePacket(0, this.video_pkt);
//                    return image != null ? (this.video_pkt.flags() & 1) != 0 : this.got_video_packet[0] != 0;
//                } else {
//                    return false;
//                }
//            }
//        }
//    }
//
//    public boolean recordSamples(Buffer... samples) throws Exception {
//        return this.recordSamples(0, 0, samples);
//    }
//
//    public boolean recordSamples(int sampleRate, int audioChannels, Buffer... samples) throws Exception {
//        if (this.audio_st == null) {
//            throw new Exception("No audio output stream (Is audioChannels > 0 and has start() been called?)");
//        } else if (samples == null && this.samples_out[0].position() > 0L) {
//            double sampleDivisor = Math.floor((double) ((int) Math.min(this.samples_out[0].limit(), 2147483647L) / this.audio_input_frame_size));
//            this.writeSamples((int) Math.floor((double) ((int) this.samples_out[0].position()) / sampleDivisor));
//            return this.record((AVFrame) null);
//        } else {
//            if (sampleRate <= 0) {
//                sampleRate = this.audio_c.sample_rate();
//            }
//
//            if (audioChannels <= 0) {
//                audioChannels = this.audio_c.channels();
//            }
//
//            int inputSize = samples != null ? samples[0].limit() - samples[0].position() : 0;
//            int inputFormat = this.samples_format;
//            int inputChannels = samples != null && samples.length > 1 ? 1 : audioChannels;
//            int inputDepth = 0;
//            int outputFormat = this.audio_c.sample_fmt();
//            int outputChannels = this.samples_out.length > 1 ? 1 : this.audio_c.channels();
//            int outputDepth = avutil.av_get_bytes_per_sample(outputFormat);
//            int inputCount;
//            if (samples != null && samples[0] instanceof ByteBuffer) {
//                inputFormat = samples.length > 1 ? 5 : 0;
//                inputDepth = 1;
//
//                for (inputCount = 0; inputCount < samples.length; ++inputCount) {
//                    ByteBuffer b = (ByteBuffer) samples[inputCount];
//                    if (this.samples_in[inputCount] instanceof BytePointer && this.samples_in[inputCount].capacity() >= (long) inputSize && b.hasArray()) {
//                        ((BytePointer) this.samples_in[inputCount]).position(0L).put(b.array(), b.position(), inputSize);
//                    } else {
//                        this.samples_in[inputCount] = new BytePointer(b);
//                    }
//                }
//            } else if (samples != null && samples[0] instanceof ShortBuffer) {
//                inputFormat = samples.length > 1 ? 6 : 1;
//                inputDepth = 2;
//
//                for (inputCount = 0; inputCount < samples.length; ++inputCount) {
//                    ShortBuffer b = (ShortBuffer) samples[inputCount];
//                    if (this.samples_in[inputCount] instanceof ShortPointer && this.samples_in[inputCount].capacity() >= (long) inputSize && b.hasArray()) {
//                        ((ShortPointer) this.samples_in[inputCount]).position(0L).put(b.array(), samples[inputCount].position(), inputSize);
//                    } else {
//                        this.samples_in[inputCount] = new ShortPointer(b);
//                    }
//                }
//            } else if (samples != null && samples[0] instanceof IntBuffer) {
//                inputFormat = samples.length > 1 ? 7 : 2;
//                inputDepth = 4;
//
//                for (inputCount = 0; inputCount < samples.length; ++inputCount) {
//                    IntBuffer b = (IntBuffer) samples[inputCount];
//                    if (this.samples_in[inputCount] instanceof IntPointer && this.samples_in[inputCount].capacity() >= (long) inputSize && b.hasArray()) {
//                        ((IntPointer) this.samples_in[inputCount]).position(0L).put(b.array(), samples[inputCount].position(), inputSize);
//                    } else {
//                        this.samples_in[inputCount] = new IntPointer(b);
//                    }
//                }
//            } else if (samples != null && samples[0] instanceof FloatBuffer) {
//                inputFormat = samples.length > 1 ? 8 : 3;
//                inputDepth = 4;
//
//                for (inputCount = 0; inputCount < samples.length; ++inputCount) {
//                    FloatBuffer b = (FloatBuffer) samples[inputCount];
//                    if (this.samples_in[inputCount] instanceof FloatPointer && this.samples_in[inputCount].capacity() >= (long) inputSize && b.hasArray()) {
//                        ((FloatPointer) this.samples_in[inputCount]).position(0L).put(b.array(), b.position(), inputSize);
//                    } else {
//                        this.samples_in[inputCount] = new FloatPointer(b);
//                    }
//                }
//            } else if (samples != null && samples[0] instanceof DoubleBuffer) {
//                inputFormat = samples.length > 1 ? 9 : 4;
//                inputDepth = 8;
//
//                for (inputCount = 0; inputCount < samples.length; ++inputCount) {
//                    DoubleBuffer b = (DoubleBuffer) samples[inputCount];
//                    if (this.samples_in[inputCount] instanceof DoublePointer && this.samples_in[inputCount].capacity() >= (long) inputSize && b.hasArray()) {
//                        ((DoublePointer) this.samples_in[inputCount]).position(0L).put(b.array(), b.position(), inputSize);
//                    } else {
//                        this.samples_in[inputCount] = new DoublePointer(b);
//                    }
//                }
//            } else if (samples != null) {
//                throw new Exception("Audio samples Buffer has unsupported type: " + samples);
//            }
//
//            int ret;
//            if (this.samples_convert_ctx == null || this.samples_channels != audioChannels || this.samples_format != inputFormat
//                            || this.samples_rate != sampleRate) {
//                this.samples_convert_ctx = swresample
//                                .swr_alloc_set_opts(this.samples_convert_ctx, this.audio_c.channel_layout(), outputFormat, this.audio_c.sample_rate(),
//                                                    avutil.av_get_default_channel_layout(audioChannels), inputFormat, sampleRate, 0, (Pointer) null);
//                if (this.samples_convert_ctx == null) {
//                    throw new Exception("swr_alloc_set_opts() error: Cannot allocate the conversion context.");
//                }
//
//                if ((ret = swresample.swr_init(this.samples_convert_ctx)) < 0) {
//                    throw new Exception("swr_init() error " + ret + ": Cannot initialize the conversion context.");
//                }
//
//                this.samples_channels = audioChannels;
//                this.samples_format = inputFormat;
//                this.samples_rate = sampleRate;
//            }
//
//            for (inputCount = 0; samples != null && inputCount < samples.length; ++inputCount) {
//                this.samples_in[inputCount].position(this.samples_in[inputCount].position() * (long) inputDepth)
//                                .limit((this.samples_in[inputCount].position() + (long) inputSize) * (long) inputDepth);
//            }
//
//            while (true) {
//                do {
//                    inputCount = (int) Math.min(samples != null ?
//                                                                (this.samples_in[0].limit() - this.samples_in[0].position()) / (long) (inputChannels
//                                                                                * inputDepth) :
//                                                                0L, 2147483647L);
//                    int outputCount = (int) Math
//                                    .min((this.samples_out[0].limit() - this.samples_out[0].position()) / (long) (outputChannels * outputDepth), 2147483647L);
//                    inputCount = Math.min(inputCount, (outputCount * sampleRate + this.audio_c.sample_rate() - 1) / this.audio_c.sample_rate());
//
//                    int i;
//                    for (i = 0; samples != null && i < samples.length; ++i) {
//                        this.samples_in_ptr.put((long) i, this.samples_in[i]);
//                    }
//
//                    for (i = 0; i < this.samples_out.length; ++i) {
//                        this.samples_out_ptr.put((long) i, this.samples_out[i]);
//                    }
//
//                    if ((ret = swresample.swr_convert(this.samples_convert_ctx, this.samples_out_ptr, outputCount, this.samples_in_ptr, inputCount)) < 0) {
//                        throw new Exception("swr_convert() error " + ret + ": Cannot convert audio samples.");
//                    }
//
//                    if (ret == 0) {
//                        return samples != null ? this.frame.key_frame() != 0 : this.record((AVFrame) null);
//                    }
//
//                    for (i = 0; samples != null && i < samples.length; ++i) {
//                        this.samples_in[i].position(this.samples_in[i].position() + (long) (inputCount * inputChannels * inputDepth));
//                    }
//
//                    for (i = 0; i < this.samples_out.length; ++i) {
//                        this.samples_out[i].position(this.samples_out[i].position() + (long) (ret * outputChannels * outputDepth));
//                    }
//                } while (samples != null && this.samples_out[0].position() < this.samples_out[0].limit());
//
//                this.writeSamples(this.audio_input_frame_size);
//            }
//        }
//    }
//
//    private void writeSamples(int nb_samples) throws Exception {
//        if (this.samples_out != null && this.samples_out.length != 0) {
//            this.frame.nb_samples(nb_samples);
//            avcodec.avcodec_fill_audio_frame(this.frame, this.audio_c.channels(), this.audio_c.sample_fmt(), this.samples_out[0],
//                                             (int) this.samples_out[0].position(), 0);
//
//            for (int i = 0; i < this.samples_out.length; ++i) {
//                //                int linesize = false;
//                int linesize;
//                if (this.samples_out[0].position() > 0L && this.samples_out[0].position() < this.samples_out[0].limit()) {
//                    linesize = (int) this.samples_out[i].position();
//                } else {
//                    linesize = (int) Math.min(this.samples_out[i].limit(), 2147483647L);
//                }
//
//                this.frame.data(i, this.samples_out[i].position(0L));
//                this.frame.linesize(i, linesize);
//            }
//
//            this.frame.quality(this.audio_c.global_quality());
//            this.record(this.frame);
//        }
//    }
//
//    boolean record(AVFrame frame) throws Exception {
//        avcodec.av_init_packet(this.audio_pkt);
//        this.audio_pkt.data(this.audio_outbuf);
//        this.audio_pkt.size(this.audio_outbuf_size);
//        int ret;
//        if ((ret = avcodec.avcodec_encode_audio2(this.audio_c, this.audio_pkt, frame, this.got_audio_packet)) < 0) {
//            throw new Exception("avcodec_encode_audio2() error " + ret + ": Could not encode audio packet.");
//        } else {
//            if (frame != null) {
//                frame.pts(frame.pts() + (long) frame.nb_samples());
//            }
//
//            if (this.got_audio_packet[0] != 0) {
//                if (this.audio_pkt.pts() != avutil.AV_NOPTS_VALUE) {
//                    this.audio_pkt.pts(avutil.av_rescale_q(this.audio_pkt.pts(), this.audio_c.time_base(), this.audio_st.time_base()));
//                }
//
//                if (this.audio_pkt.dts() != avutil.AV_NOPTS_VALUE) {
//                    this.audio_pkt.dts(avutil.av_rescale_q(this.audio_pkt.dts(), this.audio_c.time_base(), this.audio_st.time_base()));
//                }
//
//                this.audio_pkt.flags(this.audio_pkt.flags() | 1);
//                this.audio_pkt.stream_index(this.audio_st.index());
//                this.writePacket(1, this.audio_pkt);
//                return true;
//            } else {
//                return false;
//            }
//        }
//    }
//
//    private void writePacket(int mediaType, AVPacket avPacket) throws Exception {
//        AVStream avStream = mediaType == 0 ? this.audio_st : (mediaType == 1 ? this.video_st : null);
//        String mediaTypeStr = mediaType == 0 ? "video" : (mediaType == 1 ? "audio" : "unsupported media stream type");
//        AVFormatContext var5 = this.oc;
//
//        AVPacket test = new AVPacket();
//        avcodec.av_packet_copy_props(test,avPacket);
//
//        synchronized (this.oc) {
//            int ret;
//            if (this.interleaved && avStream != null) {
//                if ((ret = avformat.av_interleaved_write_frame(this.oc, avPacket)) < 0) {
//                    throw new Exception("av_interleaved_write_frame() error " + ret + " while writing interleaved " + mediaTypeStr + " packet.");
//                }
//            } else if ((ret = avformat.av_write_frame(this.oc, avPacket)) < 0) {
//                throw new Exception("av_write_frame() error " + ret + " while writing " + mediaTypeStr + " packet.");
//            }
//        }
//
//        synchronized (this.oc_c) {
//            int ret;
//            if (this.interleaved && avStream != null) {
//                if ((ret = avformat.av_interleaved_write_frame(this.oc_c, test)) < 0) {
//                    throw new Exception("av_interleaved_write_frame() error " + ret + " while writing interleaved " + mediaTypeStr + " packet.");
//                }
//            } else if ((ret = avformat.av_write_frame(this.oc_c, test)) < 0) {
//                throw new Exception("av_write_frame() error " + ret + " while writing " + mediaTypeStr + " packet.");
//            }
//        }
//    }
//
//    public boolean recordPacket(AVPacket pkt) throws Exception {
//        if (pkt == null) {
//            return false;
//        } else {
//            AVStream in_stream = this.ifmt_ctx.streams(pkt.stream_index());
//            pkt.dts(avutil.AV_NOPTS_VALUE);
//            pkt.pts(avutil.AV_NOPTS_VALUE);
//            pkt.pos(-1L);
//            if (in_stream.codec().codec_type() == 0 && this.video_st != null) {
//                pkt.stream_index(this.video_st.index());
//                pkt.duration((long) ((int) avutil.av_rescale_q(pkt.duration(), in_stream.codec().time_base(), this.video_st.codec().time_base())));
//                this.writePacket(0, pkt);
//            } else if (in_stream.codec().codec_type() == 1 && this.audio_st != null && this.audioChannels > 0) {
//                pkt.stream_index(this.audio_st.index());
//                pkt.duration((long) ((int) avutil.av_rescale_q(pkt.duration(), in_stream.codec().time_base(), this.audio_st.codec().time_base())));
//                this.writePacket(1, pkt);
//            }
//
//            avcodec.av_free_packet(pkt);
//            return true;
//        }
//    }
//
//    static {
//        try {
//            tryLoad();
//            FFmpegLockCallback.init();
//        } catch (Exception var1) {
//            var1.printStackTrace();
//        }
//
//        outputStreams = Collections.synchronizedMap(new HashMap());
//        writeCallback = new MyFFmpegFrameRecorder.WriteCallback();
//        PointerScope s = PointerScope.getInnerScope();
//        if (s != null) {
//            s.detach(writeCallback);
//        }
//
//    }
//
//    static class WriteCallback extends Write_packet_Pointer_BytePointer_int {
//        WriteCallback() {
//        }
//
//        @Override
//        public int call(Pointer opaque, BytePointer buf, int buf_size) {
//            try {
//                byte[] b = new byte[buf_size];
//                OutputStream os = (OutputStream) MyFFmpegFrameRecorder.outputStreams.get(opaque);
//                buf.get(b, 0, buf_size);
//                os.write(b, 0, buf_size);
//                return buf_size;
//            } catch (Throwable var6) {
//                System.err.println("Error on OutputStream.write(): " + var6);
//                return -1;
//            }
//        }
//    }
//}

