import org.bytedeco.javacpp.*;
import org.bytedeco.javacpp.avcodec.AVCodec;
import org.bytedeco.javacpp.avcodec.AVCodecContext;
import org.bytedeco.javacpp.avcodec.AVPacket;
import org.bytedeco.javacpp.avformat.*;
import org.bytedeco.javacpp.avutil.AVDictionary;
import org.bytedeco.javacpp.avutil.AVFrame;
import org.bytedeco.javacpp.avutil.AVRational;
import org.bytedeco.javacpp.swresample.SwrContext;
import org.bytedeco.javacpp.swscale.SwsContext;
import org.bytedeco.javacv.FFmpegLockCallback;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.FrameRecorder;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

public class MyFFmpegFrameRecorder extends FrameRecorder {
    private static Exception loadingException = null;
    static Map<Pointer, OutputStream> outputStreams;
    static MyFFmpegFrameRecorder.WriteCallback writeCallback;
    private OutputStream outputStream;
    private AVIOContext avio;
    private String filename;
    private AVFrame picture;
    private AVFrame tmp_picture;
    private BytePointer picture_buf;
    private BytePointer video_outbuf;
    private int video_outbuf_size;
    private AVFrame frame;
    private Pointer[] samples_in;
    private BytePointer[] samples_out;
    private PointerPointer samples_in_ptr;
    private PointerPointer samples_out_ptr;
    private BytePointer audio_outbuf;
    private int audio_outbuf_size;
    private int audio_input_frame_size;
    private AVOutputFormat oformat;
    private AVFormatContext oc;
    private AVCodec video_codec;
    private AVCodec audio_codec;
    private AVCodecContext video_c;
    private AVCodecContext audio_c;
    private AVStream video_st;
    private AVStream audio_st;
    private SwsContext img_convert_ctx;
    private SwrContext samples_convert_ctx;
    private int samples_channels;
    private int samples_format;
    private int samples_rate;
    private AVPacket video_pkt;
    private AVPacket audio_pkt;
    private int[] got_video_packet;
    private int[] got_audio_packet;

    public static void tryLoad() throws Exception {
        if (loadingException != null) {
            throw loadingException;
        } else {
            try {
                Loader.load(avutil.class);
                Loader.load(swresample.class);
                Loader.load(avcodec.class);
                Loader.load(avformat.class);
                Loader.load(swscale.class);
                avcodec.av_jni_set_java_vm(Loader.getJavaVM(), (Pointer) null);
                avcodec.avcodec_register_all();
                avformat.av_register_all();
                avformat.avformat_network_init();
                Loader.load(avdevice.class);
                avdevice.avdevice_register_all();
            } catch (Throwable var1) {
                if (var1 instanceof Exception) {
                    throw loadingException = (Exception) var1;
                } else {
                    throw loadingException = new Exception("Failed to load " + MyFFmpegFrameRecorder.class, var1);
                }
            }
        }
    }

    public MyFFmpegFrameRecorder(String filename, int imageWidth, int imageHeight, int audioChannels) {
        this.filename = filename;
        this.imageWidth = imageWidth;
        this.imageHeight = imageHeight;
        this.audioChannels = audioChannels;
        this.pixelFormat = -1;
        this.videoCodec = 0;
        this.videoBitrate = 400000;
        this.frameRate = 30.0D;
        this.sampleFormat = -1;
        this.audioCodec = 0;
        this.audioBitrate = 64000;
        this.sampleRate = 44100;
        this.interleaved = true;
        this.video_pkt = new AVPacket();
        this.audio_pkt = new AVPacket();
    }

    @Override
    public void release() throws Exception {
        this.releaseUnsafe();
    }

    void releaseUnsafe() throws Exception {
        if (this.video_c != null) {
            avcodec.avcodec_free_context(this.video_c);
            this.video_c = null;
        }

        if (this.audio_c != null) {
            avcodec.avcodec_free_context(this.audio_c);
            this.audio_c = null;
        }

        if (this.picture_buf != null) {
            avutil.av_free(this.picture_buf);
            this.picture_buf = null;
        }

        if (this.picture != null) {
            avutil.av_frame_free(this.picture);
            this.picture = null;
        }

        if (this.tmp_picture != null) {
            avutil.av_frame_free(this.tmp_picture);
            this.tmp_picture = null;
        }

        if (this.video_outbuf != null) {
            avutil.av_free(this.video_outbuf);
            this.video_outbuf = null;
        }

        if (this.frame != null) {
            avutil.av_frame_free(this.frame);
            this.frame = null;
        }

        if (this.samples_out != null) {
            for (int i = 0; i < this.samples_out.length; ++i) {
                avutil.av_free(this.samples_out[i].position(0L));
            }

            this.samples_out = null;
        }

        if (this.audio_outbuf != null) {
            avutil.av_free(this.audio_outbuf);
            this.audio_outbuf = null;
        }

        if (this.video_st != null && this.video_st.metadata() != null) {
            avutil.av_dict_free(this.video_st.metadata());
            this.video_st.metadata((AVDictionary) null);
        }

        if (this.audio_st != null && this.audio_st.metadata() != null) {
            avutil.av_dict_free(this.audio_st.metadata());
            this.audio_st.metadata((AVDictionary) null);
        }

        this.video_st = null;
        this.audio_st = null;
        this.filename = null;
        AVFormatContext outputStreamKey = this.oc;
        if (this.oc != null && !this.oc.isNull()) {
            if (this.outputStream == null && (this.oformat.flags() & 1) == 0) {
                avformat.avio_close(this.oc.pb());
            }

            int nb_streams = this.oc.nb_streams();

            for (int i = 0; i < nb_streams; ++i) {
                avutil.av_free(this.oc.streams(i).codec());
                avutil.av_free(this.oc.streams(i));
            }

            if (this.oc.metadata() != null) {
                avutil.av_dict_free(this.oc.metadata());
                this.oc.metadata((AVDictionary) null);
            }

            avutil.av_free(this.oc);
            this.oc = null;
        }

        if (this.img_convert_ctx != null) {
            swscale.sws_freeContext(this.img_convert_ctx);
            this.img_convert_ctx = null;
        }

        if (this.samples_convert_ctx != null) {
            swresample.swr_free(this.samples_convert_ctx);
            this.samples_convert_ctx = null;
        }

        if (this.outputStream != null) {
            try {
                this.outputStream.close();
            } catch (IOException var7) {
                throw new Exception("Error on OutputStream.close(): ", var7);
            } finally {
                this.outputStream = null;
                outputStreams.remove(outputStreamKey);
                if (this.avio != null) {
                    if (this.avio.buffer() != null) {
                        avutil.av_free(this.avio.buffer());
                        this.avio.buffer((BytePointer) null);
                    }

                    avutil.av_free(this.avio);
                    this.avio = null;
                }

            }
        }

    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        this.release();
    }

