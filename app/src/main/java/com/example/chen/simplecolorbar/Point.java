package com.example.chen.simplecolorbar;

/**
 * Created by zhantong on 16/2/25.
 */
public class Point {
    public static final int CHANNLE_Y=0;
    public static final int CHANNLE_U=1;
    public static final int CHANNLE_V=2;
    int x;
    int y;
    int []YUVvalue;
    int category;
    int[] samples;
    public Point(int x,int y){
        this.x = x;
        this.y = y;
        YUVvalue = new int[3];
    }
    public  Point(int x,int y,int []value){
        this.x=x;
        this.y=y;
        this.YUVvalue=value;
    }
    public Point(int[] samples){
        this.samples=samples;
    }
    public void print(){
        System.out.println(x+" "+y+" "+YUVvalue[0]+" "+YUVvalue[1]+" "+YUVvalue[2]);
    }
    public int getX(){
        return x;
    }
    public int getY(){
        return y;
    }

}
