package jp.ddo.kingdragon.attendance.servlet;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * 学生リストの表示を行うサーブレット
 * @author 杉本祐介
 */
public class StudentListServlet extends HttpServlet {
    // 定数の宣言
    /** シリアルバージョンUID */
    private static final long serialVersionUID = 2461373901899466187L;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        HttpSession session = request.getSession(true);
        boolean isAuthorized = false;
        Object obj = session.getAttribute("IsAuthorized");
        if (obj != null && obj instanceof Boolean) {
            isAuthorized = (Boolean)obj;
        }

        if (isAuthorized) {
            response.setContentType("text/html");
            response.setCharacterEncoding("UTF-8");
            PrintWriter pw = response.getWriter();
            pw.println("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">");
            pw.println("<html xmlns=\"http://www.w3.org/1999/xhtml\" xml:lang=\"ja\">");
            pw.println("<head>");
            pw.println("<meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\" />");
            pw.println("<meta http-equiv=\"Content-Style-Type\" content=\"text/css\" />");
            pw.println("<meta http-equiv=\"Content-Script-Type\" content=\"text/javascript\" />");
            pw.println("<link rel=\"stylesheet\" type=\"text/css\" href=\"style.css\" />");
            pw.println("");
            pw.println("<script type=\"text/javascript\" src=\"js/prototype.js\"></script>");
            pw.println("<script type=\"text/javascript\" src=\"js/dateformat.js\"></script>");
            pw.println("<script type=\"text/javascript\" src=\"js/makelisttable.js\"></script>");
            pw.println("<script type=\"text/javascript\">");
            pw.println("<!--");
            pw.println("function refresh() {");
            pw.println("document.getElementById('notice').firstChild.nodeValue = '読み込み中…';");
            pw.println("var className = document.form.class.options[document.form.class.selectedIndex].value;");
            pw.println("var mAjax = new Ajax.Request('GetStudentList', {'method': 'get', 'parameters': 'class=' + className, 'onComplete': updateTable});");
            pw.println("}");
            pw.println("");
            pw.println("function updateTable(request) {");
            pw.println("var xml = request.responseXML;");
            pw.println("var studentList = xml.getElementsByTagName('Student');");
            pw.println("if (studentList.length > 0) {");
            pw.println("var listTable = makeListTable(studentList);");
            pw.println("var oldTable = document.getElementById('list_table');");
            pw.println("document.getElementById('notice').style.display = 'none';");
            pw.println("document.getElementsByTagName('body')[0].replaceChild(listTable, oldTable);");
            pw.println("}");
            pw.println("else {");
            pw.println("document.getElementById('list_table').style.display = 'none';");
            pw.println("document.getElementById('notice').style.display = 'block';");
            pw.println("document.getElementById('notice').firstChild.nodeValue = '現在確認済みの学生データはありません。';");
            pw.println("}");
            pw.println("}");
            pw.println("");
            pw.println("window.onload = refresh;");
            pw.println("// -->");
            pw.println("</script>");
            pw.println("");
            pw.println("<title>確認状況</title>");
            pw.println("</head>");
            pw.println("<body>");
            pw.println("<p>");
            pw.println("確認状況 <a title=\"更新\" class=\"pointer\" onClick=\"refresh();\">更新</a> <a href=\"Logout\" title=\"ログアウト\">ログアウト</a><br />");
            pw.println("</p>");
            pw.println("<form action=\"#\" name=\"form\">");
            pw.println("表示する所属:<select name=\"class\" onChange=\"refresh()\">");
            pw.println("<option value=\"\">全体</option>");
            pw.println("<option value=\"1KK\">1KK</option>");
            pw.println("<option value=\"1KX\">1KX</option>");
            pw.println("<option value=\"2KK\">2KK</option>");
            pw.println("<option value=\"2KX\">2KX</option>");
            pw.println("<option value=\"3KK\">3KK</option>");
            pw.println("<option value=\"3KX\">3KX</option>");
            pw.println("<option value=\"4KK\">4KK</option>");
            pw.println("<option value=\"4KX\">4KX</option>");
            pw.println("</select>");
            pw.println("</form>");
            pw.println("<table id=\"list_table\" border=\"1\" style=\"display: none;\"></table>");
            pw.println("<p id=\"notice\">読み込み中…</p>");
            pw.println("</body>");
            pw.println("</html>");
            pw.flush();
            pw.close();
        }
        else {
            session.setAttribute("ErrorMessage", "このページの表示にはログインが必要です。");

            response.sendRedirect("");
        }
    }
}