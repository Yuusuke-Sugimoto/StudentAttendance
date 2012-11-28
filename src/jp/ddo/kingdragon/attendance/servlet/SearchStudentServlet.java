package jp.ddo.kingdragon.attendance.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URLEncoder;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * 学生の検索を行うサーブレット
 * @author 杉本祐介
 */
public class SearchStudentServlet extends HttpServlet {
    // 定数の宣言
    /** シリアルバージョンUID */
    private static final long serialVersionUID = -7540049868673003013L;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        HttpSession session = request.getSession(true);

        String no   = request.getParameter("no");
        String name = request.getParameter("name");
        if ((no == null || no.length() == 0) && (name == null || name.length() == 0)) {
            session.setAttribute("ErrorMessage", "検索条件が指定されていません。");

            response.sendRedirect("");
        }
        if (no != null) {
            if (no.matches(".*(\\[|\\]|\\-|\\(|\\)|\\||\\^|\\$|\\.|\\*|\\+|\\?|\\\\).*")) {
                session.setAttribute("ErrorMessage", "学籍番号に使用できない文字が含まれています。");

                response.sendRedirect("");
            }
        }
        else {
            no = "";
        }
        if (name != null) {
            if (name.matches(".*(\\[|\\]|\\-|\\(|\\)|\\||\\^|\\$|\\.|\\*|\\+|\\?|\\\\).*")) {
                session.setAttribute("ErrorMessage", "氏名に使用できない文字が含まれています。");

                response.sendRedirect("");
            }
        }
        else {
            name = "";
        }

        response.setContentType("text/html");
        response.setCharacterEncoding("UTF-8");
        PrintWriter pw = response.getWriter();
        pw.println("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">");
        pw.println("<html xmlns=\"http://www.w3.org/1999/xhtml\" xml:lang=\"ja\">");
        pw.println("<head>");
        pw.println("<meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\" />");
        pw.println("<meta http-equiv=\"Content-Style-Type\" content=\"text/css\" />");
        pw.println("<link rel=\"stylesheet\" type=\"text/css\" href=\"style.css\" />");
        pw.println("<meta http-equiv=\"Content-Script-Type\" content=\"text/javascript\" />");
        pw.println("");
        pw.println("<script type=\"text/javascript\" src=\"js/prototype.js\"></script>");
        pw.println("<script type=\"text/javascript\" src=\"js/dateformat.js\"></script>");
        pw.println("<script type=\"text/javascript\" src=\"js/makelisttable.js\"></script>");
        pw.println("<script type=\"text/javascript\">");
        pw.println("<!--");
        pw.println("function refresh() {");
        pw.println("document.getElementById('notice').firstChild.nodeValue = '読み込み中…';");
        pw.println("var mAjax = new Ajax.Request('GetStudentList', {'method': 'get', 'parameters': 'no=" + no + "&name=" + URLEncoder.encode(name) + "', "
                                                                 + "'onComplete': updateTable});");
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
        pw.println("document.getElementById('notice').firstChild.nodeValue = '該当する学生が見つかりません。';");
        pw.println("}");
        pw.println("}");
        pw.println("");
        pw.println("window.onload = refresh;");
        pw.println("// -->");
        pw.println("</script>");
        pw.println("");
        pw.println("<title>検索結果</title>");
        pw.println("</head>");
        pw.println("<body>");
        pw.println("<p>");
        pw.println("確認状況 <a title=\"更新\" class=\"pointer\" onClick=\"refresh();\">更新</a> <a href=\"Logout\" title=\"ログアウト\">ログアウト</a><br />");
        pw.println("</p>");
        pw.println("<table id=\"list_table\" border=\"1\" style=\"display: none;\"></table>");
        pw.println("<p id=\"notice\">読み込み中…</p>");
        pw.println("</body>");
        pw.println("</html>");
        pw.flush();
        pw.close();
    }
}