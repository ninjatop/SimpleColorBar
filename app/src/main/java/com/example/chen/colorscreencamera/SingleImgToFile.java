package com.example.chen.colorscreencamera;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.util.Log;

import net.fec.openrq.parameters.FECParameters;

import java.nio.ByteBuffer;
import java.util.BitSet;

/**
 * 识别图片中的二维码,测试用
 */
public class SingleImgToFile extends MediaToFile {
    private static final String TAG = "SingleImgToFile";//log tag
    private static final boolean VERBOSE = false;//是否记录详细log
    private static final int COLOR_TYPE=Matrix.COLOR_TYPE_RGB;
    private static BarcodeFormat barcodeFormat;

    private Handler processHandler;

    /**
     * 构造函数,获取必须的参数
     *
     * @param handler   实例
     */
    public SingleImgToFile(Handler handler,BarcodeFormat format,String truthFilePath, String saveFilePath) {
        super(handler);
        barcodeFormat=format;
        ProcessFrame processFrame=new ProcessFrame("process");
        processFrame.start();
        processHandler=new Handler(processFrame.getLooper(), processFrame);
        processHandler.sendMessage(processHandler.obtainMessage(ProcessFrame.WHAT_BARCODE_FORMAT,format));
        if(!truthFilePath.equals("")) {
            processHandler.sendMessage(processHandler.obtainMessage(ProcessFrame.WHAT_TRUTH_FILE_PATH,truthFilePath));
        }
        if(!saveFilePath.equals(""))
            processHandler.sendMessage(processHandler.obtainMessage(ProcessFrame.WHAT_FILE_NAME, saveFilePath));
    }

    /**
     * 对单个图片进行解码识别二维码
     * 注意这个方法只是拿来测试识别算法等
     *
     * @param filePath 图片路径
     */
    public void singleImg(String filePath) {
        int []borders = null; //1



        final int NUMBER_OF_SOURCE_BLOCKS=1;
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        Bitmap bitmap = BitmapFactory.decodeFile(filePath, options);
        solvePicture solve = new solvePicture(bitmap, borders);
/*        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(bitmap.getWidth() * bitmap.getHeight() * 4);
        bitmap.copyPixelsToBuffer(byteBuffer);
        bitmap.recycle();*/
        updateInfo("正在识别...");
        Matrix matrix;

        int fileByteNum;

        try {
            fileByteNum = getFileByteNum(solve.matrix);
        }catch (CRCCheckException e){
            Log.d(TAG, "head CRC check failed");
            return;
        }
        if(fileByteNum == 0){
            Log.d(TAG,"wrong file byte number");
            return;
        }
        Log.i(TAG,"file is "+fileByteNum+" bytes");
        int length = solve.matrix.realContentByteLength();
        //solve.matchHistogram();

        BitSet content = solve.getContent();
        FECParameters parameters = FECParameters.newParameters(fileByteNum, length, NUMBER_OF_SOURCE_BLOCKS);
        processHandler.sendMessage(processHandler.obtainMessage(ProcessFrame.WHAT_FEC_PARAMETERS,parameters));
        processHandler.sendMessage(processHandler.obtainMessage(ProcessFrame.WHAT_RAW_CONTENT,content));

        updateInfo("识别完成");
    }
}
