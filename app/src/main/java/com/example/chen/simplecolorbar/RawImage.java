package com.example.chen.simplecolorbar;

/**
 * Created by CHEN on 2017/4/27.
 */
public class RawImage {
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
    private long timestamp;

    private int[] thresholds;

    private int[] rectangle;

    private int offsetU;
    private int offsetV;
    public RawImage(){}
    public RawImage(byte[] pixels,int width,int height,int colorType){
        this(pixels,width,height,colorType,0,0);
    }
    public RawImage(byte[] pixels,int width,int height,int colorType,int index,long timestamp){
        this.pixels=pixels;
        this.width=width;
        this.height=height;
        this.colorType=colorType;
        this.index=index;
        this.timestamp=timestamp;
        thresholds=new int[3];
        offsetU=width*height;
        offsetV=width*height+width*height/4;
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
        int init = 300;
        int left = width / 2 - init;
        int right = width / 2 + init;
        int up = height / 2 - init;
        int down = height / 2 + init;
        return new int[] {left,up,right,down};
    }




}
