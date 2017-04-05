package com.example.chen.simplecolorbar;


import android.graphics.Color;
import android.util.Log;

import java.util.BitSet;

/**
 * Created by CHEN on 2016/10/10.
 * 保存对原始图像的操作
 */

public class Matrix {
    public static final int COLOR_TYPE_YUV=1;
    public static final int COLOR_TYPE_RGB=2;
    public static final boolean VERBOSE = true;//是否记录详细log
    protected static final String TAG = "Matrix";//log tag
    protected int [] pixels;//每个像素点的原始值
    protected  int imgWidth;//图片宽度
    protected  int imgHeight;//图片高度
    protected  int [] initBorder;//初始化得到的二维码顶点坐标

    protected  int [] vertexes;//四个顶点坐标
    protected PerspectiveTransform transform;//透视变换参数
    protected int BlackBorderLenght = 1;//第二层黑色边界
    protected int mixBorderLength = 1;//调色板的边界
    //protected int contentLength = 63;//内容长度
    protected int contentWidth = 100;//内容宽度
    protected  int contentHeight = 60;//内容高度
    protected int blockSize = 8;//小方块大小
    protected Point [][] points;
    protected int deltaNum = 4;//变化的数目
    protected int bitsPerBlock = 2;//每个小方块的bit数

    int ecNum;
    int ecLength = 12;//一个symbol对应bit数目,应与RS的decoder参数保持一致
    protected double ecLevel = 0.1;//%20用来纠错
    protected int[] borders;//上一次的border坐标
    protected int frameBitNum ;//每一帧的bit总数目

    public Matrix(){
        frameBitNum = contentHeight * contentWidth * bitsPerBlock * this.deltaNum / (this.deltaNum + 1);
        ecNum = calcEcNum(ecLevel);
    }
    protected int calcEcNum(double ecLevel){
        return (int)(frameBitNum / ecLength * ecLevel);
    }
    public Matrix(int[] pixels, int imgWidth,int imgHeight,int []initBorders){
        frameBitNum = contentHeight * contentWidth * bitsPerBlock * this.deltaNum / (this.deltaNum + 1);
        ecNum = calcEcNum(ecLevel);
        this.pixels = pixels;
        this.imgWidth = imgWidth;
        this.imgHeight = imgHeight;
        this.initBorder = initBorders;
        if(initBorder == null)
            this.vertexes = findBorder(genInitBorder());
        else
            this.vertexes = findBorder(initBorder);
        perspectiveTransform();
        getRealLocation();
    }


    public int getBlackBorderLenght(){
        return this.BlackBorderLenght;
    }
    public int getMixBorderLength(){ return this.mixBorderLength;}
    //public int getContentLength(){ return this.contentLength; }
    public int getContentWidth(){return this.contentWidth;}
    public int getContentHeight(){return this.contentHeight;}
    public void sampleContent(){

    }
    public RawContent getRaw(){return null;}

    public void getContet(){

    }


    protected int realContentByteLength(){
        return frameBitNum / 8 - ecNum * ecLength / 8 - 8;
        //return 123;
    }
    protected int RSContentByteLength(){
        return frameBitNum / 8 - ecNum * ecLength / 8;
    }

    /**
     * 相对坐标系中x，y的颜色
     * @param x
     * @param y
     */
    public int[] get(int x,int y){
        return getRGB(this.points[x][y].getX(),this.points[x][y].getY());
    }
    public BitSet getHead(){
        BitSet bitSet=new BitSet();
        int length = 40;
        for (int i = 2; i< length + 2; i++){
            int []rgb = get(1,i);
            if(rgb[2] > 150)
                bitSet.set(i-2);
        }
        return bitSet;
    }