    @Override
    public int getFrameNumber() {
        return this.picture == null ? super.getFrameNumber() : (int) this.picture.pts();
    }

    @Override
    public void setFrameNumber(int frameNumber) {
        if (this.picture == null) {
            super.setFrameNumber(frameNumber);
        } else {
            this.picture.pts((long) frameNumber);
        }

    }

    @Override
    public long getTimestamp() {
        return Math.round((double) ((long) this.getFrameNumber() * 1000000L) / this.getFrameRate());
    }

    @Override
    public void setTimestamp(long timestamp) {
        this.setFrameNumber((int) Math.round((double) timestamp * this.getFrameRate() / 1000000.0D));
    }

    @Override
    public void start() throws Exception {
        this.startUnsafe();
    }

    void startUnsafe() throws Exception {

        this.picture = null;
        this.tmp_picture = null;
        this.picture_buf = null;
        this.frame = null;
        this.video_outbuf = null;
        this.audio_outbuf = null;
        this.oc = new AVFormatContext((Pointer) null);
        this.video_c = null;
        this.audio_c = null;
        this.video_st = null;
        this.audio_st = null;
        this.got_video_packet = new int[1];
        this.got_audio_packet = new int[1];

        //输出文件 格式
        String format_name = this.format != null && this.format.length() != 0 ? this.format : null;
        if ((this.oformat = avformat.av_guess_format(format_name, this.filename, (String) null)) == null) {
            int proto = this.filename.indexOf("://");
            if (proto > 0) {
                format_name = this.filename.substring(0, proto);
            }

            if ((this.oformat = avformat.av_guess_format(format_name, this.filename, (String) null)) == null) {
                throw new Exception("av_guess_format() error: Could not guess output format for \"" + this.filename + "\" and " + this.format + " format.");
            }
        }

        format_name = this.oformat.name().getString();

        // AVFormatContext oc 编码器 配置
        if (avformat.avformat_alloc_output_context2(this.oc, (AVOutputFormat) null, format_name, this.filename) < 0) {
            throw new Exception("avformat_alloc_context2() error:\tCould not allocate format context");
        } else {
            if (this.outputStream != null) {
                this.avio = avformat.avio_alloc_context(new BytePointer(avutil.av_malloc(4096L)), 4096, 1, this.oc, (Read_packet_Pointer_BytePointer_int) null,
                                                        writeCallback, (Seek_Pointer_long_int) null);
                this.oc.pb(this.avio);
                this.filename = this.outputStream.toString();
                outputStreams.put(this.oc, this.outputStream);
            }

            this.oc.oformat(this.oformat);
            this.oc.filename().putString(this.filename);
            this.oc.max_delay(this.maxDelay);
            AVStream inpVideoStream = null;
            AVStream inpAudioStream = null;

            // 编码器 视频编码格式
            int data_size;
            if (this.imageWidth > 0 && this.imageHeight > 0) {
                if (this.videoCodec != 0) {
                    this.oformat.video_codec(this.videoCodec);
                } else if ("flv".equals(format_name)) {
                    this.oformat.video_codec(21);
                } else if ("mp4".equals(format_name)) {
                    this.oformat.video_codec(12);
                } else if ("3gp".equals(format_name)) {
                    this.oformat.video_codec(4);
                } else if ("avi".equals(format_name)) {
                    this.oformat.video_codec(25);
                }

                if ((this.video_codec = avcodec.avcodec_find_encoder_by_name(this.videoCodecName)) == null
                                && (this.video_codec = avcodec.avcodec_find_encoder(this.oformat.video_codec())) == null) {
                    this.release();
                    throw new Exception("avcodec_find_encoder() error: Video codec not found.");
                }

                this.oformat.video_codec(this.video_codec.id());
                AVRational frame_rate = avutil.av_d2q(this.frameRate, 1001000);
                AVRational supported_framerates = this.video_codec.supported_framerates();
                if (supported_framerates != null) {
                    data_size = avutil.av_find_nearest_q_idx(frame_rate, supported_framerates);
                    frame_rate = supported_framerates.position((long) data_size);
                }

                //AVStream oc的输出视频流
                if ((this.video_st = avformat.avformat_new_stream(this.oc, (AVCodec) null)) == null) {
                    this.release();
                    throw new Exception("avformat_new_stream() error: Could not allocate video stream.");
                }
                //AVStream oc的输出视频流 编码
                if ((this.video_c = avcodec.avcodec_alloc_context3(this.video_codec)) == null) {
                    this.release();
                    throw new Exception("avcodec_alloc_context3() error: Could not allocate video encoding context.");
                }

                //同 ifmt_ctx，start带入的流
                if (inpVideoStream != null) {
                    if (avcodec.avcodec_copy_context(this.video_st.codec(), inpVideoStream.codec()) < 0) {
                        this.release();
                        throw new Exception("avcodec_copy_context() error:\tFailed to copy context from input to output stream codec context");
                    }

                    this.videoBitrate = (int) inpVideoStream.codec().bit_rate();
                    this.pixelFormat = inpVideoStream.codec().pix_fmt();
                    this.aspectRatio =
                                    (double) (inpVideoStream.codec().sample_aspect_ratio().den() / inpVideoStream.codec().sample_aspect_ratio().den()) * 1.0D;
                    this.videoQuality = (double) inpVideoStream.codec().global_quality();
                    this.video_c.codec_tag(0);
                }

                //设置 oc编码器的视频编码
                this.video_c.codec_id(this.oformat.video_codec());
                this.video_c.codec_type(0);
                this.video_c.bit_rate((long) this.videoBitrate);
                if (this.imageWidth % 2 == 1) {
                    data_size = this.imageWidth + 1;
                    this.imageHeight = (data_size * this.imageHeight + this.imageWidth / 2) / this.imageWidth;
                    this.imageWidth = data_size;
                }

                this.video_c.width(this.imageWidth);
                this.video_c.height(this.imageHeight);
                if (this.aspectRatio > 0.0D) {
                    AVRational r = avutil.av_d2q(this.aspectRatio, 255);
                    this.video_c.sample_aspect_ratio(r);
                    this.video_st.sample_aspect_ratio(r);
                }

                this.video_c.time_base(avutil.av_inv_q(frame_rate));
                this.video_st.time_base(avutil.av_inv_q(frame_rate));
                if (this.gopSize >= 0) {
                    this.video_c.gop_size(this.gopSize);
                }

                if (this.videoQuality >= 0.0D) {
                    this.video_c.flags(this.video_c.flags() | 2);
                    this.video_c.global_quality((int) Math.round(118.0D * this.videoQuality));
                }

                if (this.pixelFormat != -1) {
                    this.video_c.pix_fmt(this.pixelFormat);
                } else if (this.video_c.codec_id() != 13 && this.video_c.codec_id() != 61 && this.video_c.codec_id() != 25 && this.video_c.codec_id() != 33) {
                    if (this.video_c.codec_id() == 11) {
                        this.video_c.pix_fmt(3);
                    } else if (this.video_c.codec_id() != 7 && this.video_c.codec_id() != 8) {
                        this.video_c.pix_fmt(0);
                    } else {
                        this.video_c.pix_fmt(12);
                    }
                } else {
                    this.video_c.pix_fmt(avutil.AV_PIX_FMT_RGB32);
                }

                if (this.video_c.codec_id() == 2) {
                    this.video_c.max_b_frames(2);
                } else if (this.video_c.codec_id() == 1) {
                    this.video_c.mb_decision(2);
                } else if (this.video_c.codec_id() == 4) {
                    if (this.imageWidth <= 128 && this.imageHeight <= 96) {
                        this.video_c.width(128).height(96);
                    } else if (this.imageWidth <= 176 && this.imageHeight <= 144) {
                        this.video_c.width(176).height(144);
                    } else if (this.imageWidth <= 352 && this.imageHeight <= 288) {
                        this.video_c.width(352).height(288);
                    } else if (this.imageWidth <= 704 && this.imageHeight <= 576) {
                        this.video_c.width(704).height(576);
                    } else {
                        this.video_c.width(1408).height(1152);
                    }
                } else if (this.video_c.codec_id() == 27) {
                    this.video_c.profile(578);
                }

                if ((this.oformat.flags() & 64) != 0) {
                    this.video_c.flags(this.video_c.flags() | 4194304);
                }

                if ((this.video_codec.capabilities() & 512) != 0) {
                    this.video_c.strict_std_compliance(-2);
                }

                if (this.maxBFrames >= 0) {
                    this.video_c.max_b_frames(this.maxBFrames);
                    this.video_c.has_b_frames(this.maxBFrames == 0 ? 0 : 1);
                }

                if (this.trellis >= 0) {
                    this.video_c.trellis(this.trellis);
                }
            }

            //音频编码
            int i;
            if (this.audioChannels > 0 && this.audioBitrate > 0 && this.sampleRate > 0) {
                if (this.audioCodec != 0) {
                    this.oformat.audio_codec(this.audioCodec);
                } else if (!"flv".equals(format_name) && !"mp4".equals(format_name) && !"3gp".equals(format_name)) {
                    if ("avi".equals(format_name)) {
                        this.oformat.audio_codec(65536);
                    }
                } else {
                    this.oformat.audio_codec(86018);
                }

                if ((this.audio_codec = avcodec.avcodec_find_encoder_by_name(this.audioCodecName)) == null
                                && (this.audio_codec = avcodec.avcodec_find_encoder(this.oformat.audio_codec())) == null) {
                    this.release();
                    throw new Exception("avcodec_find_encoder() error: Audio codec not found.");
                }

                this.oformat.audio_codec(this.audio_codec.id());
                if ((this.audio_st = avformat.avformat_new_stream(this.oc, (AVCodec) null)) == null) {
                    this.release();
                    throw new Exception("avformat_new_stream() error: Could not allocate audio stream.");
                }

                if ((this.audio_c = avcodec.avcodec_alloc_context3(this.audio_codec)) == null) {
                    this.release();
                    throw new Exception("avcodec_alloc_context3() error: Could not allocate audio encoding context.");
                }

                if (inpAudioStream != null && this.audioChannels > 0) {
                    if (avcodec.avcodec_copy_context(this.audio_st.codec(), inpAudioStream.codec()) < 0) {
                        throw new Exception("avcodec_copy_context() error:\tFailed to copy context from input audio to output audio stream codec context\n");
                    }

                    this.audioBitrate = (int) inpAudioStream.codec().bit_rate();
                    this.sampleRate = inpAudioStream.codec().sample_rate();
                    this.audioChannels = inpAudioStream.codec().channels();
                    this.sampleFormat = inpAudioStream.codec().sample_fmt();
                    this.audioQuality = (double) inpAudioStream.codec().global_quality();
                    this.audio_c.codec_tag(0);
                    this.audio_st.duration(inpAudioStream.duration());
                    this.audio_st.time_base().num(inpAudioStream.time_base().num());
                    this.audio_st.time_base().den(inpAudioStream.time_base().den());
                }

                this.audio_c.codec_id(this.oformat.audio_codec());
                this.audio_c.codec_type(1);
                this.audio_c.bit_rate((long) this.audioBitrate);
                this.audio_c.sample_rate(this.sampleRate);
                this.audio_c.channels(this.audioChannels);
                this.audio_c.channel_layout(avutil.av_get_default_channel_layout(this.audioChannels));
                if (this.sampleFormat != -1) {
                    this.audio_c.sample_fmt(this.sampleFormat);
                } else {
                    this.audio_c.sample_fmt(8);
                    IntPointer formats = this.audio_c.codec().sample_fmts();

                    for (i = 0; formats.get((long) i) != -1; ++i) {
                        if (formats.get((long) i) == 1) {
                            this.audio_c.sample_fmt(1);
                            break;
                        }
                    }
                }

                this.audio_c.time_base().num(1).den(this.sampleRate);
                this.audio_st.time_base().num(1).den(this.sampleRate);
                switch (this.audio_c.sample_fmt()) {
                    case 0:
                    case 5:
                        this.audio_c.bits_per_raw_sample(8);
                        break;
                    case 1:
                    case 6:
                        this.audio_c.bits_per_raw_sample(16);
                        break;
                    case 2:
                    case 7:
                        this.audio_c.bits_per_raw_sample(32);
                        break;
                    case 3:
                    case 8:
                        this.audio_c.bits_per_raw_sample(32);
                        break;
                    case 4:
                    case 9:
                        this.audio_c.bits_per_raw_sample(64);
                        break;
                    default:
                        assert false;
                }

                if (this.audioQuality >= 0.0D) {
                    this.audio_c.flags(this.audio_c.flags() | 2);
                    this.audio_c.global_quality((int) Math.round(118.0D * this.audioQuality));
                }

                if ((this.oformat.flags() & 64) != 0) {
                    this.audio_c.flags(this.audio_c.flags() | 4194304);
                }

                if ((this.audio_codec.capabilities() & 512) != 0) {
                    this.audio_c.strict_std_compliance(-2);
                }
            }

            int ret;
            AVDictionary options;
            Iterator var17;
            Entry e;
            //通过无参start方法调用的情况 设置AVDictionary参数
            if (this.video_st != null && inpVideoStream == null) {
                options = new AVDictionary((Pointer) null);
                if (this.videoQuality >= 0.0D) {
                    avutil.av_dict_set(options, "crf", "" + this.videoQuality, 0);
                }

                var17 = this.videoOptions.entrySet().iterator();

                while (var17.hasNext()) {
                    e = (Entry) var17.next();
                    avutil.av_dict_set(options, (String) e.getKey(), (String) e.getValue(), 0);
                }

                if ((ret = avcodec.avcodec_open2(this.video_c, this.video_codec, options)) < 0) {
                    this.release();
                    avutil.av_dict_free(options);
                    throw new Exception("avcodec_open2() error " + ret + ": Could not open video codec.");
                }

                avutil.av_dict_free(options);
                this.video_outbuf = null;
                if ((this.picture = avutil.av_frame_alloc()) == null) {
                    this.release();
                    throw new Exception("av_frame_alloc() error: Could not allocate picture.");
                }

                this.picture.pts(0L);
                i = avutil.av_image_get_buffer_size(this.video_c.pix_fmt(), this.video_c.width(), this.video_c.height(), 1);
                if ((this.picture_buf = new BytePointer(avutil.av_malloc((long) i))).isNull()) {
                    this.release();
                    throw new Exception("av_malloc() error: Could not allocate picture buffer.");
                }

                if ((this.tmp_picture = avutil.av_frame_alloc()) == null) {
                    this.release();
                    throw new Exception("av_frame_alloc() error: Could not allocate temporary picture.");
                }

                if (avcodec.avcodec_parameters_from_context(this.video_st.codecpar(), this.video_c) < 0) {
                    this.release();
                    throw new Exception("avcodec_parameters_from_context() error: Could not copy the video stream parameters.");
                }

                AVDictionary metadata = new AVDictionary((Pointer) null);
                Iterator var8 = this.videoMetadata.entrySet().iterator();

                while (var8.hasNext()) {
                    //                    Entry<String, String>
                    e = (Entry) var8.next();
                    avutil.av_dict_set(metadata, (String) e.getKey(), (String) e.getValue(), 0);
                }

                this.video_st.metadata(metadata);
            }

            if (this.audio_st != null && inpAudioStream == null) {
                options = new AVDictionary((Pointer) null);
                if (this.audioQuality >= 0.0D) {
                    avutil.av_dict_set(options, "crf", "" + this.audioQuality, 0);
                }

                var17 = this.audioOptions.entrySet().iterator();

                while (var17.hasNext()) {
                    e = (Entry) var17.next();
                    avutil.av_dict_set(options, (String) e.getKey(), (String) e.getValue(), 0);
                }

                if ((ret = avcodec.avcodec_open2(this.audio_c, this.audio_codec, options)) < 0) {
                    this.release();
                    avutil.av_dict_free(options);
                    throw new Exception("avcodec_open2() error " + ret + ": Could not open audio codec.");
                }

                avutil.av_dict_free(options);
                this.audio_outbuf_size = 262144;
                this.audio_outbuf = new BytePointer(avutil.av_malloc((long) this.audio_outbuf_size));
                if (this.audio_c.frame_size() <= 1) {
                    this.audio_outbuf_size = 16384;
                    this.audio_input_frame_size = this.audio_outbuf_size / this.audio_c.channels();
                    switch (this.audio_c.codec_id()) {
                        case 65536:
                        case 65537:
                        case 65538:
                        case 65539:
                            this.audio_input_frame_size >>= 1;
                    }
                } else {
                    this.audio_input_frame_size = this.audio_c.frame_size();
                }

                i = avutil.av_sample_fmt_is_planar(this.audio_c.sample_fmt()) != 0 ? this.audio_c.channels() : 1;
                data_size = avutil
                                .av_samples_get_buffer_size((IntPointer) null, this.audio_c.channels(), this.audio_input_frame_size, this.audio_c.sample_fmt(),
                                                            1) / i;
                this.samples_out = new BytePointer[i];

                for (i = 0; i < this.samples_out.length; ++i) {
                    this.samples_out[i] = (new BytePointer(avutil.av_malloc((long) data_size))).capacity((long) data_size);
                }

                this.samples_in = new Pointer[8];
                this.samples_in_ptr = new PointerPointer(8L);
                this.samples_out_ptr = new PointerPointer(8L);
                if ((this.frame = avutil.av_frame_alloc()) == null) {
                    this.release();
                    throw new Exception("av_frame_alloc() error: Could not allocate audio frame.");
                }

                this.frame.pts(0L);
                if (avcodec.avcodec_parameters_from_context(this.audio_st.codecpar(), this.audio_c) < 0) {
                    this.release();
                    throw new Exception("avcodec_parameters_from_context() error: Could not copy the audio stream parameters.");
                }

                AVDictionary metadata = new AVDictionary((Pointer) null);
                Iterator var26 = this.audioMetadata.entrySet().iterator();

                while (var26.hasNext()) {
                    //                    Entry<String, String>
                    e = (Entry) var26.next();
                    avutil.av_dict_set(metadata, (String) e.getKey(), (String) e.getValue(), 0);
                }

                this.audio_st.metadata(metadata);
            }

            options = new AVDictionary((Pointer) null);
            var17 = this.options.entrySet().iterator();

            while (var17.hasNext()) {
                e = (Entry) var17.next();
                avutil.av_dict_set(options, (String) e.getKey(), (String) e.getValue(), 0);
            }

            //AVIOContext pb
            if (this.outputStream == null && (this.oformat.flags() & 1) == 0) {
                AVIOContext pb = new AVIOContext((Pointer) null);
                if ((ret = avformat.avio_open2(pb, this.filename, 2, (AVIOInterruptCB) null, options)) < 0) {
                    this.release();
                    avutil.av_dict_free(options);
                    throw new Exception("avio_open2 error() error " + ret + ": Could not open '" + this.filename + "'");
                }

                this.oc.pb(pb);
            }

            AVDictionary metadata = new AVDictionary((Pointer) null);
            Iterator var27 = this.metadata.entrySet().iterator();

            while (var27.hasNext()) {
                //                Entry<String, String>
                e = (Entry) var27.next();
                avutil.av_dict_set(metadata, (String) e.getKey(), (String) e.getValue(), 0);
            }

            avformat.avformat_write_header(this.oc.metadata(metadata), options);
            avutil.av_dict_free(options);
            if (avutil.av_log_get_level() >= 32) {
                avformat.av_dump_format(this.oc, 0, this.filename, 1);
            }

        }
    }

