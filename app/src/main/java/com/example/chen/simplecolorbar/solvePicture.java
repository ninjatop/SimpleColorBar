package com.example.chen.simplecolorbar;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Environment;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.IntBuffer;
import java.util.BitSet;

/** *
 * Created by CHEN on 2016/10/10.
 * 处理图片
 */
public class solvePicture {
    public static final String TAG = "solvePicture";
    Bitmap img;
    Point [][]points;
    public  Matrix matrix;


    public int clolrTypeNum = 9;//颜色种类
    public int deltaNum = 4;// 变化的数目
    public int []rgbValue;
    public int []black;//黑色的值
    public int []white;//白色的值
    public int frameIndex = -1;//帧序号
    public HistogramMatching histogram = new HistogramMatching();
/*    public solvePicture(Bitmap bitmap) {
        this.img=bitmap;
        IntBuffer intBuffer = IntBuffer.allocate(img.getWidth()*img.getHeight());
        img.copyPixelsToBuffer(intBuffer);
        this.matrix = new Matrix(intBuffer.array(), img.getWidth(), img.getHeight());
        //matrix.perspectiveTransform();
        this.points = matrix.getRealLocation();//获得原始坐标点
        try {
            getFrameIndex();//获得帧序号
        }
        catch (CRCCheckException e){
            Log.d(TAG, "frameIndex CRC check failed");
        }
    }*/
    public solvePicture(Bitmap bitmap,int []initBorder) {
        this.img=bitmap;
        IntBuffer intBuffer = IntBuffer.allocate(img.getWidth()*img.getHeight());
        img.copyPixelsToBuffer(intBuffer);
        this.matrix = new Matrix(intBuffer.array(), img.getWidth(), img.getHeight(),initBorder);
        //matrix.perspectiveTransform();
        this.points = matrix.getRealLocation();//获得原始坐标点
        try {
            getFrameIndex();//获得帧序号
        }
        catch (CRCCheckException e){
            Log.d(TAG, "frameIndex CRC check failed");
        }
    }

    public void printLocation(){
        StringBuffer buffer = new StringBuffer();
        for(int i = 0; i < points.length; i ++) {
            for (int j = 0; j < points[i].length; j++)
                buffer.append(points[i][j].getX() + " " + points[i][j].getY() + ",");
            buffer.append("\n");
        }
        File file = new File(Environment.getExternalStorageDirectory(),"abc/test7/location.txt");
        OutputStream os;
        try {
            os = new FileOutputStream(file);
            os.write(buffer.toString().getBytes());
            os.close();
        } catch (FileNotFoundException e) {
            Log.i(TAG, "file path error, cannot create file:" + e.toString());
            //return false;
        }catch (IOException e){
            Log.i(TAG, "IOException:" + e.toString());
            //return false;
        }


    }
    /**
     * 处理图片
     */
    public void processImg(){
        //initBlackAndWhite();
        //getFrameIndex();
        //initRBGValue();
        //System.out.println("frame index " + this.frameIndex);
        //compare();
        getCorrectRate();
        //getColorType(get(2,4));
        //printColor();//打印颜色

    }