    /***
     *
     * 寻找二维码图片中的四个顶点坐标值
     *
     * @param initBorder    初始化的四个顶点坐标
     */
    public int [] findBorder(int []initBorder) {
        int left = initBorder[0];
        int up = initBorder[1];
        int right = initBorder[2];
        int down = initBorder[3];
        if(VERBOSE){
            Log.d(TAG,"border init: up:" + up + "\t" + "right:" + right + "\t" + "down:" + down + "\t" + "left:" + left);
        }
        /*if (left < 0 || right >= imgWidth || up < 0 || down >= imgHeight) {
            throw new NotFoundException("frame size too small");
        }*/
        //扩大方框的范围
        while(true){
            boolean flag = false;
            while (right < this.imgWidth && containsBlack(up, down, right, false)) {
                right++;
                flag = true;
            }
            while (down < this.imgHeight && containsBlack(left, right, down, true)) {
                down++;
                flag = true;
            }
            while (left > 0 && containsBlack(up, down, left, false)) {
                left--;
                flag = true;
            }
            while (up > 0 && containsBlack(left, right, up, true)) {
                up--;
                flag = true;
            }
            if( !flag ){
                break;
            }
        }



        if (VERBOSE) {
            Log.d(TAG, "find border: up:" + up + "\t" + "right:" + right + "\t" + "down:" + down + "\t" + "left:" + left);
        }
        /*if ((left == 0 || up == 0 || right == imgWidth || down == imgHeight) || (left == leftOrig && right == rightOrig && up == upOrig && down == downOrig)) {
            throw new NotFoundException("didn't find any possible bar code");
        }*/
        int[] vertexes = new int[8];
        left = findVertexes(up, down, left, vertexes, 0, 3, false, false);
        if (VERBOSE) {
            Log.d(TAG, "found 1 vertex,left border now is:" + left);
            Log.d(TAG, "vertexes: (" + vertexes[0] + "," + vertexes[1] + ")\t(" + vertexes[2] + "," + vertexes[3] + ")\t(" + vertexes[4] + "," + vertexes[5] + ")\t(" + vertexes[6] + "," + vertexes[7] + ")");
        }
        up = findVertexes(left, right, up, vertexes, 0, 1, true, false);

        if (VERBOSE) {
            Log.d(TAG, "found 2 vertex,up border now is:" + up);
            Log.d(TAG, "vertexes: (" + vertexes[0] + "," + vertexes[1] + ")\t(" + vertexes[2] + "," + vertexes[3] + ")\t(" + vertexes[4] + "," + vertexes[5] + ")\t(" + vertexes[6] + "," + vertexes[7] + ")");
        }
        right = findVertexes(up, down, right, vertexes, 1, 2, false, true);
        if (VERBOSE) {
            Log.d(TAG, "found 3 vertex,right border now is:" + right);
            Log.d(TAG, "vertexes: (" + vertexes[0] + "," + vertexes[1] + ")\t(" + vertexes[2] + "," + vertexes[3] + ")\t(" + vertexes[4] + "," + vertexes[5] + ")\t(" + vertexes[6] + "," + vertexes[7] + ")");
        }
        down = findVertexes(left, right, down, vertexes, 3, 2, true, true);
        if (VERBOSE) {
            Log.d(TAG, "found 4 vertex,down border now is:" + down);
        }
        if (VERBOSE) {
            Log.d(TAG, "vertexes: (" + vertexes[0] + "," + vertexes[1] + ")\t(" + vertexes[2] + "," + vertexes[3] + ")\t(" + vertexes[4] + "," + vertexes[5] + ")\t(" + vertexes[6] + "," + vertexes[7] + ")");
        }
        if (vertexes[0] == 0 || vertexes[2] == 0 || vertexes[4] == 0 || vertexes[6] == 0) {
            //throw new NotFoundException("vertexes error");
        }
        borders=new int[]{left,up,right,down};
        return vertexes;
    }







    /**
     * 定位到最中心
     */
    public int [] genInitBorder(){
        int init1 = 90;
        int init2 = 50;
        int left=imgWidth / 2 - init1;
        int right=imgWidth / 2 + init1;
        //int right=2210;
        int up = imgHeight / 2 - init2;
        int down = imgHeight / 2 + init2;
        this.initBorder = new int[]{left,up,right,down};
        return this.initBorder;
    }

    /**
     *  确定边界时需要
     * 判断指定线段中像素是否全部为白色
     * 以下参数可以唯一确定一条线段
     *
     * @param start      线段起点
     * @param end        线段终点
     * @param fixed      线段保持不变的坐标值
     * @param horizontal 线段是否为水平
     * @return 若线段包含有黑点, 则返回true, 否则返回false
     */
    public boolean containsBlack(int start, int end, int fixed, boolean horizontal){
        if(horizontal){
            for(int x = start; x < end; x++){
                if(pixelIsBlack(x, fixed))
                    return  true;
            }
        }
        else{
            for(int y = start; y < end; y++){
                if(pixelIsBlack(fixed, y))
                    return true;
            }
        }
        return false;
    }

    /**
     * 从数组中获得一个点的像素值
     * @param x
     * @param y
     * @return 一个 rgb组成的数组
     */
    protected int [] getRGB(int x, int y){
        int offset = y * this.imgWidth + x;
        int color = this.pixels[offset];
        int r = Color.red(color);
        int g = Color.green(color);
        int b = Color.blue(color);
        return new int []{b,g,r};///储存的时候RGB是反向存的，可能是之前保存为数组的时候反向了
    }

    /**
     * 判断一个点是否是黑色
     * 100是预先设定好的一个阈值
     * @param x
     * @param y
     * @return
     */
    protected boolean pixelIsBlack(int x,int y){
        int [] RGB = getRGB(x, y);
        //if( RGB[0] < 100 && RGB[1] < 100 || RGB[0] < 100  && RGB[2] < 100 ||  RGB[1] < 100 && RGB[2] <100 )
        //if( RGB[0] < 100 && RGB[1] < 100 && RGB[2] < 150 )
        if( RGB[2] < 150   )
            return true;
        return false;
    }

