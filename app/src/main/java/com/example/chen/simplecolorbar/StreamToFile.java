package com.example.chen.simplecolorbar;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;

import net.fec.openrq.parameters.FECParameters;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.BitSet;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Created by zhantong on 16/2/29.
 */
public class StreamToFile extends MediaToFile implements ProcessFrame.FrameCallback{
    private static final String TAG = "StreamToFile";//log tag
    private static final boolean VERBOSE = false;//是否记录详细log
    private static final long QUEUE_WAIT_SECONDS=4;
    private Handler processHandler;
    protected boolean onDecodeFlag = true;//判断是否继续解码
    public StreamToFile(Handler handler,String truthFilePath) {
        super(handler);
        //barcodeFormat=format;
        ProcessFrame processFrame = new ProcessFrame("process");processFrame.start();
        processHandler=new Handler(processFrame.getLooper(), processFrame);
        //processHandler.sendMessage(processHandler.obtainMessage(ProcessFrame.WHAT_BARCODE_FORMAT));
        if(!truthFilePath.equals("")) {
            processHandler.sendMessage(processHandler.obtainMessage(ProcessFrame.WHAT_TRUTH_FILE_PATH,truthFilePath));
        }
        processFrame.setCallback(this);
    }
    public int getImgColorType(){
        return -1;
    }
    public void notFound(int fileByteNum){}
    public void crcCheckFailed(){}
    public void beforeDataDecoded(int frameAllCount){
        /*onDecodeFlag = false;
        updateInfo("识别成功！");*/
    }
    protected void streamToFile(LinkedBlockingQueue<byte[]> imgs,int frameWidth,int frameHeight,String fileName) {
        processHandler.sendMessage(processHandler.obtainMessage(ProcessFrame.WHAT_FILE_NAME,fileName));
        final int NUMBER_OF_SOURCE_BLOCKS=1;
        int fileByteNum=-1;
        int frameCount = -1;
        int lastSuccessIndex = 0;
        int frameAmount = 0;
        byte[] img = {};

        //Matrix matrix = null;
        int[] borders = null;//用来保存边界情况
        int imgColorType = getImgColorType();
        solvePicture solve = null;
        while (onDecodeFlag) {
            long time1 = System.currentTimeMillis();
            long time2 =0; long time3 =0; long time4=0;
            frameCount++;
            updateInfo("正在识别...");
            try {
                if(VERBOSE){Log.d(TAG,"is queue empty:"+imgs.isEmpty());}
                img = imgs.poll(QUEUE_WAIT_SECONDS, TimeUnit.MINUTES);
            } catch (InterruptedException e) {
                throw new RuntimeException(e.getMessage());
            }
            if(img == null){
                updateInfo("识别失败！");
                break;
            }
            updateDebug(lastSuccessIndex, frameAmount, frameCount);
            Log.i(TAG,"processing frame: "+frameCount);
            try {
                //处理每个帧!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
                time2 = System.currentTimeMillis();
                YuvImage image = new YuvImage(img, ImageFormat.NV21, frameWidth, frameHeight, null);
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                image.compressToJpeg(new Rect(0, 0, frameWidth, frameHeight), 80, stream);
                System.out.print("stream size is " + stream.size());
                Bitmap bmp = BitmapFactory.decodeByteArray(img, 0, img.length);

                String tempName = Environment.getExternalStorageDirectory()+"/abc/test7/time/"+frameCount+"1.jpg";
                FileOutputStream stream2 = new FileOutputStream(tempName);
                bmp.compress(Bitmap.CompressFormat.JPEG,90,stream2);

                stream2.close();
                //stream.close();
                time3 = System.currentTimeMillis();
                solve = new solvePicture(bmp, borders);

            }
            catch (IOException e) {
                e.printStackTrace();
            }
            //System.out.println("each frame take " + (time2 - time1)+"\t"+(time3 - time2) +"\t"+(time4 - time3));

            if(fileByteNum == -1){
                try {
                    fileByteNum = getFileByteNum(solve.matrix);
                }catch (CRCCheckException e){
                    Log.d(TAG, "head CRC check failed");
                    crcCheckFailed();
                    continue;
                }
                if(fileByteNum == 0){
                    Log.d(TAG,"wrong file byte number");
                    fileByteNum = -1;
                    continue;
                }
                Log.i(TAG,"file is "+fileByteNum+" bytes");
                int length=solve.matrix.realContentByteLength();
                FECParameters parameters = FECParameters.newParameters(fileByteNum, length, NUMBER_OF_SOURCE_BLOCKS);
                Log.i(TAG,"FEC parameters: "+ parameters.toString());
                processHandler.sendMessage(processHandler.obtainMessage(ProcessFrame.WHAT_FEC_PARAMETERS,parameters));
            }
            BitSet content = solve.getContent();
            //rawContent.frameIndex=frameCount;
            processHandler.sendMessage(processHandler.obtainMessage(ProcessFrame.WHAT_RAW_CONTENT,content));
            borders = smallBorder(solve.matrix.borders);
            //beforeDataDecoded();
        }
    }
    @Override
    public void onLastPacket(int frameAllCount) {
        beforeDataDecoded(frameAllCount);
    }
}