    public BitSet getContent(){
        StringBuffer buffer = new StringBuffer();
        //int []color = new int[this.matrix.contentWidth * this.matrix.contentHeight];
        BitSet content = new BitSet();
        int index = 0;
        int []compare = new int [this.deltaNum+1];
        for(int i =2;i<this.matrix.contentHeight; i+= this.deltaNum + 1){
            for(int j =2;j<this.matrix.contentWidth;j += this.deltaNum + 1){
                if(this.frameIndex % 2 ==0 ) {
                    for (int k = 0; k <this.deltaNum+1;k++) {
                        compare[k] = getRGB(get(i, j + k))[0];
                        buffer.append(compare[k]+",");
                    }
                }
                else{
                    for (int k = 0; k <this.deltaNum+1;k++) {
                        compare[k] = getRGB(get(i, j + k))[1];
                        buffer.append(compare[k]+",");
                    }
                }
                buffer.append("\n");

                for(int row =1;row < this.deltaNum + 1; row++)
                    for(int line = 0;line < this.deltaNum + 1; line++){
                        int colorType = getStr(get(i+row,j+line),compare);
                        switch(colorType % 4){
                            case 0:break;
                            case 1:
                                content.set(index);
                                break;
                            case 2:
                                content.set(index+1);
                                break;
                            case 3:
                                content.set(index);
                                content.set(index + 1);
                                break;
                        }
                        int []temp = getRGB(get(i+row,j+line));
                        if(this.frameIndex %2 == 0)
                            buffer.append(temp[0]+",");
                        else
                            buffer.append(temp[1]+",");
                        index += 2;
                    }
                buffer.append("\n");
            }
        }
        File file = new File(Environment.getExternalStorageDirectory(),"abc/test7/compare.txt");
        OutputStream os;
        try {
            os = new FileOutputStream(file);
            os.write(buffer.toString().getBytes());
            os.close();
        } catch (FileNotFoundException e) {
            Log.i(TAG, "file path error, cannot create file:" + e.toString());
            //return false;
        }catch (IOException e){
            Log.i(TAG, "IOException:" + e.toString());
            //return false;
        }
        printLocation();
        return content;
    }
    public void matchHistogram(){
        int []content = new int[matrix.contentWidth * matrix.contentHeight * 4 / 5];
        StringBuffer buffer = new StringBuffer();
        int index = 0;
        int []compare = new int [this.deltaNum+1];
        for(int i =2;i<this.matrix.contentHeight; i+= this.deltaNum + 1){
            for(int j =2;j<this.matrix.contentWidth;j += this.deltaNum + 1){
                for(int row =1;row < this.deltaNum + 1; row++)
                    for(int line = 0;line < this.deltaNum + 1; line++){
                        int []temp = getRGB(get(i+row,j+line));
                        if(this.frameIndex %2 == 0)
                            buffer.append(temp[0]+",");
                        else
                            buffer.append(temp[1]+",");
                    }
            }
        }
        String []data = buffer.toString().split(",");
        for(int i = 0; i < data.length ; i++){
            content[i] = Integer.parseInt(data[i]);
        }
        histogram.HistogramMatching(content,"abc/test1/0.txt");




    }


    public void initBlackAndWhite(){
        int []black = new int[]{0,0,0};
        int []white = new int[]{0,0,0};
        for(int i = 12;i<20;i+=2){
            int []color = getRGB(get(1,i));
            for(int j = 0; j < 3; j++)
                white[j] += color[j];
        }
        for(int i = 13;i<20;i+=2){
            int []color = getRGB(get(1,i));
            for(int j = 0; j < 3; j++)
                black[j] += color[j];
        }
        for(int j = 0; j < 3; j++) {
            black[j] = black[j] / 4;
            white[j] = white[j] / 4;
        }
        this.black = black;
        this.white = white;

    }

    /**
     * 获得这一帧的帧序号
     */
    public void getFrameIndex() throws CRCCheckException{
        CRC8 crcCheck = new CRC8();
        int x = 2;
        int y = 2 + matrix.contentWidth;
        BitSet bitset = new BitSet();
        for(int i = 0;i < 40; i++){
            int []rgb = getRGB(get(x+i,y));
            if(rgb[2] > 150)
                bitset.set(i);
        }
        int intLength=32;
        int byteLength=8;
        int index=0;
        for(int i=0;i< intLength;i++){
            if(bitset.get(i)){
                index |= 1<<(intLength-i-1);
            }
        }
        int crcLength=8;
        int crc=0;
        for(int i=0;i<byteLength;i++){
            if(bitset.get(intLength+i)){
                crc|=1<<(crcLength-i-1);
            }
        }
        crcCheck.reset();
        crcCheck.update(index);
        int truth=(int)crcCheck.getValue();
        Log.d(TAG, "CRC check: frame original index:" + index + " CRC:" + crc + " truth:" + truth);
        if(crc!=truth || index<0){
            throw CRCCheckException.getNotFoundInstance();
        }
        this.frameIndex = index;
    }
    public void printColor(){
        for(int i = 0; i < this.matrix.getBarCodeWidth(); i++){
            for(int j = 0; j < this.matrix.getBarCodeHeight(); j++){
                Log.d(TAG,"第" + i + "行" + "第" + j + "列" + "是第" + getColorType(get(i,j))+"  "+ getRGB(i,j));
            }
        }
    }
    public int get(int x ,int y){
        //System.out.println(this.points[x][y].getX()+" "+this.points[x][y].getY());
        return this.img.getPixel(this.points[x][y].getX(),this.points[x][y].getY());
        //return rgb;
    }

