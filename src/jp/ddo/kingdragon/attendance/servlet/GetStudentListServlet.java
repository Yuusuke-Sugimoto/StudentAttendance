package jp.ddo.kingdragon.attendance.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.TreeSet;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import jp.ddo.kingdragon.attendance.DisasterModeActivity;
import jp.ddo.kingdragon.attendance.student.Attendance;

/**
 * 学生リストの取得を行うサーブレット
 * @author 杉本祐介
 */
public class GetStudentListServlet extends HttpServlet {
    // 定数の宣言
    /** 1ページあたりの表示数 */
    private static final int NUM_OF_STUDENTS_PER_PAGE = 50;
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
        int sortNo = 0;
        String sortParam = request.getParameter("sort");
        if (sortParam != null) {
            try {
                sortNo = Integer.parseInt(sortParam);
            }
            catch (NumberFormatException e) {}
        }
        int pageNo = 1;
        String pageParam = request.getParameter("page");
        if (pageParam != null) {
            try {
                pageNo = Integer.parseInt(pageParam);
            }
            catch (NumberFormatException e) {}
        }

        response.setContentType("text/xml");
        response.setCharacterEncoding("UTF-8");
        response.addHeader("Cache-Control", "no-store, no-cache, must-revalidate, post-check=0, pre-check=0");
        PrintWriter pw = response.getWriter();
        pw.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        pw.println("<StudentList>");

        if (isAuthorized || targetNo != null || targetName != null) {
            ArrayList<Attendance> attendances = DisasterModeActivity.getAttendanceSheet().getAttendanceList();
            int numOfPages = (int)Math.ceil((double)attendances.size() / (double)GetStudentListServlet.NUM_OF_STUDENTS_PER_PAGE);
            if (attendances.size() != 0 && pageNo <= numOfPages) {
                if (pageParam != null) {
                    Comparator<Attendance> mComparator;
                    switch (sortNo) {
                        case 1: {
                            mComparator = new Comparator<Attendance>() {
                                @Override
                                public int compare(Attendance o1, Attendance o2) {
                                    return o1.getClassName().compareTo(o2.getClassName());
                                }
                            };

                            break;
                        }
                        case 2: {
                            mComparator = new Comparator<Attendance>() {
                                @Override
                                public int compare(Attendance o1, Attendance o2) {
                                    return o1.getStudentNo().compareTo(o2.getStudentNo());
                                }
                            };

                            break;
                        }
                        case 3: {
                            mComparator = new Comparator<Attendance>() {
                                @Override
                                public int compare(Attendance o1, Attendance o2) {
                                    return o1.getStudentRuby().compareTo(o2.getStudentRuby());
                                }
                            };

                            break;
                        }
                        case 4: {
                            mComparator = new Comparator<Attendance>() {
                                @Override
                                public int compare(Attendance o1, Attendance o2) {
                                    int retInt = 0;

                                    long result = o1.getTimeStamp() - o2.getTimeStamp();
                                    if (result > 0) {
                                        retInt = 1;
                                    }
                                    else if (result < 0) {
                                        retInt = -1;
                                    }

                                    return retInt;
                                }
                            };

                            break;
                        }
                        default: {
                            mComparator = new Comparator<Attendance>() {
                                @Override
                                public int compare(Attendance o1, Attendance o2) {
                                    return 0;
                                }
                            };

                            break;
                        }
                    }
                    Collections.sort(attendances, mComparator);
                }
                TreeSet<String> classNames = new TreeSet<String>(new Comparator<String>() {
                    @Override
                    public int compare(String o1, String o2) {
                        return o1.toUpperCase().compareTo(o2.toUpperCase());
                    }
                });
                ArrayList<Attendance> targets = new ArrayList<Attendance>();
                for (Attendance mAttendance : attendances) {
                    boolean isTarget = false;

                    classNames.add(mAttendance.getClassName());

                    String upperClassName   = mAttendance.getClassName().toUpperCase();
                    String upperStudentNo   = mAttendance.getStudentNo().toUpperCase();
                    String upperStudentName = mAttendance.getStudentName().toUpperCase();
                    if (isAuthorized && targetNo == null && targetName == null) {
                        if (targetClass == null || upperClassName.matches("^" + targetClass + "$")) {
                            isTarget = true;
                        }
                    }
                    else if (targetNo != null && upperStudentNo.matches("^" + targetNo + "$")) {
                        if (targetClass == null || upperClassName.matches("^" + targetClass + "$")) {
                            isTarget = true;
                        }
                    }
                    else if (targetName != null && (upperStudentName.matches("^" + targetName + ".*")
                             || upperStudentName.replaceAll(" ", "").matches("^" + targetName + ".*"))) {
                        if (targetClass == null || upperClassName.matches("^" + targetClass + "$")) {
                            isTarget = true;
                        }
                    }
                    if (isTarget) {
                        targets.add(mAttendance);
                    }
                }

                int startIndex = GetStudentListServlet.NUM_OF_STUDENTS_PER_PAGE * (pageNo - 1);
                numOfPages = (int)Math.ceil((double)targets.size() / (double)GetStudentListServlet.NUM_OF_STUDENTS_PER_PAGE);
                int numInPage;
                if (startIndex + GetStudentListServlet.NUM_OF_STUDENTS_PER_PAGE <= targets.size()) {
                    numInPage = GetStudentListServlet.NUM_OF_STUDENTS_PER_PAGE;
                }
                else {
                    numInPage = targets.size() - startIndex;
                }
                pw.println("<Information>");
                pw.println("<NumOfStudents>" + targets.size() + "</NumOfStudents>");
                pw.println("<NumOfPages>" + numOfPages + "</NumOfPages>");
                pw.println("<NumOfStudentsPerPage>" + GetStudentListServlet.NUM_OF_STUDENTS_PER_PAGE + "</NumOfStudentsPerPage>");
                pw.println("<PageNo>" + pageNo + "</PageNo>");
                pw.println("<NumOfStudentsInPage>" + numInPage + "</NumOfStudentsInPage>");
                pw.println("<ClassNameList>");
                for (String className : classNames) {
                    pw.println("<ClassName>" + className + "</ClassName>");
                }
                pw.println("</ClassNameList>");
                pw.println("</Information>");
                int count = 0;
                for (int i = startIndex; count < GetStudentListServlet.NUM_OF_STUDENTS_PER_PAGE && i < targets.size(); i++) {
                    Attendance mAttendance = targets.get(i);
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
                    count++;
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
     * @return 異常がなければ引数の各文字を大文字にしたもの 異常があればnull
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