    @Override
    public void stop() throws Exception {
        if (this.oc != null) {
            try {
                AVFormatContext var1 = this.oc;
                synchronized (this.oc) {
                    AVPacket tmp = null;
                    while (this.video_st != null && (tmp = this.getVideoPacket(0, 0, 0, 0, 0, -1, (Buffer[]) null)) != null) {
                        this.writePacket(0, tmp);
                    }

                    while (this.audio_st != null && (tmp = this.getAudioPacket(0, 0, (Buffer[]) null)) != null) {
                        this.writePacket(1, tmp);
                    }

                    if (this.interleaved && this.video_st != null && this.audio_st != null) {
                        avformat.av_interleaved_write_frame(this.oc, (AVPacket) null);
                    } else {
                        avformat.av_write_frame(this.oc, (AVPacket) null);
                    }

                    avformat.av_write_trailer(this.oc);
                }
            } finally {
                this.release();
            }
        }
    }

    public void stopCopy() throws Exception {
        if (this.oc != null) {
            try {
                synchronized (this.oc) {
                    avformat.av_write_frame(this.oc, (AVPacket) null);
                    avformat.av_write_trailer(this.oc);
                }
            } finally {
                this.release();
            }
        }
    }

    @Override
    public void record(Frame frame) throws Exception {}

