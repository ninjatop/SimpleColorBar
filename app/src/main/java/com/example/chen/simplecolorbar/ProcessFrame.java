package com.example.chen.simplecolorbar;

import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;

import net.fec.openrq.ArrayDataDecoder;
import net.fec.openrq.EncodingPacket;
import net.fec.openrq.OpenRQ;
import net.fec.openrq.SymbolType;
import net.fec.openrq.decoder.SourceBlockDecoder;
import net.fec.openrq.parameters.FECParameters;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

import com.example.chen.simplecolorbar.ReedSolomon.GenericGF;
import com.example.chen.simplecolorbar.ReedSolomon.ReedSolomonDecoder;
import com.example.chen.simplecolorbar.ReedSolomon.ReedSolomonException;

/**
 * Created by zhantong on 16/5/29.
 */
public class ProcessFrame extends HandlerThread implements Handler.Callback {
    private static final String TAG="ProcessFrame";
    private static final boolean VERBOSE = false;

    //public static final int WHAT_BARCODE_FORMAT=1;
    public static final int WHAT_FEC_PARAMETERS=1;
    public static final int WHAT_RAW_CONTENT=2;
    public static final int WHAT_FILE_NAME=3;
    public static final int WHAT_TRUTH_FILE_PATH=4;

    private static boolean IS_RAPTORQ_ENABLE;

    private static boolean IS_STATISTIC_ENABLE;

    private List<RawContent> rawContentList;
    //private Matrix matrix;
    private solvePicture solve;
    private ArrayDataDecoder dataDecoder;
    private SourceBlockDecoder sourceBlock;
    private ReedSolomonDecoder decoder;
    private String fileName;
    private FrameCallback mFrameCallback;
    private FileToBitSet truthBitSet;

    private Statistics statistics;

    private BitSet withoutRaptorQ;
    private int maxSourceEsi;
    private long start = System.currentTimeMillis();
    private int count = 0;
    private int indexStart = -1;
    private  int indexEnd = -1;

    private boolean decodingFinish=false;


    public ProcessFrame(String name){

        super(name);
        rawContentList=new ArrayList<>();
        IS_RAPTORQ_ENABLE = true;
        IS_STATISTIC_ENABLE = false;
        solve = new solvePicture();
        decoder = new ReedSolomonDecoder(selectRSLengthParam(solve.ecLength));
        //PropertiesReader propertiesReader=new PropertiesReader();
        //IS_RAPTORQ_ENABLE Boolean.parseBoolean(propertiesReader.getProperty("RaptorQ.enable"));
        /*IS_STATISTIC_ENABLE=Boolean.parseBoolean(propertiesReader.getProperty("statistic.enable"));
        if(IS_STATISTIC_ENABLE) {
            statistics = new Statistics();
        }*/

    }



    public interface FrameCallback{
        void onLastPacket(int frameAllCount);
    }
    public void setCallback(FrameCallback callback){
        mFrameCallback = callback;
    }
    ///这里就是放每一帧的内容
    private void put(BitSet content){
        EncodingPacket encodingPacket;
        try {
            int[] conIn12 = BitSetToInt(content);
            decoder.decode(conIn12, solve.ecNum);
            int realByteNum = solve.RSContentByteLength();
            byte[] raw = new byte[realByteNum];
            for(int i=0;i<raw.length * 8;i++){
                if((conIn12[i/solve.ecLength]&(1<<(i%solve.ecLength)))>0){
                    raw[i/8]|=1<<(i%8);
                }
            }
            encodingPacket = dataDecoder.parsePacket(raw, true).value();
            Log.i(TAG, "encoding symbol ID:" + encodingPacket.encodingSymbolID() + "\t" + encodingPacket.symbolType());
            if(encodingPacket.encodingSymbolID() == 1 && indexStart == -1)
                indexStart = count;
            if (isLastEncodingPacket(sourceBlock, encodingPacket)) {
                decodingFinish = true;
                Log.d(TAG, "the last esi is " + encodingPacket.encodingSymbolID());
            }
            dataDecoder.sourceBlock(encodingPacket.sourceBlockNumber()).putEncodingPacket(encodingPacket);
        }catch (ReedSolomonException e){
            Log.d(TAG,"RS decode failed");
        }
        if (dataDecoder.isDataDecoded()) {
            indexEnd = count;
            mFrameCallback.onLastPacket(indexEnd - indexStart);//告诉Stream到最后一帧了
            long end = System.currentTimeMillis();
            Log.d(TAG,"all frame takes" + (end - start));
            writeRaptorQDataFile(dataDecoder, fileName);
        }
    }
    private boolean isAllSet(BitSet bitSet,int size){
        return bitSet.get(0) && bitSet.nextClearBit(0) > size;
    }
    private GenericGF selectRSLengthParam(int ecLength){
        switch (ecLength){
            case 8:
                return GenericGF.QR_CODE_FIELD_256;
            case 10:
                return GenericGF.AZTEC_DATA_10;
            case 12:
                return GenericGF.AZTEC_DATA_12;
        }
        return null;
    }
    private void checkBitSet(BitSet raw,int esi){
        BitSet truth=truthBitSet.getPacket(esi);
        if(truth==null){
            Log.d(TAG,"esi "+esi+" don't exist");
            return;
        }
        BitSet clone = (BitSet) raw.clone();
        clone.xor(truth);
        int bitError=clone.cardinality();
        Log.d(TAG, "esi " + esi + " has " + bitError + " bit errors");
    }

