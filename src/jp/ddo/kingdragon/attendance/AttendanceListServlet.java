package jp.ddo.kingdragon.attendance;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import jp.ddo.kingdragon.attendance.student.Attendance;
import jp.ddo.kingdragon.attendance.student.AttendanceSheet;

public class AttendanceListServlet extends HttpServlet {
    /**
     * シリアルバージョンUID
     */
    private static final long serialVersionUID = 823662399690987801L;

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        AttendanceSheet mAttendanceSheet = DisasterModeActivity.getAttendanceSheet();

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
        if (mAttendanceSheet.size() != 0) {
            pw.println("確認状況<br />");
            pw.println("<table border=\"1\">");
            pw.println("<tr>");
            pw.println("<td>連番</td>");
            pw.println("<td>所属</td>");
            pw.println("<td>学籍番号</td>");
            pw.println("<td>氏名</td>");
            pw.println("<td>カナ</td>");
            pw.println("<td>状態</td>");
            pw.println("<td>緯度</td>");
            pw.println("<td>経度</td>");
            pw.println("<td>高度</td>");
            pw.println("<td>精度</td>");
            pw.println("</tr>");
            for (Attendance mAttendance : mAttendanceSheet.getAttendanceDisplayData()) {
                pw.println("<tr>");
                pw.println("<td>" + mAttendance.getStudentNum() + "</td>");
                pw.println("<td>" + mAttendance.getClassName() + "</td>");
                pw.println("<td>" + mAttendance.getStudentNo() + "</td>");
                pw.println("<td>" + mAttendance.getStudentName() + "</td>");
                pw.println("<td>" + mAttendance.getStudentRuby() + "</td>");
                pw.println("<td>" + mAttendance.getStatusString() + "</td>");
                pw.println("<td>" + mAttendance.getLatitude() + "</td>");
                pw.println("<td>" + mAttendance.getLongitude() + "</td>");
                pw.println("<td>" + mAttendance.getAltitude() + "</td>");
                pw.println("<td>" + mAttendance.getAccuracy() + "</td>");
                pw.println("</tr>");
            }
            pw.println("</table>");
        }
        else {
            pw.println("表示する内容がありません。");
        }
        pw.println("</p>");
        pw.println("</body>");
        pw.println("</html>");
        pw.close();
    }
}