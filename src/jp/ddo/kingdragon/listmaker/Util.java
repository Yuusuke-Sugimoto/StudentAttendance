package jp.ddo.kingdragon.listmaker;

public class Util {
    private Util() {}

    /**
     * バイト配列を16進数表現の文字列にして返す<br />
     * 参考:16進数文字列(String)⇔バイト配列(byte[]) - lambda {|diary| lambda { diary.succ! } }.call(hatena)<br />
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
            hexString.append(Integer.toHexString(bottomByte).toUpperCase());
        }

        return hexString.toString();
    }
}