import org.bytedeco.javacpp.avcodec;
import org.bytedeco.javacv.CanvasFrame;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.FFmpegFrameRecorder;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.OpenCVFrameGrabber;
import org.bytedeco.javacpp.avutil;
import org.bytedeco.javacpp.opencv_core.IplImage;

/**
 * @author baojikui (bjklwr@outlook.com)
 * @date 2019/01/11
 */
public class JavacvTest {

    public static final String FILENAME = "/Users/baojikui/Desktop/output.mp4";

    public static void main(String[] args) throws Exception {
        FFmpegFrameGrabber grabber = new FFmpegFrameGrabber("1");
        grabber.setFormat("avfoundation");
        grabber.setImageWidth(1280);
        grabber.setImageHeight(720);
        grabber.start();
        Frame grabbedImage = grabber.grab();

        CanvasFrame canvasFrame = new CanvasFrame("Cam");
        canvasFrame.setCanvasSize(grabbedImage.imageWidth, grabbedImage.imageHeight);

        System.out.println("framerate = " + grabber.getFrameRate());
        grabber.setFrameRate(grabber.getFrameRate());
        FFmpegFrameRecorder recorder = new FFmpegFrameRecorder(FILENAME, grabber.getImageWidth(), grabber.getImageHeight());

        recorder.setVideoCodec(13);
        recorder.setFormat("mp4");
        recorder.setPixelFormat(avutil.AV_PIX_FMT_YUV420P);
        recorder.setFrameRate(30);
        recorder.setVideoBitrate(10 * 1024 * 1024);

//        MyFFmpegFrameRecorder recorder = new MyFFmpegFrameRecorder("rtmp://pili-publish.daishuclass.cn/daishu-video/test_1234", grabber.getImageWidth(), grabber.getImageHeight());
//        recorder.setInterleaved(true);
//        recorder.setVideoOption("tune", "zerolatency");
//        recorder.setVideoOption("preset", "ultrafast");
//        recorder.setVideoOption("crf", "28");
//        recorder.setVideoBitrate(2000000);
//        recorder.setVideoCodec(avcodec.AV_CODEC_ID_H264);
//        recorder.setFormat("flv");
//        recorder.setFrameRate(30);

        recorder.start();
        while (canvasFrame.isVisible() && (grabbedImage = grabber.grab()) != null) {
            canvasFrame.showImage(grabbedImage);
            recorder.record(grabbedImage);
        }
        recorder.stop();
        grabber.stop();
        canvasFrame.dispose();
    }
}
