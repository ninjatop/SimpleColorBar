package com.example.chen.simplecolorbar;

import android.util.Log;

import java.util.Arrays;

/**
 * Created by CHEN on 2017/4/27.
 */
public class RawImage {
    public static final boolean VERBOSE = true;//是否记录详细log
    protected static final String TAG = "RawImage";//log tag
    public static final int COLOR_TYPE_YUV=0;
    public static final int COLOR_TYPE_RGB=1;

    public static final int CHANNLE_R=0;
    public static final int CHANNLE_G=1;
    public static final int CHANNLE_B=2;
    public static final int CHANNLE_Y=0;
    public static final int CHANNLE_U=1;
    public static final int CHANNLE_V=2;
    private byte[] pixels;
    private int width;
    private int height;
    private int colorType;
    private int index;


    private int[] thresholds;

    private int[] rectangle;

    private int offsetU;
    private int offsetV;
    public RawImage(){}
    public RawImage(byte[] pixels,int width,int height,int colorType){
        this(pixels,width,height,colorType,0);
    }
    public RawImage(byte[] pixels,int width,int height,int colorType,int index){
        this.pixels=pixels;
        this.width=width;
        this.height=height;
        this.colorType=colorType;
        this.index=index;
        thresholds = new int[3];
        thresholds[0] = 120;//这里是定义了YUV的初始阈值
        thresholds[1] = 120;
        thresholds[2] = 120;
        //initThreshold();
        offsetU = width * height;
        offsetV = width * height + width * height/4;
        int a = 2;
    }
    public int getPixel(int x,int y,int channel){
        switch (channel){
            case CHANNLE_Y:
                return pixels[y * width + x] & 0xff;
            case CHANNLE_U:
                return pixels[offsetU+y/2*(width/2)+x/2] & 0xff;
            case CHANNLE_V:
                return pixels[offsetV+y/2*(width/2)+x/2] & 0xff;
            default:
                throw new IllegalArgumentException();
        }
    }
    private int[] genInitBorder(){
        int init = 200;
        int left = width / 2 - init;
        int right = width / 2 + init;
        int up = height / 2 - init;
        int down = height / 2 + init;
        return new int[] {left,up,right,down};
    }
    public void initThreshold(){
        thresholds = new int[3];
        int startx = (int)(this.width * 0.2);
        int endx = this.width - startx;
        int y1 = (int)(this.height * 0.3);
        int y2 = y1 *2;
        int []sum = new int[]{0,0,0};
        int count = 0;
        for(int x = startx; x < endx; x+=3){
            for(int i =0; i< 3;i++){
                sum[i] += getPixel(x,y1,i);
                sum[i] += getPixel(x,y2,i);
            }
            count ++;
        }
        for(int i = 0; i <3; i++){
            thresholds[i] = sum[i]/count/2;
        }
        thresholds[0] += 20;
        thresholds[1] += 20;
        thresholds[2] += 20;

    }
    public int[] getBarcodeVertexes(int[] initRectangle,int channel) throws NotFoundException{
        int[] whiteRectangle=findWhiteRectangle(initRectangle,channel);
        System.out.println("white rectangle: "+ Arrays.toString(whiteRectangle));
        rectangle = whiteRectangle;
        //int[]whiteRectangle=findWhiteRectangle1(null);
        //return findVertexesFromWhiteRectangle1(whiteRectangle);
        return findVertexesFromWhiteRectangle(whiteRectangle);
        //return findVertexesFromWhiteRectangle3(whiteRectangle);
    }
    public byte[] getPixels(){
        return pixels;
    }
    private int[] findWhiteRectangle(int[] initRectangle,int channel) throws NotFoundException {
        if(initRectangle==null){
            initRectangle=genInitBorder();
        }
        int left=initRectangle[0];
        int up=initRectangle[1];
        int right=initRectangle[2];
        int down=initRectangle[3];
        int leftOrig = left;
        int rightOrig = right;
        int upOrig = up;
        int downOrig = down;
        if (left < 0 || right >= width || up < 0 || down >= height) {
            throw new NotFoundException("frame size too small");
        }
        boolean flag;
        while (true) {
            flag = false;
            while (right < width && contains(up, down, right, false,0)) {
                right++;
                flag = true;

            }
            while (down < height && contains(left, right, down, true/*,channel*/,0)) {
                down++;
                flag = true;
            }
            while (left > 0 && contains(up, down, left, false/*,channel*/,0)) {
                left--;
                flag = true;
            }
            while (up > 0 && contains(left, right, up, true/*,channel*/,0)) {
                up--;
                flag = true;
            }
            if (!flag) {
                break;
            }
        }
        if ((left == 0 || up == 0 || right == width || down == height) || (left == leftOrig && right == rightOrig && up == upOrig && down == downOrig)) {
             throw new NotFoundException("didn't find any possible bar code: "+left+" "+up+" "+right+" "+down);
        }
        return new int[]{left,up,right,down};
    }
    private int[] findVertexesFromWhiteRectangle(int[] whiteRectangle){
        int channel=0;
        boolean greater=true;
        int left=whiteRectangle[0];
        int up=whiteRectangle[1];
        int right=whiteRectangle[2];
        int down=whiteRectangle[3];
        System.out.println(left+" "+up+" "+right+" "+down);

        int[] vertexes=new int[8];
        int length=Math.min(right-left,down-up);
        boolean flag=false;
        for(int startX=left,startY=up;startY-up<length;startY++){
            for(int currentX=startX,currentY=startY;currentY>=up;currentX++,currentY--){
                if(pixelEquals(currentX,currentY,0,0)/*&&pixelEquals(currentX,currentY,1,1)&&pixelEquals(currentX,currentY,2,1)*/){
                    vertexes[0]=currentX;
                    vertexes[1]=currentY;
                    flag=true;
                    break;
                }
            }
            if(flag){
                break;
            }
        }
        flag=false;
        for(int startX=right,startY=up;right-startX<length;startX--){
            for(int currentX=startX,currentY=startY;currentX<=right;currentX++,currentY++){
                if(pixelEquals(currentX,currentY,0,0)/*&&pixelEquals(currentX,currentY,1,1)&&pixelEquals(currentX,currentY,2,1)*/){
                    vertexes[2]=currentX;
                    vertexes[3]=currentY;
                    flag=true;
                    break;
                }
            }
            if(flag){
                break;
            }
        }
        flag=false;
        for(int startX=right,startY=down;right-startX<length;startX--){
            for(int currentX=startX,currentY=startY;currentX<=right;currentX++,currentY--){
                if(pixelEquals(currentX,currentY,0,0)/*&&pixelEquals(currentX,currentY,1,1)&&pixelEquals(currentX,currentY,2,1)*/){
                    vertexes[4]=currentX;
                    vertexes[5]=currentY;
                    flag=true;
                    break;
                }
            }
            if(flag){
                break;
            }
        }
        flag=false;
        for(int startX=left,startY=down;down-startY<length;startY--){
            for(int currentX=startX,currentY=startY;currentY<=down;currentX++,currentY++){
                if(pixelEquals(currentX,currentY,0,0)/*&&pixelEquals(currentX,currentY,1,1)&&pixelEquals(currentX,currentY,2,1)*/){
                    vertexes[6]=currentX;
                    vertexes[7]=currentY;
                    flag=true;
                    break;
                }
            }
            if(flag){
                break;
            }
        }
        if (VERBOSE) {
            Log.d(TAG, "vertexes: (" + vertexes[0] + "," + vertexes[1] + ")\t(" + vertexes[2] + "," + vertexes[3] + ")\t(" + vertexes[4] + "," + vertexes[5] + ")\t(" + vertexes[6] + "," + vertexes[7] + ")");
        }
        return vertexes;
    }
    private boolean isSinglePoint(int x,int y,int channel){
        int countSame=0;
        int value=getBinary(x,y,channel);
        for(int i=-1;i<2;i++){
            for(int j=-1;j<2;j++){
                int get=getBinary(x+i,y+j,channel);
                if(value==get){
                    countSame++;
                }
            }
        }
        return countSame<=2;
    }
    private boolean contains(int start, int end, int fixed, boolean horizontal,int shouldBe) {
        if (horizontal) {
            for (int x = start; x <= end; x++) {
                if(pixelEquals(x,fixed,0,shouldBe)||pixelEquals(x,fixed,1,shouldBe)||pixelEquals(x,fixed,2,shouldBe)){
                    return true;
                }
            }
        } else {
            for (int y = start; y <= end; y++) {
                if(pixelEquals(fixed,y,0,shouldBe)||pixelEquals(fixed,y,1,shouldBe)||pixelEquals(fixed,y,2,shouldBe)){
                    return true;
                }
            }
        }
        return false;
    }
    private boolean contains(int start, int end, int fixed, boolean horizontal,int channel,int shouldBe) {
        if (horizontal) {
            for (int x = start; x <= end; x++) {
                if(pixelEquals(x,fixed,channel,shouldBe)&&pixelEquals(x+1,fixed,channel,shouldBe)&&pixelEquals(x-1,fixed,channel,shouldBe)&&pixelEquals(x+2,fixed,channel,shouldBe)&&pixelEquals(x-2,fixed,channel,shouldBe)){
                    return true;
                }
            }
        } else {
            for (int y = start; y <= end; y++) {
                if(pixelEquals(fixed,y,channel,shouldBe)&&pixelEquals(fixed,y+1,channel,shouldBe)&&pixelEquals(fixed,y-1,channel,shouldBe)&&pixelEquals(fixed,y+2,channel,shouldBe)&&pixelEquals(fixed,y-2,channel,shouldBe)){
                    return true;
                }
            }
        }
        return false;
    }
    private boolean pixelEquals(int x, int y,int channel, int pixel){
        int a = getBinary(x,y,channel);
        return getBinary(x,y,channel)==pixel;
    }
    public int getBinary(int x,int y,int channel){
        int value = getPixel(x,y,channel);
        int a = thresholds[channel];
        if(value >= a){
            return 1;
        }else{
            return 0;
        }
    }
    public int getWidth(){
        return this.width;
    }
    public int getHeight(){return this.height; }
    public int[]getThresholds(){return this.thresholds;}
    public int[]getRectangle(){return this.rectangle;}
    @Override
    public String toString() {
        return width+"x"+height+" color type "+colorType+" index "+index;
    }




}