    /**
     * 获得转化成String类型的rgb值
     * @param x
     * @param y
     * @return
     */
    public  String getRGB(int x,int y){
        int color = this.img.getPixel(x,y);
        int r = Color.red(color);
        int g = Color.green(color);
        int b = Color.blue(color);
        int a = Color.alpha(color);
        String result="r:"+r+"\tg:"+g+"\tb:"+b;
        //System.out.println(img.getWidth()+"\t"+img.getHeight());
        return result;
    }

    /**
     *
     * @param color int型color值
     * @return 长度为3的int数组
     */
    public int [] getRGB(int color){
        int r = Color.red(color);
        int g = Color.green(color);
        int b = Color.blue(color);
        return new int[]{r,g,b};
    }

    /**
     * 获得参考点的相对值
     */
    protected void initRBGValue(){
        this.rgbValue = new int[this.clolrTypeNum];
        int index = 0;
        if(frameIndex % 2 ==0) {
            for (int loop = 0; loop < 3; loop++) {
                for(int i = 0; i < this.clolrTypeNum; i++)
                this.rgbValue[i] += getRGB(get(loop * this.clolrTypeNum + i + 1 , 1))[0];
            }
        }
        else{
            for (int loop = 0; loop < 3; loop++) {
                for(int i = 0; i < this.clolrTypeNum; i++)
                    this.rgbValue[i] += getRGB(get(loop * this.clolrTypeNum + 1 + i , 1))[1];
            }
        }
        StringBuffer buffer = new StringBuffer();
        for(int i = 0;i<this.clolrTypeNum;i++) {
            this.rgbValue[i] /= 3;
            buffer.append(this.rgbValue[i] + " ");
        }
        Log.d(TAG,"referece color: "+ buffer.toString());

    }


    public int getColorType(int color,int standard){
        int []current =getRGB(color);
        int []std =getRGB(standard);
        if(this.frameIndex %2 == 0){//在红色帧上
            if(current[0]>std[0])
                return 1;
            else
                return 0;
        }
        else{
            if(current[1]>std[1])
                return 1;
            else
                return 0;
        }
    }

    public int getColorType(int color){
        int []current = getRGB(color);
        int result = -1 ;
        int similarity = 999999999;
        for(int i = 0; i < this.clolrTypeNum; i++){
            int []temp = getRGB(this.rgbValue[i]);
            int diff =  (temp[0] - current[0]) * (temp[0] - current[0]) + (temp[1] - current[1]) * (temp[1] - current[1]) + (temp[2] - current[2]) * (temp[2] - current[2]);
            if(diff <similarity){
                similarity = diff;
                result = i;
            }
        }
        if(result == -1)
            return -1;
        return result;
    }

    /**
     * 输出每个内容点颜色
     */
    public void compare(){
        //int []color = new int[this.matrix.contentLength * this.matrix.contentLength];
        //int index = 0;
        StringBuffer str = new StringBuffer();
        for(int i =2;i<=this.matrix.contentHeight + 2;i++){
            for(int j =2;j<=this.matrix.contentWidth + 2;j++){
                int colorType = getColorType(get(i,j));
                str.append(colorType+",");
                //color[index] =colorType;
                //index++;
            }
            str.append("\n");
        }
        Log.d(TAG,str.toString());
    }
    /*
    public int getCorrectRate(){
        //获得图片的颜色种类值
        int []color = new int[this.matrix.contentLength * this.matrix.contentLength];
        int index = 0;
        for(int i =2;i<this.matrix.contentLength+2;i++){
            for(int j =2;j<this.matrix.contentLength+2;j++){
                int colorType = getColorType(get(i,j));
                color[index] =colorType;
                index++;
            }
        }
        //Log.d(TAG,"index:"+index);

        String path = Environment.getExternalStorageDirectory()+"/screencamera/6.txt";
        StringBuffer str = new StringBuffer();
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(new File(path))));
            String line = null;
            while((line =reader.readLine())!=null){
                str.append(line.trim());
            }

        }catch (Exception e){
            e.printStackTrace();
        }

        String []temp = str.toString().split(",");
        Log.d(TAG,temp.length+"");
        int errorNum = 0;
        for(int i = 0;i<color.length;i++){
            if(color[i]!=Integer.parseInt(temp[i]))
                errorNum++;
        }
        Log.d(TAG,"Error Num is "+errorNum);
        System.out.println("Error rate is :"+(((double)errorNum)/matrix.contentLength/matrix.contentLength));
        return errorNum;

    }
    */