    public void writePacket(int mediaType, AVPacket avPacket) throws Exception {
        AVStream avStream = mediaType == 0 ? this.audio_st : (mediaType == 1 ? this.video_st : null);
        String mediaTypeStr = mediaType == 0 ? "video" : (mediaType == 1 ? "audio" : "unsupported media stream type");
        AVFormatContext var5 = this.oc;

        synchronized (this.oc) {
            int ret;
            if (this.interleaved && avStream != null) {
                if ((ret = avformat.av_interleaved_write_frame(this.oc, avPacket)) < 0) {
                    throw new Exception("av_interleaved_write_frame() error " + ret + " while writing interleaved " + mediaTypeStr + " packet.");
                }
            } else if ((ret = avformat.av_write_frame(this.oc, avPacket)) < 0) {
                throw new Exception("av_write_frame() error " + ret + " while writing " + mediaTypeStr + " packet.");
            }
        }
    }

    public AVPacket getVideoPacket(int width, int height, int depth, int channels, int stride, int pixelFormat, Buffer... image) throws Exception {
        if (this.video_st == null) {
            throw new Exception("No video output stream (Is imageWidth > 0 && imageHeight > 0 and has start() been called?)");
        } else {
            if (image != null && image.length != 0) {
                int step = stride * Math.abs(depth) / 8;
                BytePointer data = image[0] instanceof ByteBuffer ?
                                new BytePointer((ByteBuffer) image[0].position(0)) :
                                new BytePointer(new Pointer(image[0].position(0)));
                if (pixelFormat == -1) {
                    if ((depth == 8 || depth == -8) && channels == 3) {
                        pixelFormat = 3;
                    } else if ((depth == 8 || depth == -8) && channels == 1) {
                        pixelFormat = 8;
                    } else if ((depth == 16 || depth == -16) && channels == 1) {
                        pixelFormat = ByteOrder.nativeOrder().equals(ByteOrder.BIG_ENDIAN) ? 29 : 30;
                    } else if ((depth == 8 || depth == -8) && channels == 4) {
                        pixelFormat = 26;
                    } else {
                        if (depth != 8 && depth != -8 || channels != 2) {
                            throw new Exception("Could not guess pixel format of image : depth=" + depth + ", channels=" + channels);
                        }

                        pixelFormat = 24;
                    }
                }

                if (pixelFormat == 24) {
                    step = width;
                }

                if (this.video_c.pix_fmt() == pixelFormat && this.video_c.width() == width && this.video_c.height() == height) {
                    avutil.av_image_fill_arrays(new PointerPointer(this.picture), this.picture.linesize(), data, pixelFormat, width, height, 1);
                    this.picture.linesize(0, step);
                    this.picture.format(pixelFormat);
                    this.picture.height(height);
                    this.picture.width(width);
                } else {
                    this.img_convert_ctx = swscale
                                    .sws_getCachedContext(this.img_convert_ctx, width, height, pixelFormat, this.video_c.width(), this.video_c.height(),
                                                          this.video_c.pix_fmt(), this.imageScalingFlags != 0 ? this.imageScalingFlags : 2, null, null,
                                                          (DoublePointer) null);
                    if (this.img_convert_ctx == null) {
                        throw new Exception("sws_getCachedContext() error: Cannot initialize the conversion context.");
                    }

                    avutil.av_image_fill_arrays(new PointerPointer(this.tmp_picture), this.tmp_picture.linesize(), data, pixelFormat, width, height, 1);
                    avutil.av_image_fill_arrays(new PointerPointer(this.picture), this.picture.linesize(), this.picture_buf, this.video_c.pix_fmt(),
                                                this.video_c.width(), this.video_c.height(), 1);
                    this.tmp_picture.linesize(0, step);
                    this.tmp_picture.format(pixelFormat);
                    this.tmp_picture.width(width);
                    this.tmp_picture.height(height);
                    this.picture.format(this.video_c.pix_fmt());
                    this.picture.width(this.video_c.width());
                    this.picture.height(this.video_c.height());
                    swscale.sws_scale(this.img_convert_ctx, new PointerPointer(this.tmp_picture), this.tmp_picture.linesize(), 0, height,
                                      new PointerPointer(this.picture), this.picture.linesize());
                }
            }

            avcodec.av_init_packet(this.video_pkt);
            this.video_pkt.data(this.video_outbuf);
            this.video_pkt.size(this.video_outbuf_size);
            this.picture.quality(this.video_c.global_quality());
            int ret;
            if ((ret = avcodec.avcodec_encode_video2(this.video_c, this.video_pkt, image != null && image.length != 0 ? this.picture : null,
                                                     this.got_video_packet)) < 0) {
                throw new Exception("avcodec_encode_video2() error " + ret + ": Could not encode video packet.");
            } else {
                this.picture.pts(this.picture.pts() + 1L);
                if (this.got_video_packet[0] != 0) {
                    if (this.video_pkt.pts() != avutil.AV_NOPTS_VALUE) {
                        this.video_pkt.pts(avutil.av_rescale_q(this.video_pkt.pts(), this.video_c.time_base(), this.video_st.time_base()));
                    }

                    if (this.video_pkt.dts() != avutil.AV_NOPTS_VALUE) {
                        this.video_pkt.dts(avutil.av_rescale_q(this.video_pkt.dts(), this.video_c.time_base(), this.video_st.time_base()));
                    }

                    this.video_pkt.stream_index(this.video_st.index());

                    return this.video_pkt;
                } else {
                    return null;
                }
            }
        }
    }

