import org.bytedeco.javacpp.avcodec;
import org.bytedeco.javacv.FFmpegFrameRecorder;
import org.bytedeco.javacv.OpenCVFrameConverter;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Date;

import static org.bytedeco.javacpp.opencv_imgcodecs.cvLoadImage;

/**
 * @author baojikui (bjklwr@outlook.com)
 * @date 2019/01/11
 */
public class ScreenRecorder {
    public static boolean videoComplete = false;
    public static String inputImageDir = "/Users/baojikui/Desktop" + File.separator;
    public static String inputImgExt = "png";
    public static String outputVideo = "recording.mp4";
    public static int counter = 0;
    public static int imgProcessed = 0;
    public static FFmpegFrameRecorder recorder = null;
    public static int videoWidth = 1920;
    public static int videoHeight = 1080;
    public static int videoFrameRate = 3;
    public static int videoQuality = 0;
    public static int videoBitRate = 9000;
    public static String videoFormat = "mp4";
    public static int videoCodec = avcodec.AV_CODEC_ID_MPEG4;
    public static Thread t1 = null;
    public static Thread t2 = null;
    public static boolean isRegionSelected = false;
    public static int c1 = 0;
    public static int c2 = 0;
    public static int c3 = 0;
    public static int c4 = 0;

    public static void main(String[] args) {

        try {
            if (getRecorder() == null) {
                System.out.println("Cannot make recorder object, Exiting program");
                System.exit(0);
            }
            if (getRobot() == null) {
                System.out.println("Cannot make robot object, Exiting program");
                System.exit(0);
            }
            File scanFolder = new File(inputImageDir);
            scanFolder.delete();
            scanFolder.mkdirs();

            startRecording();

            Thread.sleep(10000);

            stopRecording();
        } catch (Exception e) {
            System.out.println("Exception in program " + e.getMessage());
        }
    }

    public static void startRecording() {
        t1 = new Thread() {
            @Override
            public void run() {
                try {
                    takeScreenshot(getRobot());
                } catch (Exception e) {
                    System.out.println("Cannot make robot object, Exiting program " + e.getMessage());
                    System.exit(0);
                }
            }
        };

        t2 = new Thread() {
            @Override
            public void run() {
                prepareVideo();
            }
        };

        t1.start();
        t2.start();
        System.out.println("Started recording at " + new Date());
    }

    public static Robot getRobot() throws Exception {
        Robot r = null;
        try {
            r = new Robot();
            return r;
        } catch (AWTException e) {
            System.out.println("Issue while initiating Robot object " + e.getMessage());
            throw new Exception("Issue while initiating Robot object");
        }
    }

    public static void takeScreenshot(Robot r) {
        Dimension size = Toolkit.getDefaultToolkit().getScreenSize();
        Rectangle rec = new Rectangle(size);
        if (isRegionSelected) {
            rec = new Rectangle(c1, c2, c3 - c1, c4 - c2);
        }
        while (!videoComplete) {
            counter++;
            BufferedImage img = r.createScreenCapture(rec);
            try {
                ImageIO.write(img, inputImgExt, new File(inputImageDir + counter + "." + inputImgExt));
            } catch (IOException e) {
                System.out.println("Got an issue while writing the screenshot to disk " + e.getMessage());
                counter--;
            }
        }
    }

    public static void prepareVideo() {
        File scanFolder = new File(inputImageDir);
        while (!videoComplete) {
            File[] inputFiles = scanFolder.listFiles();
            try {
                getRobot().delay(500);
            } catch (Exception e) {
            }
            //for(int i=0;i<scanFolder.list().length;i++)
            for (int i = 0; i < inputFiles.length; i++) {
                //imgProcessed++;
                addImageToVideo(inputFiles[i].getAbsolutePath());
                //String imgToAdd=scanFolder.getAbsolutePath()+File.separator+imgProcessed+"."+inputImgExt;
                //addImageToVideo(imgToAdd);
                //new File(imgToAdd).delete();
                inputFiles[i].delete();
            }
        }

        File[] inputFiles = scanFolder.listFiles();
        for (int i = 0; i < inputFiles.length; i++) {
            addImageToVideo(inputFiles[i].getAbsolutePath());
            inputFiles[i].delete();
        }
    }

    public static FFmpegFrameRecorder getRecorder() throws Exception {
        if (recorder != null) {
            return recorder;
        }
        recorder = new FFmpegFrameRecorder(outputVideo, videoWidth, videoHeight);
        try {
            recorder.setFrameRate(videoFrameRate);
            recorder.setVideoCodec(videoCodec);
            recorder.setVideoBitrate(videoBitRate);
            recorder.setFormat(videoFormat);
            recorder.setVideoQuality(videoQuality);
            recorder.start();
        } catch (Exception e) {
            System.out.println("Exception while starting the recorder object " + e.getMessage());
            throw new Exception("Unable to start recorder");
        }
        return recorder;
    }

    public static OpenCVFrameConverter.ToIplImage getFrameConverter() {
        OpenCVFrameConverter.ToIplImage grabberConverter = new OpenCVFrameConverter.ToIplImage();
        return grabberConverter;
    }

    public static void addImageToVideo(String imgPath) {
        try {
            getRecorder().record(getFrameConverter().convert(cvLoadImage(imgPath)));
        } catch (Exception e) {
            System.out.println("Exception while adding image to video " + e.getMessage());
        }
    }

    public static void stopRecording() {
        try {
            videoComplete = true;
            System.out.println("Stopping recording at " + new Date());
            t1.join();
            System.out.println("Screenshot thread complete");
            t2.join();
            System.out.println("Video maker thread complete");
            getRecorder().stop();
            System.out.println("Recording has been saved successfully at " + new File(outputVideo).getAbsolutePath());
        } catch (Exception e) {
            System.out.println("Exception while stopping the recorder " + e.getMessage());
        }
    }
}
