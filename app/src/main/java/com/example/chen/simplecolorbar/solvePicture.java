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
    public  RawImage img;
    public int []rgbValue;
    public int []black;//黑色的值
    public int []white;//白色的值
    public int frameIndex = -1;//帧序号
    public int []vertexes;//四个顶点的位置

    protected  int imgWidth;//图片宽度
    protected  int imgHeight;//图片高度

    protected int bitsPerBlock = 3 ;//每个小方块的bit数
    protected int deltaNum = (int)(Math.pow(2.0,bitsPerBlock )) ;//变化的数目



    protected PerspectiveTransform transform;//透视变换参数
    protected int BlackBorderLenght = 1;//第二层黑色边界
    protected int mixBorderLength = 1;//调色板的边界
    protected int MixBorderLeft = deltaNum;//左边的参考色


    protected int contentWidth = 120;//内容宽度
    protected  int contentHeight = 50;//内容高度

    protected Point [][] points;


    int ecNum;
    int ecLength = 12;//一个symbol对应bit数目,应与RS的decoder参数保持一致
    protected double ecLevel = 0.2;//%10用来纠错
    protected int[] borders;//上一次的border坐标
    protected int frameBitNum ;//每一帧的bit总数目
    protected int[] threshols;
    public solvePicture(){
        frameBitNum = contentHeight * contentWidth * bitsPerBlock ;
        ecNum = calcEcNum(ecLevel);
    }


    public solvePicture(RawImage img,int []initBorder) {

        frameBitNum = contentHeight * contentWidth * bitsPerBlock ;
        ecNum = calcEcNum(ecLevel);
        this.img = img;
        this.imgWidth = img.getWidth();
        this.imgHeight = img.getHeight();
        this.threshols = img.getThresholds();
        try {
            this.vertexes = img.getBarcodeVertexes(initBorder, RawImage.CHANNLE_Y);
        } catch (NotFoundException e) {
            e.printStackTrace();
        }
        this.borders = img.getRectangle();
        perspectiveTransform();
        getRealLocation();
        //display();
        try {
            getFrameIndex();//获得帧序号
        }
        catch (CRCCheckException e){
            Log.d(TAG, "frameIndex CRC check failed");
        }
    }
    /*
    for test
     */
    public void display(){
        StringBuffer buffer = new StringBuffer();
        for(int y= 0;y<60;y++)
            buffer.append(4+"\t"+ y+"location:"+ points[4][y].getX()+"\t"+points[4][y].getY()+"\n");


        System.out.println(buffer);
    }
    protected int calcEcNum(double ecLevel){
        return (int)(frameBitNum / ecLength * ecLevel);
    }

    protected int getBarCodeWidth(){
        return BlackBorderLenght * 2 + mixBorderLength + MixBorderLeft + contentWidth;
    }
    protected int getBarCodeHeight(){
        return BlackBorderLenght  * 2 + mixBorderLength + contentHeight;
    }


    protected void perspectiveTransform(){
        perspectiveTransform(0, 0,getBarCodeWidth(), 0, getBarCodeWidth(), getBarCodeHeight(), 0, getBarCodeHeight());
    }
    /**
     * 透视变换
     * 指定透视变换后二维码的四个顶点坐标,结合找到的图像中的二维码顶点坐标进行透视变换
     * 透视变换相当于只是算出一些矩阵参数,不进行具体的像素数据操作
     *
     * @param p1ToX 左上角顶点x值
     * @param p1ToY 左上角顶点y值
     * @param p2ToX 右上角顶点x值
     * @param p2ToY 右上角顶点y值
     * @param p3ToX 右下角顶点x值
     * @param p3ToY 右下角顶点y值
     * @param p4ToX 左下角顶点x值
     * @param p4ToY 左下角顶点y值
     */
    protected void perspectiveTransform(float p1ToX, float p1ToY,
                                        float p2ToX, float p2ToY,
                                        float p3ToX, float p3ToY,
                                        float p4ToX, float p4ToY) {
        transform = PerspectiveTransform.quadrilateralToQuadrilateral(p1ToX, p1ToY,
                p2ToX, p2ToY,
                p3ToX, p3ToY,
                p4ToX, p4ToY,
                vertexes[0], vertexes[1],
                vertexes[2], vertexes[3],
                vertexes[4], vertexes[5],
                vertexes[6], vertexes[7]);
    }
    /**
     *
     * 获得每个小方块的原始坐标
     */
    public void getRealLocation(){
        this.points = new Point[getBarCodeHeight()][getBarCodeWidth()];
        float[] points = new float[2 * getBarCodeWidth()];
        int max = points.length;
        for (int y = 0; y < getBarCodeHeight(); y++) {
            float iValue = (float) y + 0.5f;
            for (int x = 0; x < max; x += 2) {
                points[x] = (float) (x / 2) + 0.5f;
                points[x + 1] = iValue;
            }
            transform.transformPoints(points);
            for(int i =0;i<max;i+=2){
                //Log.d(TAG,Math.round(points[i])+"+"+ Math.round(points[i + 1]));
                int m=Math.round(points[i]);
                int n=Math.round(points[i+1]);
                this.points[y][i/2]=new Point(m,n);
            }
        }
    }

    /**
     *
     * @param x 原始坐标x
     * @param y 原始坐标y
     * @return 对应信道的值
     */
    public int[] getYUV(int x, int y){
        int realx = points[x][y].getX();
        int realy = points[x][y].getY();
        return new int[]{this.img.getPixel(realx, realy, RawImage.CHANNLE_Y), this.img.getPixel(realx, realy, RawImage.CHANNLE_U), this.img.getPixel(realx, realy, RawImage.CHANNLE_V)};
    }

    /**
     * 打印每个点的真实位置
     */
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
/*    public int[][] getRefeColor(){
        int colorType = 2 * this.deltaNum + 1;
        int [][]refe = new int[colorType][3];
        int round = 4;//参考点的组数
        for( int loop  = 0; loop <round; loop++) {
            for (int i = 1; i <= 5; i++) {
                int[] yuv = getYUV(loop * colorType + i , 1);
                for( int j = 0 ;j < 3;j++){
                    refe[i-1][j] += yuv[j];
                }
            }
        }
        for(int i = 0; i < refe.length; i++)
            for(int j = 0; j<refe[i].length; j++)
                refe[i][j] /= round;
        return refe;
    }*/

    /**
     * 获得每行的参考色
     * @return
     */
    public int[] getRefeColor(int x, int frameIndex){
        int []refe = new int[this.deltaNum];
        if(frameIndex %2 == 0){
            for(int i =0; i < refe.length;i++)
                refe[i] = getYUV(x,1+i)[RawImage.CHANNLE_U];
        }
        else{
            for(int i =0; i < refe.length;i++)
                refe[i] = getYUV(x,1+i)[RawImage.CHANNLE_V];
        }
        return refe;
    }

    /**
     * 获得参考点颜色
     * @param x
     * @return
     */
    public int[][] getRefeColor(int x){
        int [][]refe = new int[this.deltaNum][2];
        for(int i =0; i < refe.length;i++) {
            refe[i][0] = getYUV(x, 1 + i)[RawImage.CHANNLE_U];
            refe[i][1] = getYUV(x, 1 + i)[RawImage.CHANNLE_V];
        }
        return refe;
    }

    public BitSet  getContent(){
        StringBuffer buffer = new StringBuffer();
        BitSet content = new BitSet();
        int index = 0;
        int left = MixBorderLeft + BlackBorderLenght ;
        for(int i =2;i<this.contentHeight+2; i++){
            int [][]compare = getRefeColor(i);
            for(int j = left;j<this.contentWidth+left;j++) {
                int []yuv = getYUV(i,j);
                int realx = points[i][j].getX();
                int realy = points[i][j].getY();
                int colorType = getStr(i, j, compare);
                buffer.append(colorType+",");
                switch (colorType % this.deltaNum) {
                    case 0:
                        break;
                    case 1:
                        content.set(index + 2);
                        break;
                    case 2:
                        content.set(index + 1);
                        break;
                    case 3:
                        content.set(index + 1);
                        content.set(index + 2);
                        break;
                    case 4:
                        content.set(index);
                        break;
                    case 5:
                        content.set(index);
                        content.set(index + 2);
                        break;
                    case 6:
                        content.set(index);
                        content.set(index + 1);
                        break;
                    case 7:
                        content.set(index);
                        content.set(index + 1);
                        content.set(index + 2);
                        break;
                }
                index += this.bitsPerBlock;
            }
            buffer.append("\n");
        }
        /*File file = new File(Environment.getExternalStorageDirectory(),"abc/test/compare/"+frameIndex+".txt");
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
        }*/
        //printLocation();
        int aaaaa = content.length();
        return content;

    }

    /**
     * 获得这一帧的帧序号
     */
    public void getFrameIndex() throws CRCCheckException{
        CRC8 crcCheck = new CRC8();
        int x = this.BlackBorderLenght + this.mixBorderLength;
        int y = this.BlackBorderLenght + this.contentWidth + this.MixBorderLeft;
        BitSet bitset = new BitSet();
        for(int i = 0;i < 40; i++){
            int Y = getYUV(x + i, y)[RawImage.CHANNLE_Y];
            int realx = points[x+i][y].getX();
            int realy = points[x+i][y].getY();
            if( Y > (getYUV(x + i, y + this.mixBorderLength)[RawImage.CHANNLE_Y] + 10 ))
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
        /*if(crc!=truth || index<0){
            throw CRCCheckException.getNotFoundInstance();
        }*/
        this.frameIndex = index;
    }

    public int getBlackRefe(){
        int x = this.BlackBorderLenght + this.mixBorderLength;
        int y = this.BlackBorderLenght + this.contentWidth + this.MixBorderLeft + this.mixBorderLength;
        int max = 0;
        for(int i = 0; i < 10;i++){
            int Y = getYUV(x + i, y)[RawImage.CHANNLE_Y];
            if(Y > max)
                max = Y;
        }
        return max;
    }

    /**
     * 获得头信息
     * @return
     */

    public BitSet getHead(){
        BitSet bitSet=new BitSet();
        int length = 40;
        int left = this.MixBorderLeft + this.BlackBorderLenght;
        for (int i = left; i< length + left; i++){
            int []YUV = getYUV(1,i);
            int realx = points[1][i].getX();
            int realy = points[1][i].getY();
            if(YUV[RawImage.CHANNLE_Y] > threshols[RawImage.CHANNLE_Y])
                bitSet.set(i-left);
        }
        return bitSet;
    }
    protected int realContentByteLength(){
        return frameBitNum / 8 - ecNum * ecLength / 8 - 8;
    }
    protected int RSContentByteLength(){
        return frameBitNum / 8 - ecNum * ecLength / 8;
    }
    /**
     *
     * @param x
     * @param y
     * @param compare
     * @return 返回颜色代表的index
     */
    public int getStr(int x, int y, int []compare){
        int cur = -1;
        if(this.frameIndex % 2 == 0) {
            cur = getYUV(x, y)[RawImage.CHANNLE_U];
            int result = -1;
            int dist = 500;
            for(int i = 0; i < this.deltaNum ;i++){
                if(Math.abs(cur-compare[i]) < dist){
                    dist = Math.abs(cur-compare[i]);
                    result = i;
                }
            }
            return result ;
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
            cur = getYUV(x, y)[RawImage.CHANNLE_V];
            int result = -1;
            int dist = 500;
            for(int i = 0; i < this.deltaNum ;i++){
                if(Math.abs(cur-compare[i]) < dist){
                    dist = Math.abs(cur-compare[i]);
                    result = i;
                }
            }
            return result ;
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

    /**
     * 获得小方块的颜色种类值
     * @param x
     * @param y
     * @param compare
     * @return
     */
    public int getStr(int x, int y, int [][]compare){
        int cur = -1;
        int curU = getYUV(x, y)[RawImage.CHANNLE_U];
        int curV = getYUV(x, y)[RawImage.CHANNLE_V];
        int result = -1;
        int dist = 100000;
        for(int i = 0; i < this.deltaNum ;i++){
            int stdU = compare[i][0];
            int stdV = compare[i][1];
            int diff = Math.abs(curU - stdU)* Math.abs(curU - stdU) + Math.abs(curV - stdV)* Math.abs(curV - stdV);
            if( diff < dist){
                dist = Math.abs(diff);
                result = i;
            }
        }

        return result ;



    }
    /*public int getCorrectRate(){
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

    }*/
}
