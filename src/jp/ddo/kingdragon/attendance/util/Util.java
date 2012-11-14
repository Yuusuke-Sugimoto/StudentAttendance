package jp.ddo.kingdragon.attendance.util;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;

import java.io.File;
import java.io.IOException;

public class Util {
    private Util() {}

    /**
     * バイト配列を16進数表現の文字列に変換する<br>
     * 参考:16進数文字列(String)⇔バイト配列(byte[]) - lambda {|diary| lambda { diary.succ! } }.call(hatena)<br>
     *      http://d.hatena.ne.jp/winebarrel/20041012/p1
     * @param bytes バイト配列
     * @return 16進数表現の文字列
     */
    public static String byteArrayToHexString(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();

        for (byte b : bytes) {
            // 下位8ビットのみ取り出す
            int bottomByte = b & 0xff;
            if (bottomByte < 0x10) {
                // 10未満の場合は0を付加
                hexString.append("0");
            }
            hexString.append(Integer.toHexString(bottomByte));
        }

        return hexString.toString().toUpperCase();
    }

    /**
     * 画像を回転させる
     * @param srcBitmap 元となる画像のBitmap
     * @param rotation 回転角度
     * @return 回転させた画像のBitmap
     * @throws IOException
     */
    public static Bitmap rotateBitmap(Bitmap srcBitmap, float rotation) {
        Matrix mMatrix = new Matrix();
        mMatrix.postRotate(rotation);
        Bitmap retBitmap = Bitmap.createBitmap(srcBitmap, 0, 0, srcBitmap.getWidth(), srcBitmap.getHeight(), mMatrix, true);

        return retBitmap;
    }

    /**
     * 画像を回転させる<br>
     * 画像のサイズが大きければ縮小も行う
     * @param inFile 元となる画像ファイル
     * @return 回転させた画像のBitmap
     * @throws IOException
     */
    public static Bitmap rotateImage(File inFile) throws IOException {
        /**
         * 最大サイズを超える画像の場合、縮小して読み込み
         * 参考:AndroidでBitmapFactoryを使ってサイズの大きな画像を読み込むサンプル - hoge256ブログ
         *      http://www.hoge256.net/2009/08/432.html
         *
         * 画像を回転させる
         * 参考:Androidでカメラ撮影し画像を保存する方法 - DRY（日本やアメリカで働くエンジニア日記）
         *      http://d.hatena.ne.jp/ke-16/20110712/1310433427
         */
        BitmapFactory.Options mOptions = new BitmapFactory.Options();
        mOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(inFile.getAbsolutePath(), mOptions);
        int widthScale  = (int)Math.ceil((double)mOptions.outWidth / (double)PreferenceUtil.DEFAULT_SIZE_WIDTH);
        int heightScale = (int)Math.ceil((double)mOptions.outHeight / (double)PreferenceUtil.DEFAULT_SIZE_HEIGHT);
        int scale = Math.max(widthScale, heightScale);

        mOptions.inJustDecodeBounds = false;
        mOptions.inSampleSize = scale;
        Bitmap srcBitmap = BitmapFactory.decodeFile(inFile.getAbsolutePath(), mOptions);

        // 画像の向きを検出する
        // 画像を正しい向きに修正するためにパラメータを設定
        int orientation = 0;
        ExifInterface mExifInterface = new ExifInterface(inFile.getAbsolutePath());
        orientation = mExifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, 0);

        float rotation = 0.0f;
        switch (orientation) {
            case ExifInterface.ORIENTATION_ROTATE_180: {
                rotation = 180.0f;

                break;
            }
            case ExifInterface.ORIENTATION_ROTATE_90: {
                rotation = 90.0f;

                break;
            }
            case ExifInterface.ORIENTATION_ROTATE_270: {
                rotation = 270.0f;

                break;
            }
        }

        return rotateBitmap(srcBitmap, rotation);
    }
}