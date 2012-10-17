package jp.ddo.kingdragon.attendance;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.Serializable;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;

import android.util.Log;

import jp.ddo.kingdragon.attendance.student.Attendance;

/**
 * サーバに送信する出席データを管理するクラス
 * @author 杉本祐介
 */
public class SendAttendanceQueue implements Iterable<Attendance>, Serializable {
    // 定数の宣言
    /**
     * シリアルバージョンUID
     */
    private static final long serialVersionUID = 6767596055879392050L;

    // 変数の宣言
    /**
     * 一時停止中かどうか
     */
    private boolean isPaused;
    /**
     * 送信中かどうか
     */
    private boolean isSending;

    /**
     * 送信先のアドレス
     */
    private String serverAddress;
    /**
     * 文字コード
     */
    private String characterCode;
    /**
     * 何ミリ秒でタイムアウトにするか
     */
    private int timeout;
    /**
     * 送信中の出席データ
     */
    private Attendance sendingAttendance;

    /**
     * 送信待ちの出席データのキュー
     */
    private LinkedList<Attendance> queue;

    // コンストラクタ
    /**
     * 送信先のアドレスと文字コード、タイムアウトの時間を指定してインスタンスを生成する
     * @param serverAddress 送信先のアドレス
     * @param characterCode 文字コード
     * @param timeout タイムアウト(ミリ秒)
     */
    public SendAttendanceQueue(String serverAddress, String characterCode, int timeout) {
        isPaused = false;
        isSending = false;
        this.serverAddress = serverAddress;
        this.characterCode = characterCode;
        this.timeout = timeout;
        sendingAttendance = null;
        queue = new LinkedList<Attendance>();
    }

    @Override
    public Iterator<Attendance> iterator() {
        return queue.iterator();
    }

    /**
     * 送信中かどうかを返す
     * @return 送信中ならばtrue そうでなければfalse
     */
    public boolean isSending() {
        return isSending;
    }

    /**
     * 送信中の出席データを返す
     * @return 送信中の出席データ 送信中でなければnull
     */
    public Attendance getSendingAttendance() {
        return sendingAttendance;
    }

    /**
     * 待機中の出席データを削除する
     */
    public void removeWaitingAttendances() {
        queue.clear();
    }

    /**
     * 送信を一時停止する
     */
    public void pause() {
        isPaused = true;
    }
    /**
     * 送信を再開する
     */
    public void resume() {
        isPaused = false;
        execute();
    }

    /**
     * キューに出席データを追加する
     * @param inAttendance 出席データ
     */
    public void enqueue(Attendance inAttendance) {
        queue.offer(inAttendance);
        if (!isPaused && !isSending) {
            execute();
        }
    }

    /**
     * 出席データを送信する
     */
    private void execute() {
        sendingAttendance = queue.poll();
        if (sendingAttendance != null) {
            isSending = true;
            sendAttendance(sendingAttendance);
        }
        else {
            isSending = false;
        }
    }

    /**
     * 出席データを送信する処理
     */
    private void sendAttendance(final Attendance inAttendance) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                HttpURLConnection connection = null;
                PrintStream ps = null;
                BufferedReader br = null;
                /***
                 * POST送信
                 * 参考:挫折から始まる Androidアプリ開発日誌～ときどきJava Tips etc...～  Androidでhttp通信をしてみよう（HttpUrlConnectionによるPOST編）
                 *      http://yukimura1227.blog.fc2.com/blog-entry-36.html
                 */
                try {
                    URL mUrl = new URL(serverAddress);
                    connection = (HttpURLConnection)mUrl.openConnection();
                    connection.setRequestMethod("POST");
                    connection.setDoOutput(true);
                    connection.setConnectTimeout(timeout);

                    SimpleDateFormat format = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
                    ps = new PrintStream(connection.getOutputStream());
                    ps.print("number="       + URLEncoder.encode(String.valueOf(inAttendance.getStudentNum()), characterCode)
                             + "&belong="    + URLEncoder.encode(inAttendance.getClassName(), characterCode)
                             + "&id="        + URLEncoder.encode(inAttendance.getStudentNo(), characterCode)
                             + "&name="      + URLEncoder.encode(inAttendance.getStudentName(), characterCode)
                             + "&kana="      + URLEncoder.encode(inAttendance.getStudentRuby(), characterCode)
                             + "&safety="    + URLEncoder.encode(inAttendance.getStatusString(), characterCode)
                             + "&time="      + URLEncoder.encode(format.format(new Date(inAttendance.getTimeStamp())), characterCode)
                             + "&latitude="  + URLEncoder.encode(String.valueOf(inAttendance.getLatitude()), characterCode)
                             + "&longitude=" + URLEncoder.encode(String.valueOf(inAttendance.getLongitude()), characterCode));

                    br = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    while (br.readLine() != null) {}
                }
                catch (IOException e) {
                    queue.offer(inAttendance);

                    Log.e("sendAttendance", e.getMessage(), e);
                }
                finally {
                    if (connection != null) {
                        connection.disconnect();
                    }
                    if (ps != null) {
                        ps.flush();
                        ps.close();
                    }
                    if (br != null) {
                        try {
                            br.close();
                        }
                        catch (IOException e) {
                            Log.e("sendAttendance", e.getMessage(), e);
                        }
                    }
                    if (!isPaused) {
                        execute();
                    }
                }
            }
        }).start();
    }
}