    public AVPacket getAudioPacket(int sampleRate, int audioChannels, Buffer... samples) throws Exception {
        if (this.audio_st == null) {
            throw new Exception("No audio output stream (Is audioChannels > 0 and has start() been called?)");
        } else if (samples == null && this.samples_out[0].position() > 0L) {
            double sampleDivisor = Math.floor((double) ((int) Math.min(this.samples_out[0].limit(), 2147483647L) / this.audio_input_frame_size));
            this.MyWriteSamples((int) Math.floor((double) ((int) this.samples_out[0].position()) / sampleDivisor));
            return this.MyRecord((AVFrame) null);
        } else {
            if (sampleRate <= 0) {
                sampleRate = this.audio_c.sample_rate();
            }

            if (audioChannels <= 0) {
                audioChannels = this.audio_c.channels();
            }

            int inputSize = samples != null ? samples[0].limit() - samples[0].position() : 0;
            int inputFormat = this.samples_format;
            int inputChannels = samples != null && samples.length > 1 ? 1 : audioChannels;
            int inputDepth = 0;
            int outputFormat = this.audio_c.sample_fmt();
            int outputChannels = this.samples_out.length > 1 ? 1 : this.audio_c.channels();
            int outputDepth = avutil.av_get_bytes_per_sample(outputFormat);
            int inputCount;
            if (samples != null && samples[0] instanceof ByteBuffer) {
                inputFormat = samples.length > 1 ? 5 : 0;
                inputDepth = 1;

                for (inputCount = 0; inputCount < samples.length; ++inputCount) {
                    ByteBuffer b = (ByteBuffer) samples[inputCount];
                    if (this.samples_in[inputCount] instanceof BytePointer && this.samples_in[inputCount].capacity() >= (long) inputSize && b.hasArray()) {
                        ((BytePointer) this.samples_in[inputCount]).position(0L).put(b.array(), b.position(), inputSize);
                    } else {
                        this.samples_in[inputCount] = new BytePointer(b);
                    }
                }
            } else if (samples != null && samples[0] instanceof ShortBuffer) {
                inputFormat = samples.length > 1 ? 6 : 1;
                inputDepth = 2;

                for (inputCount = 0; inputCount < samples.length; ++inputCount) {
                    ShortBuffer b = (ShortBuffer) samples[inputCount];
                    if (this.samples_in[inputCount] instanceof ShortPointer && this.samples_in[inputCount].capacity() >= (long) inputSize && b.hasArray()) {
                        ((ShortPointer) this.samples_in[inputCount]).position(0L).put(b.array(), samples[inputCount].position(), inputSize);
                    } else {
                        this.samples_in[inputCount] = new ShortPointer(b);
                    }
                }
            } else if (samples != null && samples[0] instanceof IntBuffer) {
                inputFormat = samples.length > 1 ? 7 : 2;
                inputDepth = 4;

                for (inputCount = 0; inputCount < samples.length; ++inputCount) {
                    IntBuffer b = (IntBuffer) samples[inputCount];
                    if (this.samples_in[inputCount] instanceof IntPointer && this.samples_in[inputCount].capacity() >= (long) inputSize && b.hasArray()) {
                        ((IntPointer) this.samples_in[inputCount]).position(0L).put(b.array(), samples[inputCount].position(), inputSize);
                    } else {
                        this.samples_in[inputCount] = new IntPointer(b);
                    }
                }
            } else if (samples != null && samples[0] instanceof FloatBuffer) {
                inputFormat = samples.length > 1 ? 8 : 3;
                inputDepth = 4;

                for (inputCount = 0; inputCount < samples.length; ++inputCount) {
                    FloatBuffer b = (FloatBuffer) samples[inputCount];
                    if (this.samples_in[inputCount] instanceof FloatPointer && this.samples_in[inputCount].capacity() >= (long) inputSize && b.hasArray()) {
                        ((FloatPointer) this.samples_in[inputCount]).position(0L).put(b.array(), b.position(), inputSize);
                    } else {
                        this.samples_in[inputCount] = new FloatPointer(b);
                    }
                }
            } else if (samples != null && samples[0] instanceof DoubleBuffer) {
                inputFormat = samples.length > 1 ? 9 : 4;
                inputDepth = 8;

                for (inputCount = 0; inputCount < samples.length; ++inputCount) {
                    DoubleBuffer b = (DoubleBuffer) samples[inputCount];
                    if (this.samples_in[inputCount] instanceof DoublePointer && this.samples_in[inputCount].capacity() >= (long) inputSize && b.hasArray()) {
                        ((DoublePointer) this.samples_in[inputCount]).position(0L).put(b.array(), b.position(), inputSize);
                    } else {
                        this.samples_in[inputCount] = new DoublePointer(b);
                    }
                }
            } else if (samples != null) {
                throw new Exception("Audio samples Buffer has unsupported type: " + samples);
            }

            int ret;
            if (this.samples_convert_ctx == null || this.samples_channels != audioChannels || this.samples_format != inputFormat
                            || this.samples_rate != sampleRate) {
                this.samples_convert_ctx = swresample
                                .swr_alloc_set_opts(this.samples_convert_ctx, this.audio_c.channel_layout(), outputFormat, this.audio_c.sample_rate(),
                                                    avutil.av_get_default_channel_layout(audioChannels), inputFormat, sampleRate, 0, (Pointer) null);
                if (this.samples_convert_ctx == null) {
                    throw new Exception("swr_alloc_set_opts() error: Cannot allocate the conversion context.");
                }

                if ((ret = swresample.swr_init(this.samples_convert_ctx)) < 0) {
                    throw new Exception("swr_init() error " + ret + ": Cannot initialize the conversion context.");
                }

                this.samples_channels = audioChannels;
                this.samples_format = inputFormat;
                this.samples_rate = sampleRate;
            }

            for (inputCount = 0; samples != null && inputCount < samples.length; ++inputCount) {
                this.samples_in[inputCount].position(this.samples_in[inputCount].position() * (long) inputDepth)
                                .limit((this.samples_in[inputCount].position() + (long) inputSize) * (long) inputDepth);
            }

            while (true) {
                do {
                    inputCount = (int) Math.min(samples != null ?
                                                                (this.samples_in[0].limit() - this.samples_in[0].position()) / (long) (inputChannels
                                                                                * inputDepth) :
                                                                0L, 2147483647L);
                    int outputCount = (int) Math
                                    .min((this.samples_out[0].limit() - this.samples_out[0].position()) / (long) (outputChannels * outputDepth), 2147483647L);
                    inputCount = Math.min(inputCount, (outputCount * sampleRate + this.audio_c.sample_rate() - 1) / this.audio_c.sample_rate());

                    int i;
                    for (i = 0; samples != null && i < samples.length; ++i) {
                        this.samples_in_ptr.put((long) i, this.samples_in[i]);
                    }

                    for (i = 0; i < this.samples_out.length; ++i) {
                        this.samples_out_ptr.put((long) i, this.samples_out[i]);
                    }

                    if ((ret = swresample.swr_convert(this.samples_convert_ctx, this.samples_out_ptr, outputCount, this.samples_in_ptr, inputCount)) < 0) {
                        throw new Exception("swr_convert() error " + ret + ": Cannot convert audio samples.");
                    }

                    if (ret == 0) {
                        return null;
                    }

                    for (i = 0; samples != null && i < samples.length; ++i) {
                        this.samples_in[i].position(this.samples_in[i].position() + (long) (inputCount * inputChannels * inputDepth));
                    }

                    for (i = 0; i < this.samples_out.length; ++i) {
                        this.samples_out[i].position(this.samples_out[i].position() + (long) (ret * outputChannels * outputDepth));
                    }
                } while (samples != null && this.samples_out[0].position() < this.samples_out[0].limit());

                return this.MyWriteSamples(this.audio_input_frame_size);
            }
        }
    }

