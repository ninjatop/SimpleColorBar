package com.example.chen.simplecolorbar;

import android.os.Environment;

import org.apache.http.util.EncodingUtils;

import java.io.File;
import java.io.FileInputStream;
import java.text.DecimalFormat;

/**
 * Created by CHEN on 2017/3/15.
 */
public class HistogramMatching {



    public boolean HistogramMatching(int []srcImg, String matchingImgPath) {
        if (srcImg == null || matchingImgPath == null) {
            return false;
        }
        int []dstImg = new int[srcImg.length];
        double[] srcCp = new double[256];

        double[] matchCp = new double[256];

        //分别计算两幅图像的累计概率分布
        getCumulativeProbabilityRGB(srcImg, matchingImgPath, srcCp, matchCp);


        double diffA = 0, diffB = 0;
        short k = 0;
        //逆映射函数
        short[] mapPixel = new short[256];

        //计算逆映射函数

        for (int i = 0; i < 256; i++) {
            diffB = 1;
            for (int j = k; j < 256; j++) {
                //找到两个累计分布函数中最相似的位置
                diffA = Math.abs(srcCp[i] - matchCp[j]);
                if (diffA - diffB < 1.0E-08) {//当两概率之差小于0.000000001时可近似认为相等
                    diffB = diffA;
                    //记录下此时的灰度级
                    k = (short)j;
                }
                else {
                    k = (short)Math.abs(j - 1);
                    break;
                }
            }
            if (k == 255) {
                for (int l = i; l < 256; l++) {
                    mapPixel[l] = k;
                }
                break;
            }
            mapPixel[i] = k;
        }
/*        for(int i = 0; i <256; i++){
            diffBR = 1;
            kR = 0;
            for(int j = 0; j <256; j++){
                diffAR = Math.abs(srcCpR[i] - matchCpR[j]);
                if(diffAR <= diffBR){
                    kR = (short)j;
                    diffBR = diffAR;
                }
            }
            mapPixelR[i] = kR;
        }*/

        //映射变换

        for (int i = 0; i < srcImg.length; i++) {
            dstImg[i] = mapPixel[srcImg[i]];
            System.out.print(dstImg[i]);
        }

        return true;
    }


    /// <summary>
/// 计算各个图像分量的累计概率分布
/// </summary>
/// <param name="srcBmp">原始图像</param>
/// <param name="cpR">R分量累计概率分布</param>
/// <param name="cpG">G分量累计概率分布</param>
/// <param name="cpB">B分量累计概率分布</param>
    public void getCumulativeProbabilityRGB(int []srcMap, String matchingPath, double[] srcCP, double []matchingCP) {
        int[] srcH = new int[256];
        int []matchingH = new int[256];
        srcH = getHistogram(srcMap);
        matchingH = getHistogram(matchingPath);
        int totalPxl = srcMap.length;
        double [] tempSrc = new double[256];
        double [] tempMatching = new double[256];
        tempSrc[0] = srcH[0];
        tempMatching[0] = matchingH[0];

        for (int i = 1; i < 256; i++) {
            tempSrc[i] = tempSrc[i - 1] + srcH[i];
            tempMatching[i] = tempMatching[i - 1] + matchingH[i];
            srcCP[i] = (tempSrc[i] / totalPxl);
            matchingCP[i] = (tempMatching[i] / totalPxl);
        }
    }

    /**
     * 返回图片数据区域的直方图数组
     * @param map
     * @return
     */
    public int [] getHistogram(int[] map) {
        int []h = new int[256];
        if (map == null)
            return null;
        for (int i = 0; i < map.length; i++) {
                h[map[i]]++;
        }
        return h;
    }

    /**
     * 从txt文本中获取参考图的直方图
     * @return
     */
    public int []getHistogram(String matchingPath){
        File file = new File(Environment.getExternalStorageDirectory(),matchingPath);
        String res="";
        int []color = new int[]{60, 100, 140, 180};
        int []h = new int[256];
        try{
            FileInputStream fin = new FileInputStream(file);
            int length = fin.available();
            byte [] buffer = new byte[length];
            fin.read(buffer);
            res = EncodingUtils.getString(buffer, "UTF-8");
            fin.close();
        }
        catch(Exception e){
            e.printStackTrace();
        }
        String []data = res.split("[,\n]");
        for(int i = 0; i < data.length; i++){
            h[color[Integer.parseInt(data[i])]]++;
        }
        return h;
    }
    public int []getRGB(int color){
        return new int[]{(color >> 16) & 0xFF, (color >> 8) & 0xFF, color & 0xFF};
    }
    public int geneRGB(int[]rgb){
        DecimalFormat df=new DecimalFormat("00000000");
        String a1 =df.format(Integer.parseInt(Integer.toBinaryString(rgb[0])));
        String b1 =df.format(Integer.parseInt(Integer.toBinaryString(rgb[1])));
        String c1 =df.format(Integer.parseInt(Integer.toBinaryString(rgb[2])));
        return Integer.valueOf(a1+b1+c1, 2);
    }
    public int geneRGB(int r, int g, int b){
        DecimalFormat df=new DecimalFormat("00000000");
        String a1 =df.format(Integer.parseInt(Integer.toBinaryString(r)));
        String b1 =df.format(Integer.parseInt(Integer.toBinaryString(g)));
        String c1 =df.format(Integer.parseInt(Integer.toBinaryString(b)));
        return Integer.valueOf(a1+b1+c1, 2);
    }
}
