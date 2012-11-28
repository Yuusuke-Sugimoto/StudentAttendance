package jp.ddo.kingdragon.attendance.servlet;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import jp.ddo.kingdragon.attendance.DisasterModeActivity;
import jp.ddo.kingdragon.attendance.student.Attendance;
import jp.ddo.kingdragon.attendance.student.AttendanceSheet;

/**
 * 学生リストの取得を行うサーブレット
 * @author 杉本祐介
 */
public class GetStudentListServlet extends HttpServlet {
    // 定数の宣言
    /** シリアルバージョンUID */
    private static final long serialVersionUID = -5994569700577761416L;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        HttpSession session = request.getSession(true);
        boolean isAuthorized = false;
        Object obj = session.getAttribute("IsAuthorized");
        if (obj != null && obj instanceof Boolean) {
            isAuthorized = (Boolean)obj;
        }

        String targetClass = checkParameter(request.getParameter("class"));
        String targetNo    = checkParameter(request.getParameter("no"));
        String targetName  = checkParameter(request.getParameter("name"));

        response.setContentType("text/xml");
        response.setCharacterEncoding("UTF-8");
        response.addHeader("Cache-Control", "no-store, no-cache, must-revalidate, post-check=0, pre-check=0");
        PrintWriter pw = response.getWriter();
        pw.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        pw.println("<StudentList>");

        if (isAuthorized || targetNo != null || targetName != null) {
            AttendanceSheet mAttendanceSheet = DisasterModeActivity.getAttendanceSheet();
            for (Attendance mAttendance : mAttendanceSheet.getAttendanceList()) {
                boolean isTarget = false;

                String upperClassName   = mAttendance.getClassName().toUpperCase();
                String upperStudentNo   = mAttendance.getStudentNo().toUpperCase();
                String upperStudentName = mAttendance.getStudentName().toUpperCase();
                if (isAuthorized && targetClass == null && targetNo == null && targetName == null) {
                    isTarget = true;
                }
                else if (!isTarget && isAuthorized && targetClass != null && upperClassName.matches("^" + targetClass + "$")) {
                    isTarget = true;
                }
                else if (!isTarget && targetNo != null && upperStudentNo.matches("^" + targetNo + "$")) {
                    isTarget = true;
                }
                else if (!isTarget && targetName != null && (upperStudentName.matches("^" + targetName + ".*")
                         || upperStudentName.replaceAll(" ", "").matches("^" + targetName + ".*"))) {
                    isTarget = true;
                }
                if (isTarget) {
                    pw.println("<Student>");
                    pw.println("<AttendanceNo>" + mAttendance.getAttendanceNo() + "</AttendanceNo>");
                    pw.println("<ClassName>" + mAttendance.getClassName() + "</ClassName>");
                    pw.println("<StudentNo>" + mAttendance.getStudentNo() + "</StudentNo>");
                    pw.println("<StudentName>" + mAttendance.getStudentName() + "</StudentName>");
                    pw.println("<StudentRuby>" + mAttendance.getStudentRuby() + "</StudentRuby>");
                    pw.println("<Status>" + mAttendance.getStatus() + "</Status>");
                    pw.println("<TimeStamp>" + mAttendance.getTimeStamp() + "</TimeStamp>");
                    pw.println("<Latitude>" + mAttendance.getLatitude() + "</Latitude>");
                    pw.println("<Longitude>" + mAttendance.getLongitude() + "</Longitude>");
                    pw.println("<Accuracy>" + mAttendance.getAccuracy() + "</Accuracy>");
                    pw.println("<PhotoPath>" + mAttendance.getExtra(Attendance.PHOTO_PATH, "") + "</PhotoPath>");
                    pw.println("<MoviePath>" + mAttendance.getExtra(Attendance.MOVIE_PATH, "") + "</MoviePath>");
                    pw.println("</Student>");
                }
            }
        }

        pw.println("</StudentList>");
        pw.flush();
        pw.close();
    }

    /**
     * パラメータの文字列をチェックする
     * @param value 文字列
     * @return 異常がなければ文字列の{@link String#toUpperCase()} 異常があればnull
     */
    private String checkParameter(String value) {
        if (value != null) {
            if (value.length() == 0) {
                value = null;
            }
            else if (value.matches(".*(\\[|\\]|\\-|\\(|\\)|\\||\\^|\\$|\\.|\\*|\\+|\\?|\\\\).*")) {
                value = null;
            }
            else {
                value.toUpperCase();
            }
        }

        return value;
    }
}