    /**
     * 寻找矩形内二维码顶点坐标
     * 方法在findBorder()中已经描述
     *
     * @param b1         较小边界坐标值
     * @param b2         较大边界坐标值
     * @param fixed      需要移动边界,在边界线段上不变的坐标值
     * @param vertexs    存储寻找到的顶点坐标
     * @param p1         可能的顶点编号
     * @param p2         可能的顶点编号
     * @param horizontal 此边是否为水平
     * @param sub        此边的移动方向,即对fix加还是减
     * @return 返回收缩后的矩形边界fixed值
     *
     */
    private int findVertexes(int b1, int b2, int fixed, int[] vertexs, int p1, int p2, boolean horizontal, boolean sub) /*throws NotFoundException */{
        int mid = (b2 - b1) / 2;
        //int mid = (b2 - b1);
        boolean checkP1 = vertexs[p1 * 2] == 0;
        boolean checkP2 = vertexs[p2 * 2] == 0;

        if (horizontal) {
            while (true) {
                if (checkP1) {
                    for (int i = 1; i <= mid; i++) {
                        if (pixelIsBlack(b1 + i, fixed) && !isNoisePoint(b1 + i, fixed)) {
                            vertexs[p1 * 2] = b1 + i;
                            vertexs[p1 * 2 + 1] = fixed;
                            return fixed;
                        }
                    }
                }
                if (checkP2) {
                    for (int i = 1; i <= mid; i++) {
                        if (pixelIsBlack(b2 - i, fixed) && !isNoisePoint(b2 - i, fixed)) {
                            vertexs[p2 * 2] = b2 - i;
                            vertexs[p2 * 2 + 1] = fixed;
                            return fixed;
                        }
                    }
                }
                if (sub) {
                    fixed--;
                    if (fixed <= 0) {
                        //throw new NotFoundException("didn't find any possible bar code");
                    }
                } else {
                    fixed++;
                    if (fixed >= imgHeight) {
                        //throw new NotFoundException("didn't find any possible bar code");
                    }
                }
            }
        } else {
            while (true) {
                if (checkP1) {
                    for (int i = 1; i <= mid; i++) {
                        if (pixelIsBlack(fixed, b1 + i) && !isNoisePoint(fixed, b1 + i)) {
                            vertexs[p1 * 2] = fixed;
                            vertexs[p1 * 2 + 1] = b1 + i;
                            return fixed;
                        }
                    }
                }
                if (checkP2) {
                    for (int i = 1; i <= mid; i++) {
                        if (pixelIsBlack(fixed, b2 - i) && !isNoisePoint(fixed, b2 - i)) {
                            vertexs[p2 * 2] = fixed;
                            vertexs[p2 * 2 + 1] = b2 - i;
                            return fixed;
                        }
                    }
                }
                if (sub) {
                    fixed--;
                    if (fixed <= 0) {
                        //throw new NotFoundException("didn't find any possible bar code");
                    }
                } else {
                    fixed++;
                    if (fixed >= imgWidth) {
                        //throw new NotFoundException("didn't find any possible bar code");
                    }
                }
            }
        }
    }
    /**
     * 判断此点是否为噪点
     * 若此点一定是黑点,通过判断周围8个像素点,是否有大于5个像素点为黑色
     * 即中值滤波
     *
     * @param x x轴,即列
     * @param y y轴,即行
     * @return 为噪点则返回true, 否则返回true
     */
    private boolean isNoisePoint(int x, int y) {
        int sum = 0;
        sum += pixelIsBlack(x - 1, y - 1) ? 1 : 0;
        sum += pixelIsBlack(x - 1, y) ? 1 : 0;
        sum += pixelIsBlack(x - 1, y + 1) ? 1 : 0;
        sum += pixelIsBlack(x, y - 1) ? 1 : 0;
        sum += pixelIsBlack(x, y) ? 1 : 0;
        sum += pixelIsBlack(x, y + 1) ? 1 : 0;
        sum += pixelIsBlack(x + 1, y-1) ? 1 : 0;
        sum += pixelIsBlack(x + 1, y) ? 1 : 0;
        sum += pixelIsBlack(x + 1, y + 1) ? 1 : 0;
        return sum < 5;
    }

    protected int getBarCodeWidth(){
        return (BlackBorderLenght + mixBorderLength) * 2 + contentWidth;
    }
    protected int getBarCodeHeight(){
        return (BlackBorderLenght + mixBorderLength) * 2 + contentHeight;
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
    public Point[][] getRealLocation(){
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
        return this.points;

    }
    /*public Point[][] getPoints(){
        return points;
    }*/

}