    /**
     *
     * @param current
     * @param compare
     * @return 返回颜色代表的index
     */
    public int getStr(int current,int []compare){
        int cur = -1;
        if(this.frameIndex % 2 == 0) {
            cur = getRGB(current)[0];
            int result = -1;
            int dist = 500;
            for(int i = 1; i < this.deltaNum + 1;i++){
                if(Math.abs(cur-compare[i]) < dist){
                    dist = Math.abs(cur-compare[i]);
                    result = i;
                }
            }
            return result - 1;
            /*if(cur < compare[0]){
                int dist = 500;
                for(int i = 1;i< this.deltaNum/2 + 1 ;i++){
                    if(Math.abs(cur-compare[i]) < dist){
                        dist = Math.abs(cur-compare[i]);
                        result = i;
                    }
                }
                return result - 1;
            }
            else{
                int dist = 500;
                for(int i = this.deltaNum/2 + 1;i < this.deltaNum + 1;i++){
                    if(Math.abs(cur-compare[i]) < dist){
                        dist = Math.abs(cur-compare[i]);
                        result = i;
                    }
                }
                return result - 1;
            }*/


        }
        else{
            cur = getRGB(current)[1];
            int result = -1;
            int dist = 500;
            for(int i = 1; i < this.deltaNum + 1;i++){
                if(Math.abs(cur-compare[i]) < dist){
                    dist = Math.abs(cur-compare[i]);
                    result = i;
                }
            }
            return result + this.deltaNum - 1;
            /*cur = getRGB(current)[1];
            int result = -1;
            if(cur < compare[0]){
                int dist = 500;
                for(int i = 1;i < this.deltaNum/2 + 1;i++){
                    if(Math.abs(cur-compare[i]) < dist){
                        dist = Math.abs(cur-compare[i]);
                        result = i;
                    }
                }
                return result + this.deltaNum - 1;
            }
            else{
                int dist = 500;
                for(int i = this.deltaNum/2;i < this.deltaNum + 1;i++){
                    if(Math.abs(cur-compare[i]) < dist){
                        dist = Math.abs(cur-compare[i]);
                        result = i;
                    }
                }
                return result + this.deltaNum - 1;
            }*/
        }


    }
    public int getCorrectRate(){
        //获得图片的颜色种类值
        int []color = new int[this.matrix.contentWidth * this.matrix.contentHeight];
        int index = 0;
        int []compare = new int [this.deltaNum+1];
        for(int i =2;i<this.matrix.contentHeight; i+= this.deltaNum + 1){
            for(int j =2;j<this.matrix.contentWidth;j += this.deltaNum + 1){
                if(this.frameIndex % 2 ==0 ) {
                    for (int k = 0; k <this.deltaNum+1;k++)
                        compare[k] = getRGB(get(i,j+k))[0];
                }
                else{
                    for (int k = 0; k <this.deltaNum+1;k++)
                        compare[k] = getRGB(get(i,j+k))[1];
                }
                for(int row =1;row < this.deltaNum + 1; row++)
                    for(int line = 0;line < this.deltaNum + 1; line++){
                        int colorType = getStr(get(i+row,j+line),compare);
                        color[index] = colorType;
                        index ++;
                    }
            }
        }
        //Log.d(TAG,"index:"+index);

        String path = Environment.getExternalStorageDirectory()+"/abc/test9/colorSequence/"+this.frameIndex+".txt";
        StringBuffer str = new StringBuffer();
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(new File(path))));
            String line = null;
            while((line =reader.readLine())!=null){
                str.append(line.trim());
            }

        }catch (Exception e){
            e.printStackTrace();
        }

        //String []temp = str.toString().split(",");
        //String temp = str.toString();
        String []real = str.toString().split(",");
        Log.d(TAG,real.length+"");
        int errorNum = 0;

        for(int i = 0;i<real.length;i++){
            if(color[i]!=Integer.parseInt(real[i])) {
                errorNum++;
                //System.out.print(i);
            }
        }
        Log.d(TAG,"Error Num is "+errorNum);
        System.out.println("Error rate is :"+(((double)errorNum)/real.length));
        return errorNum;

    }
}
