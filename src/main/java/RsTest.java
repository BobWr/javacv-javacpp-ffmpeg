import org.bytedeco.javacpp.*;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import static org.bytedeco.javacpp.avcodec.*;
import static org.bytedeco.javacpp.avformat.*;
import static org.bytedeco.javacpp.avutil.*;
import static org.bytedeco.javacpp.swscale.*;
import static org.bytedeco.javacpp.avdevice.*;

/**
 * @author baojikui (bjklwr@outlook.com)
 * @date 2019/01/09
 */
public class RsTest {

    public static void main(String[] args) throws IOException {

        AVFormatContext avFormatContext;
        AVCodecContext avCodecContext;
        AVCodec avCodec;

        //初始化
        avformat_network_init();

        avFormatContext = avformat_alloc_context();

        //注册
        avdevice_register_all();

        //device参数
        //        AVDictionary avDictionary = new AVDictionary();
        //        av_dict_set(avDictionary, "framerate", "30", 0);

        show_avfoundation_device();

        //Mac OS
        AVInputFormat avInputFormat = av_find_input_format("avfoundation");
        if (avformat_open_input(avFormatContext, "1", avInputFormat, null) != 0) {
            System.out.println("Couldn't open input stream.");
            return;
        }

        if (avformat_find_stream_info(avFormatContext, (PointerPointer) null) < 0) {
            System.out.println("Couldn't find stream info.");
            return;
        }

        //找到一个视频流
        int videoIndex = -1;
        for (int i = 0; i < avFormatContext.nb_streams(); i++) {
            if (avFormatContext.streams(i).codec().codec_type() == AVMEDIA_TYPE_VIDEO) {
                videoIndex = i;
                break;
            }
        }
        if (videoIndex == -1) {
            System.out.println("Couldn't find a video stream.");
            return;
        }

        //解析视频编码
        avCodecContext = avFormatContext.streams(videoIndex).codec();
        avCodec = avcodec_find_decoder(avCodecContext.codec_id());

        if (avCodec == null) {
            System.out.println("codec not found.");
            return;
        }
        if (avcodec_open2(avCodecContext, avCodec, (PointerPointer) null) < 0) {
            System.out.println("Couldn't open codec.");
            return;
        }

        System.out.println("Step 1: Get video stream info success.");

        AVFrame frm = av_frame_alloc();

        // Allocate an AVFrame structure
        AVFrame pFrameRGB = av_frame_alloc();
        if (pFrameRGB == null) {
            System.exit(-1);
        }

        // Determine required buffer size and allocate buffer
        int numBytes = av_image_get_buffer_size(AV_PIX_FMT_RGB24, avCodecContext.width(), avCodecContext.height(), 1);
        BytePointer buffer = new BytePointer(av_malloc(numBytes));

        SwsContext sws_ctx = sws_getContext(avCodecContext.width(), avCodecContext.height(), avCodecContext.pix_fmt(), avCodecContext.width(),
                                            avCodecContext.height(), AV_PIX_FMT_RGB24, SWS_BILINEAR, null, null, (DoublePointer) null);

        if (sws_ctx == null) {
            System.out.println("Can not use sws");
            throw new IllegalStateException();
        }

        av_image_fill_arrays(pFrameRGB.data(), pFrameRGB.linesize(), buffer, AV_PIX_FMT_RGB24, avCodecContext.width(), avCodecContext.height(), 1);

        int i = 0;
        int ret1 = -1, ret2 = -1, fi = -1;
        AVPacket pkt = new AVPacket();
        while (av_read_frame(avFormatContext, pkt) >= 0) {
            if (pkt.stream_index() == videoIndex) {
                ret1 = avcodec_send_packet(avCodecContext, pkt);
                ret2 = avcodec_receive_frame(avCodecContext, frm);
                System.out.printf("ret1 %d ret2 %d\n", ret1, ret2);
            }
            if (ret2 >= 0 && ++i <= 5) {
                sws_scale(sws_ctx, frm.data(), frm.linesize(), 0, avCodecContext.height(), pFrameRGB.data(), pFrameRGB.linesize());

                save_frame(pFrameRGB, avCodecContext.width(), avCodecContext.height(), i);
            }
            av_packet_unref(pkt);
            if (i >= 5) {
                break;
            }
        }

        av_frame_free(frm);

        avcodec_close(avCodecContext);
        avcodec_free_context(avCodecContext);

        avformat_close_input(avFormatContext);
        System.out.println("Shutdown");
        System.exit(0);

        //创建输出流
        //        String outFile = "/Users/baojikui/Desktop/ttttt.mkv";
        //
        //        AVFormatContext outFormatContext = new AVFormatContext();
        //        int value = 0;
        //        avformat_alloc_output_context2(outFormatContext, null, null, outFile);
        //        if (outFormatContext.isNull()) {
        //            System.out.println("Couldn't allocate outFormatContext.");
        //            return;
        //        }
        //
        //        AVOutputFormat outputFormat = av_guess_format(null, outFile, null);
        //        if (outputFormat.isNull()) {
        //            System.out.println("Couldn't guess video format.");
        //            return;
        //        }
        //
        //        AVStream outStream = avformat_new_stream(outFormatContext, null);
        //        if (outStream.isNull()) {
        //            System.out.println("Couldn't create a new av format stream.");
        //            return;
        //        }
        //
        //        AVCodec outCodec = avcodec_find_encoder(AV_CODEC_ID_MPEG4);
        //        if (outCodec.isNull()) {
        //            System.out.println("Couldn't find thsis codec.");
        //            return;
        //        }
        //
        //        AVCodecContext outCodecContent = avcodec_alloc_context3(outCodec);
        //        if (outCodecContent.isNull()) {
        //            System.out.println("Couldn't allocate outCodecContent.");
        //            return;
        //        }
        //
        //        // AV_CODEC_ID_MPEG4 AV_CODEC_ID_H264
        //        outStream.codec(outCodecContent);
        //        outCodecContent.codec_id(AV_CODEC_ID_MPEG4);
        //        outCodecContent.codec_type(AVMEDIA_TYPE_VIDEO);
        //        outCodecContent.pix_fmt(AV_PIX_FMT_YUV420P);
        //        outCodecContent.bit_rate(400000);
        //        outCodecContent.width(1280);
        //        outCodecContent.height(720);
        //        outCodecContent.gop_size(3);
        //        outCodecContent.max_b_frames(2);
        //        outCodecContent.time_base().num(1);
        //        outCodecContent.time_base().den(30);
        //
        //        if (outCodecContent.codec_id() == AV_CODEC_ID_H264) {
        //            av_opt_set(outCodecContent.priv_data(), "preset", "slow", 0);
        //        }
        //
        //        // 某些容器格式（如MP4）需要存在全局标头标记编码器以使其相应地运行
        //        if ((outCodecContent.flags() & AVFMT_GLOBALHEADER) != 0) {
        //            outCodecContent.flags(outCodecContent.flags() | AV_CODEC_FLAG_GLOBAL_HEADER);
        //        }
        //
        //        value = avcodec_open2(outCodecContent, outCodec, (PointerPointer) null);
        //        if (value < 0) {
        //            System.out.println("Couldn't open the avCodec.");
        //            return;
        //        }
        //
        //        AVIOContext avioContext = new AVIOContext();
        //        outFormatContext.pb(avioContext);
        //        if ((outFormatContext.flags() & AVFMT_NOFILE) == 0) {
        //            // 坑           if (avio_open2(outFormatContext.pb(), outFile, AVIO_FLAG_WRITE, null, null) < 0) {
        //            if (avio_open2(outFormatContext.pb(), outFile, AVIO_FLAG_WRITE, null, null) < 0) {
        //                System.out.println("Couldn't create outFile.");
        //                return;
        //            }
        //        }
        //
        //        if (outFormatContext.nb_streams() <= 0) {
        //            System.out.println("outFormatContext has no stream.");
        //            return;
        //        }
        //
        //        AVDictionary avDictionary = new AVDictionary();
        //        value = avformat_write_header(outFormatContext, (AVDictionary) null);
        //        if (value < 0) {
        //            System.out.println("Couldn't write the header context.");
        //            return;
        //        }
        //
        //        //record
        //        AVPacket avPacket = new AVPacket();
        //        av_init_packet(avPacket);
        //
        //        AVFrame avFrame = av_frame_alloc();
        //        AVFrame outFrame = av_frame_alloc();
        //
        //        int nbytes = av_image_get_buffer_size(outCodecContent.pix_fmt(), outCodecContent.width(), outCodecContent.height(), 1);
        //        BytePointer outPointer = new BytePointer(av_malloc(nbytes));
        //        value = av_image_fill_arrays(outFrame.data(), outFrame.linesize(), outPointer, AV_PIX_FMT_YUV420P, outCodecContent.width(), outCodecContent
        // .height(),
        //                                     1);
        //        if (value < 0) {
        //            System.out.println("Couldn't fill image array.");
        //            return;
        //        }
        //
        //        SwsContext swsCtx = sws_getContext(avCodecContext.width(), avCodecContext.height(), avCodecContext.pix_fmt(), outCodecContent.width(),
        //                                           outCodecContent.height(), outCodecContent.pix_fmt(), SWS_BICUBIC, null, null, (DoublePointer) null);
        //
        //        AVPacket outPacket = new AVPacket();
        //        int frames = 100;
        //        int i = 0, j = 0;
        //        int gotPicture = -1;
        //        Integer ret = -1;
        //
        //        while (av_read_frame(avFormatContext, avPacket) >= 0 && i++ < 100) {
        //            if (avPacket.stream_index() == videoIndex) {
        //                //                value = avcodec_decode_video2(avCodecContext, avFrame, finished, avPacket);
        //                avcodec_receive_packet(avCodecContext, avPacket);
        //                ret = avcodec_receive_frame(avCodecContext, avFrame);
        //            }
        //            if (ret < 0) {
        //                System.out.println("ret < 0.");
        //                return;
        //            }
        //            sws_scale(swsCtx, avFrame.data(), avFrame.linesize(), 0, avCodecContext.height(), outFrame.data(), outFrame.linesize());
        //            av_init_packet(outPacket);
        //            outPacket.data(null);
        //            outPacket.size(0);
        //            //            avcodec_encode_video2(outCodecContent ,outPacket ,outFrame ,gotPicture);
        //            avcodec_send_packet(outCodecContent, outPacket);
        //            gotPicture = avcodec_send_frame(outCodecContent, outFrame);
        //            if (gotPicture >= 0) {
        //
        //                //                if (outPacket.pts() != AV_NOPTS_VALUE) {
        //                outPacket.pts(av_rescale_q(outPacket.pts(), outStream.time_base(), outStream.time_base()));
        //                //                }
        //                //                if (outPacket.dts() != AV_NOPTS_VALUE) {
        //                outPacket.dts(av_rescale_q(outPacket.dts(), outStream.time_base(), outStream.time_base()));
        //                //                }
        //                System.out.println("Write frame " + j++ + ". size= " + outPacket.size() / 1000);
        //                if (av_write_frame(outFormatContext, outPacket) != 0) {
        //                    System.out.println("Error occurs when write video frame.");
        //                }
        //                av_packet_unref(outPacket);
        //            }
        //            av_packet_unref(outPacket);
        //        }

    }

    static void show_avfoundation_device() {
        AVFormatContext avFormatContext = avformat_alloc_context();
        AVDictionary avDictionary = new AVDictionary();
        av_dict_set(avDictionary, "list_devices", "true", 0);
        AVInputFormat avInputFormat = av_find_input_format("avfoundation");

        System.out.println("==AVFoundation Device Info===");
        avformat_open_input(avFormatContext, "", avInputFormat, avDictionary);
        System.out.println("=============================");
    }

    static void save_frame(AVFrame pFrame, int width, int height, int f_idx) throws IOException {
        // Open file
        String szFilename = String.format("frame%d_.ppm", f_idx);
        OutputStream pFile = new FileOutputStream(szFilename);

        // Write header
        pFile.write(String.format("P6\n%d %d\n255\n", width, height).getBytes());

        // Write pixel data
        BytePointer data = pFrame.data(0);
        byte[] bytes = new byte[width * 3];
        int l = pFrame.linesize(0);
        for (int y = 0; y < height; y++) {
            data.position(y * l).get(bytes);
            pFile.write(bytes);
        }

        // Close file
        pFile.close();
    }
}