    private AVPacket MyWriteSamples(int nb_samples) throws Exception {
        if (this.samples_out != null && this.samples_out.length != 0) {
            this.frame.nb_samples(nb_samples);
            avcodec.avcodec_fill_audio_frame(this.frame, this.audio_c.channels(), this.audio_c.sample_fmt(), this.samples_out[0],
                                             (int) this.samples_out[0].position(), 0);

            for (int i = 0; i < this.samples_out.length; ++i) {
                //                int linesize = false;
                int linesize;
                if (this.samples_out[0].position() > 0L && this.samples_out[0].position() < this.samples_out[0].limit()) {
                    linesize = (int) this.samples_out[i].position();
                } else {
                    linesize = (int) Math.min(this.samples_out[i].limit(), 2147483647L);
                }

                this.frame.data(i, this.samples_out[i].position(0L));
                this.frame.linesize(i, linesize);
            }

            this.frame.quality(this.audio_c.global_quality());
            return this.MyRecord(this.frame);
        }
        return null;
    }

    AVPacket MyRecord(AVFrame frame) throws Exception {
        avcodec.av_init_packet(this.audio_pkt);
        this.audio_pkt.data(this.audio_outbuf);
        this.audio_pkt.size(this.audio_outbuf_size);
        int ret;
        if ((ret = avcodec.avcodec_encode_audio2(this.audio_c, this.audio_pkt, frame, this.got_audio_packet)) < 0) {
            throw new Exception("avcodec_encode_audio2() error " + ret + ": Could not encode audio packet.");
        } else {
            if (frame != null) {
                frame.pts(frame.pts() + (long) frame.nb_samples());
            }

            if (this.got_audio_packet[0] != 0) {
                if (this.audio_pkt.pts() != avutil.AV_NOPTS_VALUE) {
                    this.audio_pkt.pts(avutil.av_rescale_q(this.audio_pkt.pts(), this.audio_c.time_base(), this.audio_st.time_base()));
                }

                if (this.audio_pkt.dts() != avutil.AV_NOPTS_VALUE) {
                    this.audio_pkt.dts(avutil.av_rescale_q(this.audio_pkt.dts(), this.audio_c.time_base(), this.audio_st.time_base()));
                }

                this.audio_pkt.flags(this.audio_pkt.flags() | 1);
                this.audio_pkt.stream_index(this.audio_st.index());

                return this.audio_pkt;
            } else {
                return null;
            }
        }
    }

    static {
        try {
            tryLoad();
            FFmpegLockCallback.init();
        } catch (Exception var1) {
            var1.printStackTrace();
        }

        outputStreams = Collections.synchronizedMap(new HashMap());
        writeCallback = new MyFFmpegFrameRecorder.WriteCallback();
        PointerScope s = PointerScope.getInnerScope();
        if (s != null) {
            s.detach(writeCallback);
        }

    }

    static class WriteCallback extends Write_packet_Pointer_BytePointer_int {
        WriteCallback() {
        }

        @Override
        public int call(Pointer opaque, BytePointer buf, int buf_size) {
            try {
                byte[] b = new byte[buf_size];
                OutputStream os = (OutputStream) MyFFmpegFrameRecorder.outputStreams.get(opaque);
                buf.get(b, 0, buf_size);
                os.write(b, 0, buf_size);
                return buf_size;
            } catch (Throwable var6) {
                System.err.println("Error on OutputStream.write(): " + var6);
                return -1;
            }
        }
    }
}
