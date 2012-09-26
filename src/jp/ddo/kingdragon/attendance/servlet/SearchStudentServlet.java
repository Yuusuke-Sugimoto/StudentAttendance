package jp.ddo.kingdragon.attendance.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import jp.ddo.kingdragon.attendance.DisasterModeActivity;
import jp.ddo.kingdragon.attendance.student.Attendance;
import jp.ddo.kingdragon.attendance.student.AttendanceSheet;

/**
 * 学生の検索を行うサーブレット
 * @author 杉本祐介
 */
public class SearchStudentServlet extends HttpServlet {
    // 定数の宣言
    /**
     * シリアルバージョンUID
     */
    private static final long serialVersionUID = -7540049868673003013L;

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String name = request.getParameter("name");

        response.setContentType("text/html");
        response.setCharacterEncoding("UTF-8");
        PrintWriter pw = response.getWriter();
        pw.println("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">");
        pw.println("<html xmlns=\"http://www.w3.org/1999/xhtml\" xml:lang=\"ja\">");
        pw.println("<head>");
        pw.println("<meta http-equiv=\"content-type\" content=\"text/html; charset=UTF-8\" />");
        pw.println("<meta http-equiv=\"content-style-type\" content=\"text/css\" />");
        pw.println("<link rel=\"stylesheet\" type=\"text/css\" href=\"style.css\" />");
        pw.println("");
        pw.println("<title>検索結果</title>");
        pw.println("</head>");
        pw.println("<body>");
        pw.println("<p>");
        if (name != null) {
            if (!name.matches(".*(\\[|\\]|\\-|\\(|\\)|\\||\\^|\\$|\\.|\\*|\\+|\\?|\\\\).*")) {
                Attendance target = null;
                AttendanceSheet mAttendanceSheet = DisasterModeActivity.getAttendanceSheet();
                for (Attendance mAttendance : mAttendanceSheet.getAttendanceDisplayData()) {
                    String studentName = mAttendance.getStudentName().toLowerCase();
                    String matchPattern = "^" + name.toLowerCase() + ".*";
                    if (studentName.matches(matchPattern) || studentName.replaceAll(" ", "").matches(matchPattern)) {
                        target = mAttendance;
                    }
                }

                pw.println("検索結果<br />");
                if (target != null) {
                    pw.println("<table border=\"1\">");
                    pw.println("<tr>");
                    pw.println("<th>連番</th>");
                    pw.println("<th>所属</th>");
                    pw.println("<th>学籍番号</th>");
                    pw.println("<th>氏名</th>");
                    pw.println("<th>カナ</th>");
                    pw.println("<th>状態</th>");
                    pw.println("<th>確認日時</th>");
                    pw.println("<th>緯度</th>");
                    pw.println("<th>経度</th>");
                    pw.println("<th>高度</th>");
                    pw.println("<th>精度</th>");
                    pw.println("<th>その他</th>");
                    pw.println("</tr>");
                    long timeStamp = target.getTimeStamp();
                    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
                    String photoPath = target.getExtra(Attendance.PHOTO_PATH, null);
                    String moviePath = target.getExtra(Attendance.MOVIE_PATH, null);
                    pw.println("<tr>");
                    pw.println("<td>" + target.getStudentNum() + "</td>");
                    pw.println("<td>" + target.getClassName() + "</td>");
                    pw.println("<td>" + target.getStudentNo() + "</td>");
                    pw.println("<td>" + target.getStudentName() + "</td>");
                    pw.println("<td>" + target.getStudentRuby() + "</td>");
                    pw.println("<td>");
                    if (target.getStatus() != Attendance.ABSENCE) {
                        pw.println("確認済");
                    }
                    pw.println("</td>");
                    pw.println("<td>" + dateFormat.format(new Date(timeStamp)) + "</td>");
                    pw.println("<td>");
                    if (target.getLatitude() != -1.0) {
                        pw.println(target.getLatitude());
                    }
                    pw.println("</td>");
                    pw.println("<td>");
                    if (target.getLongitude() != -1.0) {
                        pw.println(target.getLongitude());
                    }
                    pw.println("</td>");
                    pw.println("<td>");
                    if (target.getAltitude() != -1.0) {
                        pw.println(target.getAltitude());
                    }
                    pw.println("</td>");
                    pw.println("<td>");
                    if (target.getAccuracy() != -1.0f) {
                        pw.println(target.getAccuracy());
                    }
                    pw.println("</td>");
                    pw.println("<td>");
                    if (photoPath != null) {
                        pw.println("<a href=\"" + photoPath + "\" title=\"写真\">写真</a>");
                    }
                    if (moviePath != null) {
                        pw.println("<a href=\"ShowMovie?path=" + moviePath + "\" title=\"動画\">動画</a>");
                    }
                    pw.println("</td>");
                    pw.println("</tr>");
                    pw.println("</table>");
                }
                else {
                    pw.println("該当する学生が見つかりません。");
                }
            }
            else {
                pw.println("氏名に使用できない文字が含まれています。");
            }
        }
        else {
            pw.println("氏名が指定されていません。");
        }
        pw.println("</p>");
        pw.println("</body>");
        pw.println("</html>");
        pw.close();
    }
}