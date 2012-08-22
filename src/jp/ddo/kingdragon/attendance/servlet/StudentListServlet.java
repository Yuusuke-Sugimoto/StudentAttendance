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

public class StudentListServlet extends HttpServlet {
    // 定数の宣言
    /**
     * シリアルバージョンUID
     */
    private static final long serialVersionUID = 823662399690987801L;
    /**
     * パスワード
     */
    private static final String PASSWORD = "test1234";

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
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
        pw.println("<title>確認状況</title>");
        pw.println("</head>");
        pw.println("<body>");
        pw.println("<p>このページの表示にはパスワードの入力が必要です。</p>");
        pw.println("</body>");
        pw.println("</html>");
        pw.close();
    }

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String passwd = request.getParameter("passwd");

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
        pw.println("<title>確認状況</title>");
        pw.println("</head>");
        pw.println("<body>");
        pw.println("<p>");
        if (passwd != null && passwd.equals(StudentListServlet.PASSWORD)) {
            AttendanceSheet mAttendanceSheet = DisasterModeActivity.getAttendanceSheet();
            if (mAttendanceSheet.size() != 0) {
                pw.println("確認状況<br />");
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
                for (Attendance mAttendance : mAttendanceSheet.getAttendanceDisplayData()) {
                    long timeStamp = mAttendance.getTimeStamp();
                    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
                    String photoPath = mAttendance.getExtra(Attendance.PHOTO_PATH, null);
                    String moviePath = mAttendance.getExtra(Attendance.MOVIE_PATH, null);
                    pw.println("<tr>");
                    pw.println("<td>" + mAttendance.getStudentNum() + "</td>");
                    pw.println("<td>" + mAttendance.getClassName() + "</td>");
                    pw.println("<td>" + mAttendance.getStudentNo() + "</td>");
                    pw.println("<td>" + mAttendance.getStudentName() + "</td>");
                    pw.println("<td>" + mAttendance.getStudentRuby() + "</td>");
                    pw.println("<td>");
                    if (mAttendance.getStatus() != Attendance.ABSENCE) {
                        pw.println("確認済");
                    }
                    pw.println("</td>");
                    pw.println("<td>" + dateFormat.format(new Date(timeStamp)) + "</td>");
                    pw.println("<td>");
                    if (mAttendance.getLatitude() != -1.0) {
                        pw.println(mAttendance.getLatitude());
                    }
                    pw.println("</td>");
                    pw.println("<td>");
                    if (mAttendance.getLongitude() != -1.0) {
                        pw.println(mAttendance.getLongitude());
                    }
                    pw.println("</td>");
                    pw.println("<td>");
                    if (mAttendance.getAltitude() != -1.0) {
                        pw.println(mAttendance.getAltitude());
                    }
                    pw.println("</td>");
                    pw.println("<td>");
                    if (mAttendance.getAccuracy() != -1.0f) {
                        pw.println(mAttendance.getAccuracy());
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
                }
                pw.println("</table>");
            }
            else {
                pw.println("表示する内容がありません。");
            }
        }
        else {
            pw.println("パスワードが間違っています。");
        }
        pw.println("</p>");
        pw.println("</body>");
        pw.println("</html>");
        pw.close();
    }
}