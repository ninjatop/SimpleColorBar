package com.example.chen.simplecolorbar;

/**
 * Created by zhantong on 16/4/24.
 */
public class MatrixFactory {
    public static Matrix createMatrix(BarcodeFormat barcodeFormat,byte[] pixels,int imgColorType, int imgWidth, int imgHeight,int[] initBorder) throws NotFoundException{
        Matrix matrix=null;
        switch (barcodeFormat){
            case NORMAL:
                matrix=new MatrixNormal();
                break;
            case ZOOM:
                matrix=new MatrixZoom();
                break;
            case ZOOMVARY:
                matrix=new MatrixZoomVary();
                break;
            case ZOOMVARYALT:
                matrix=new MatrixZoomVaryAlt();
        }
        return matrix;
    }
    public static Matrix createMatrix(BarcodeFormat barcodeFormat){
        Matrix matrix=null;
        switch (barcodeFormat){
            case NORMAL:
                matrix=new MatrixNormal();
                break;
            case ZOOM:
                matrix=new MatrixZoom();
                break;
            case ZOOMVARY:
                matrix=new MatrixZoomVary();
                break;
            case ZOOMVARYALT:
                matrix=new MatrixZoomVaryAlt();
                break;
        }
        return matrix;
    }
}
