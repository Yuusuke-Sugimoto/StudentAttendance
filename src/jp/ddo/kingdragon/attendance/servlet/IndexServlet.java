package jp.ddo.kingdragon.attendance.servlet;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * トップページの表示を行うサーブレット
 * @author 杉本祐介
 */
public class IndexServlet extends HttpServlet {
    // 定数の宣言
    /** シリアルバージョンUID */
    private static final long serialVersionUID = -6797874574014146363L;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        boolean logout = false;
        if (session != null) {
            Object obj = session.getAttribute("Logout");
            if (obj != null && obj instanceof Boolean) {
                logout = (Boolean)obj;
            }
        }
        if (logout) {
            session.invalidate();
            session = null;
        }

        boolean isAuthorized = false;
        if (session != null) {
            Object obj = session.getAttribute("IsAuthorized");
            if (obj != null && obj instanceof Boolean) {
                isAuthorized = (Boolean)obj;
            }
        }
        String errorMessage = null;
        if (session != null) {
            Object obj = session.getAttribute("ErrorMessage");
            if (obj != null && obj instanceof String) {
                errorMessage = (String)obj;
                session.removeAttribute("ErrorMessage");
            }
        }

        response.setContentType("text/html");
        response.setCharacterEncoding("UTF-8");
        PrintWriter pw = response.getWriter();
        pw.println("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">");
        pw.println("<html xmlns=\"http://www.w3.org/1999/xhtml\" xml:lang=\"ja\">");
        pw.println("<head>");
        pw.println("<meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\" />");
        pw.println("<meta http-equiv=\"Content-Style-Type\" content=\"text/css\" />");
        pw.println("<link rel=\"stylesheet\" type=\"text/css\" href=\"css/style.css\" />");
        pw.println("");
        pw.println("<title>安否確認用ページ</title>");
        pw.println("</head>");
        pw.println("<body>");
        pw.println("<p>安否確認用ページ</p>");

        if (logout) {
            pw.println("<p>ログアウトしました。</p>");
        }
        if (errorMessage != null) {
            pw.println("<p>" + errorMessage + "</p>");
        }

        pw.println("<form action=\"StudentList\" method=\"GET\">");
        pw.println("<table>");
        pw.println("<tr>");
        pw.println("<td colspan=\"2\">学生を検索する</td>");
        pw.println("</tr>");
        pw.println("<tr>");
        pw.println("<td>学籍番号(完全一致)</td>");
        pw.println("<td><input type=\"text\" name=\"no\" size=\"20\" maxlength=\"6\" value=\"\" /></td>");
        pw.println("</tr>");
        pw.println("<tr>");
        pw.println("<td>氏名(前方一致)</td>");
        pw.println("<td><input type=\"text\" name=\"name\" size=\"20\" maxlength=\"20\" value=\"\" /></td>");
        pw.println("</tr>");
        pw.println("<tr>");
        pw.println("<td colspan=\"2\"><input type=\"submit\" value=\"検索\" style=\"width: 100%;\" /></td>");
        pw.println("</tr>");
        pw.println("</table>");
        pw.println("</form>");
        pw.println("<p />");

        if (!isAuthorized) {
            pw.println("<form action=\"LoginCheck\" method=\"POST\">");
            pw.println("<table>");
            pw.println("<tr>");
            pw.println("<td colspan=\"2\">教職員用</td>");
            pw.println("</tr>");
            pw.println("<tr>");
            pw.println("<td>パスワード</td>");
            pw.println("<td><input type=\"password\" name=\"password\" size=\"20\" maxlength=\"20\" value=\"\" /></td>");
            pw.println("</tr>");
            pw.println("<tr>");
            pw.println("<td colspan=\"2\"><input type=\"submit\" value=\"ログイン\" style=\"width: 100%;\" /></td>");
            pw.println("</tr>");
            pw.println("</table>");
            pw.println("</form>");
        }
        else {
            pw.println("<p>");
            pw.println("ログイン済み <a href=\"Logout\" title=\"ログアウト\">ログアウト</a><br />");
            pw.println("<a href=\"StudentList\" title=\"学生リスト\">学生リスト</a>");
            pw.println("</p>");
        }

        pw.println("</body>");
        pw.println("</html>");
        pw.flush();
        pw.close();
    }
}