    /**
     * BitSet转化成只有低12位有用的Int数组
     * @param content
     * @return
     */
    private int[] BitSetToInt(BitSet content){
        int numRealBits = solve.frameBitNum ;
        int[] con=new int[numRealBits / solve.ecLength];
        for(int i=0;i < numRealBits;i++){
            if(content.get(i)){
                con[i/solve.ecLength] |= 1<<(i % solve.ecLength);
            }
        }
        return con;
    }
    private byte[] getContent(BitSet content) throws ReedSolomonException {
        int[] rawContent=BitSetToInt(content);
        int[] decodedContent=decode(rawContent,solve.ecNum);
        int realByteNum=solve.RSContentByteLength();
        byte[] res=new byte[realByteNum];
        for(int i=0;i<res.length*8;i++){
            if((decodedContent[i/solve.ecLength]&(1<<(i%solve.ecLength)))>0){
                res[i/8]|=1<<(i%8);
            }
        }
        return res;
    }
    private int[] decode(int[] raw,int ecNum) throws ReedSolomonException {
        decoder.decode(raw, ecNum);
        return raw;
    }
    private boolean isLastEncodingPacket(SourceBlockDecoder sourceBlock,EncodingPacket encodingPacket){
        return (sourceBlock.missingSourceSymbols().size() - sourceBlock.availableRepairSymbols().size() == 1)
                &&((encodingPacket.symbolType()== SymbolType.SOURCE && !sourceBlock.containsSourceSymbol(encodingPacket.encodingSymbolID()))
                ||(encodingPacket.symbolType()== SymbolType.REPAIR && !sourceBlock.containsRepairSymbol(encodingPacket.encodingSymbolID())));
    }
    private void writeRaptorQDataFile(ArrayDataDecoder decoder,String fileName){
        byte[] out = decoder.dataArray();
        String sha1 = FileVerification.bytesToSHA1(out);
        Log.d(TAG, "file SHA-1 verification: " + sha1);
        bytesToFile(out, fileName);
    }
    private boolean bytesToFile(byte[] bytes,String fileName){
        if(fileName.isEmpty()){
            Log.i(TAG, "file name is empty");
            return false;
        }
        File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),fileName);
        OutputStream os;
        try {
            os = new FileOutputStream(file);
            os.write(bytes);
            os.close();
        } catch (FileNotFoundException e) {
            Log.i(TAG, "file path error, cannot create file:" + e.toString());
            return false;
        }catch (IOException e){
            Log.i(TAG, "IOException:" + e.toString());
            return false;
        }
        Log.i(TAG,"file created successfully: "+file.getAbsolutePath());
        return true;
    }
    private static BitSet toBitSet(int data[],int bitNum){
        int index=0;
        BitSet bitSet=new BitSet();
        for(int current:data){
            for(int i=0;i<bitNum;i++){
                if((current&(1<<i))>0){
                    bitSet.set(index);
                }
                index++;
            }
        }
        return bitSet;
    }
@Override
public boolean handleMessage(Message msg) {
    switch (msg.what) {
        /*case WHAT_BARCODE_FORMAT:
            barcodeFormat = (BarcodeFormat) msg.obj;
            matrix = new Matrix();
            decoder = new ReedSolomonDecoder(selectRSLengthParam(matrix.ecLength));
            if(IS_STATISTIC_ENABLE) {
                statistics.setBarcodeFormat(barcodeFormat);
            }
            break;*/
        case WHAT_FEC_PARAMETERS:
            FECParameters parameters = (FECParameters) msg.obj;
            if(IS_STATISTIC_ENABLE) {
                statistics.loadFECParameters(parameters);
            }
            dataDecoder = OpenRQ.newDecoder(parameters, 0);
            sourceBlock = dataDecoder.sourceBlock(dataDecoder.numberOfSourceBlocks() - 1);
            if(!IS_RAPTORQ_ENABLE){
                withoutRaptorQ=new BitSet();
                maxSourceEsi=sourceBlock.numberOfSourceSymbols()-1;
            }
            break;
        case WHAT_RAW_CONTENT:
            BitSet content = (BitSet) msg.obj;
            if(!decodingFinish) {
                count ++;
                put(content);
            }
            break;
        case WHAT_FILE_NAME:
            fileName = (String) msg.obj;
            break;
        case WHAT_TRUTH_FILE_PATH:
            String truthFilePath=(String)msg.obj;
            if(IS_STATISTIC_ENABLE) {
                statistics.loadTruthFile(truthFilePath);
            }
            //truthBitSet=new FileToBitSet(barcodeFormat,truthFilePath);
            break;
    }
    return true;
}

    public int getFecPayloadID(BitSet bitSet){
        int value=0;
        for (int i = bitSet.nextSetBit(0); i <32; i = bitSet.nextSetBit(i + 1)) {
            value|=(1<<(i%8))<<(3-i/8)*8;
        }
        return value;
    }
    public int extractSourceBlockNumber(int fecPayloadID){
        return fecPayloadID>>24;
    }
    public int extractEncodingSymbolID(int fecPayloadID){
        return fecPayloadID&0x0FFF;
    }
}
