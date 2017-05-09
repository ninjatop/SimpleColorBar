package com.example.chen.simplecolorbar;

import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.os.Handler;
import android.util.Log;

import java.io.File;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by zhantong on 16/4/29.
 */
public class VideoToFile extends StreamToFile {
    private static final String TAG = "VideoToFile";//log tag
    private static final boolean VERBOSE = false;//是否记录详细log
    private static final int COLOR_TYPE = Matrix.COLOR_TYPE_RGB;
    private VideoToFrames videoToFrames;
    public VideoToFile(Handler handler,String truthFilePath) {
        super(handler,truthFilePath);
    }
    public int getImgColorType(){
        return COLOR_TYPE;
    }
    public void beforeDataDecoded(int frameAllCount){
        videoToFrames.stopDecode();
        onDecodeFlag = false;
        updateInfo("识别成功！所用帧数为\t"+frameAllCount);
    }
    public void toFile(String fileName,final String videoFilePath){
        if(VERBOSE){Log.i(TAG,"process video file");}
        final LinkedBlockingQueue<RawImage> frameQueue = new LinkedBlockingQueue<>();
        int[] widthAndHeight= getVideoWidthAndHeight(videoFilePath);
        int frameWidth=widthAndHeight[0];
        int frameHeight=widthAndHeight[1];
        videoToFrames = new VideoToFrames();
        videoToFrames.setEnqueue(frameQueue);
        try {
            videoToFrames.decode(videoFilePath);
        }catch (Throwable t){
            t.printStackTrace();
        }

        streamToFile(frameQueue, frameWidth, frameHeight, fileName);
    }
    private int[] getVideoWidthAndHeight(String videoFilePath){
        File inputFile = new File(videoFilePath);
        MediaExtractor extractor = new MediaExtractor();
        try {
            extractor.setDataSource(inputFile.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
        int trackIndex = selectTrack(extractor);
        extractor.selectTrack(trackIndex);
        MediaFormat format = extractor.getTrackFormat(trackIndex);
        int imgWidth = format.getInteger(MediaFormat.KEY_WIDTH);
        int imgHeight = format.getInteger(MediaFormat.KEY_HEIGHT);
        return new int[] {imgWidth,imgHeight};
    }
    private int selectTrack(MediaExtractor extractor) {
        // Select the first video track we find, ignore the rest.
        int numTracks = extractor.getTrackCount();
        for (int i = 0; i < numTracks; i++) {
            MediaFormat format = extractor.getTrackFormat(i);
            String mime = format.getString(MediaFormat.KEY_MIME);
            if (mime.startsWith("video/")) {
                if (VERBOSE) {
                    Log.d(TAG, "Extractor selected track " + i + " (" + mime + "): " + format);
                }
                return i;
            }
        }
        return -1;
    }
}
