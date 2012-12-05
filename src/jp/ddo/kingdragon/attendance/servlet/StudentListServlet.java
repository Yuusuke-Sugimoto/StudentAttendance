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

        String no   = request.getParameter("no");
        if (no != null) {
            if (no.matches(".*(\\[|\\]|\\-|\\(|\\)|\\||\\^|\\$|\\.|\\*|\\+|\\?|\\\\).*")) {
                session.setAttribute("ErrorMessage", "学籍番号に使用できない文字が含まれています。");

                response.sendRedirect("");
            }
        }
        else {
            no = "";
        }
        String name = request.getParameter("name");
        if (name != null) {
            if (name.matches(".*(\\[|\\]|\\-|\\(|\\)|\\||\\^|\\$|\\.|\\*|\\+|\\?|\\\\).*")) {
                session.setAttribute("ErrorMessage", "氏名に使用できない文字が含まれています。");

                response.sendRedirect("");
            }
        }
        else {
            name = "";
        }

        if (isAuthorized || no.length() != 0 || name.length() != 0) {
            response.setContentType("text/html");
            response.setCharacterEncoding("UTF-8");
            PrintWriter pw = response.getWriter();
            pw.println("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">");
            pw.println("<html xmlns=\"http://www.w3.org/1999/xhtml\" xml:lang=\"ja\">");
            pw.println("<head>");
            pw.println("<meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\" />");
            pw.println("<meta http-equiv=\"Content-Style-Type\" content=\"text/css\" />");
            pw.println("<meta http-equiv=\"Content-Script-Type\" content=\"text/javascript\" />");
            pw.println("<link rel=\"stylesheet\" type=\"text/css\" href=\"css/style.css\" />");
            pw.println("<link rel=\"stylesheet\" type=\"text/css\" href=\"css/lightbox.css\" />");
            pw.println("");
            pw.println("<script type=\"text/javascript\" src=\"js/prototype.js\"></script>");
            pw.println("<script type=\"text/javascript\" src=\"js/dateformat.js\"></script>");
            pw.println("<script type=\"text/javascript\" src=\"js/makelisttable.js\"></script>");
            pw.println("<script type=\"text/javascript\" src=\"js/jquery-1.7.2.min.js\"></script>");
            pw.println("<script type=\"text/javascript\" src=\"js/lightbox.js\"></script>");
            pw.println("<script type=\"text/javascript\">");
            pw.println("<!--");
            pw.println("var className = '';");
            pw.println("var sortKey = '';");
            pw.println("var pageNo = 1;");
            pw.println("");
            pw.println("function refresh() {");
            pw.println("var notice = document.getElementById('notice');");
            pw.println("notice.firstChild.nodeValue = '読み込み中…';");
            pw.println("var anchors = notice.getElementsByTagName('a');");
            pw.println("if (anchors.length != 0) {");
            pw.println("notice.removeChild(anchors[0]);");
            pw.println("}");
            pw.println("var params = 'class=' + className + '&sort=' + sortKey + '&no=" + no + "&name=" + URLEncoder.encode(name) + "&page=' + pageNo;");
            pw.println("var mAjax = new Ajax.Request('GetStudentList', {'method': 'get', "
                                                                     + "'parameters': params, "
                                                                     + "'onComplete': updateUi});");
            pw.println("}");
            pw.println("");
            pw.println("function updateUi(request) {");
            pw.println("var xml = request.responseXML;");
            pw.println("var studentList = xml.getElementsByTagName('Student');");
            pw.println("if (studentList.length != 0) {");
            pw.println("var listTable = makeListTable(studentList);");
            pw.println("var oldTable = document.getElementById('list_table');");
            pw.println("document.getElementById('notice').style.display = 'none';");
            pw.println("listTable.style.display = 'block';");
            pw.println("var body = document.getElementsByTagName('body')[0];");
            pw.println("body.replaceChild(listTable, oldTable);");
            pw.println("");
            pw.println("var classNames = xml.getElementsByTagName('ClassNameList')[0].getElementsByTagName('ClassName');");
            pw.println("var classSelector = document.createElement('select');");
            pw.println("classSelector.setAttribute('id', 'class');");
            pw.println("classSelector.setAttribute('name', 'class');");
            pw.println("var wholeOption = document.createElement('option');");
            pw.println("wholeOption.setAttribute('value', '');");
            pw.println("wholeOption.appendChild(document.createTextNode('全て表示'));");
            pw.println("classSelector.appendChild(wholeOption);");
            pw.println("for (var i = 0; i < classNames.length; i++) {");
            pw.println("var mClassName = classNames[i].firstChild.nodeValue;");
            pw.println("var option = document.createElement('option');");
            pw.println("option.setAttribute('value', mClassName);");
            pw.println("option.appendChild(document.createTextNode(mClassName));");
            pw.println("classSelector.appendChild(option);");
            pw.println("if (mClassName == className) {");
            pw.println("classSelector.selectedIndex = i + 1;");
            pw.println("}");
            pw.println("}");
            pw.println("var form = document.getElementById('form');");
            pw.println("form.replaceChild(classSelector, document.getElementById('class'));");
            pw.println("form.style.display = 'block';");
            pw.println("");
            pw.println("var numOfStudents = parseInt(xml.getElementsByTagName('NumOfStudents')[0].firstChild.nodeValue);");
            pw.println("var numOfPages = parseInt(xml.getElementsByTagName('NumOfPages')[0].firstChild.nodeValue);");
            pw.println("var numPerPage = parseInt(xml.getElementsByTagName('NumOfStudentsPerPage')[0].firstChild.nodeValue);");
            pw.println("var numInPage = parseInt(xml.getElementsByTagName('NumOfStudentsInPage')[0].firstChild.nodeValue);");
            pw.println("var firstNo = numPerPage * (pageNo - 1) + 1;");
            pw.println("var pageNavi = document.createElement('p');");
            pw.println("pageNavi.setAttribute('id', 'page_navi_top');");
            pw.println("pageNavi.appendChild(document.createTextNode(numOfStudents + '人中' + firstNo + '～' + (firstNo + numInPage - 1) + '人目を表示'));");
            pw.println("pageNavi.appendChild(document.createElement('br'));");
            pw.println("pageNavi.appendChild(document.createTextNode('ページ :'));");
            pw.println("for (var i = 1; i <= numOfPages; i++) {");
            pw.println("pageNavi.appendChild(document.createTextNode(' '));");
            pw.println("if (i != pageNo) {");
            pw.println("var anchor = document.createElement('a');");
            pw.println("anchor.appendChild(document.createTextNode(i));");
            pw.println("anchor.setAttribute('class', 'pointer');");
            pw.println("anchor.setAttribute('onClick', 'setPage(' + i + ');');");
            pw.println("pageNavi.appendChild(anchor);");
            pw.println("}");
            pw.println("else {");
            pw.println("pageNavi.appendChild(document.createTextNode(i));");
            pw.println("}");
            pw.println("}");
            pw.println("body.replaceChild(pageNavi, document.getElementById('page_navi_top'));");
            pw.println("pageNavi = pageNavi.cloneNode(true);");
            pw.println("pageNavi.setAttribute('id', 'page_navi_bottom');");
            pw.println("body.replaceChild(pageNavi, document.getElementById('page_navi_bottom'));");
            pw.println("}");
            pw.println("else {");
            pw.println("document.getElementById('list_table').style.display = 'none';");
            pw.println("document.getElementById('form').style.display = 'none';");
            pw.println("document.getElementById('page_navi_top').style.display = 'none';");
            pw.println("document.getElementById('page_navi_bottom').style.display = 'none';");
            pw.println("var notice = document.getElementById('notice');");
            pw.println("notice.style.display = 'block';");
            pw.println("notice.firstChild.nodeValue = '現在確認済みの学生データはありません。 ';");
            pw.println("var anchor = document.createElement('a');");
            pw.println("anchor.appendChild(document.createTextNode('更新'));");
            pw.println("anchor.setAttribute('class', 'pointer');");
            pw.println("anchor.setAttribute('onClick', 'refresh();');");
            pw.println("anchor.setAttribute('title', '更新');");
            pw.println("notice.appendChild(anchor);");
            pw.println("}");
            pw.println("}");
            pw.println("");
            pw.println("function sort() {");
            pw.println("className = document.form.class.options[document.form.class.selectedIndex].value;");
            pw.println("sortKey = document.form.sort_key.options[document.form.sort_key.selectedIndex].value;");
            pw.println("pageNo = 1;");
            pw.println("refresh();");
            pw.println("}");
            pw.println("");
            pw.println("function setPage(inPageNo) {");
            pw.println("pageNo = inPageNo;");
            pw.println("refresh();");
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
            pw.print("確認状況");
            if (isAuthorized) {
                pw.print(" <a href=\"Logout\" title=\"ログアウト\">ログアウト</a>");
            }
            pw.println("<br />");
            pw.println("</p>");
            pw.println("<p id=\"page_navi_top\"></p>");
            pw.println("<form id=\"form\" name=\"form\" action=\"#\">");
            pw.println("表示する所属 : <select id=\"class\" name=\"class\"></select>");
            pw.println("");
            pw.println(" ソート : <select name=\"sort_key\">");
            pw.println("<option value=\"0\">なし</option>");
            pw.println("<option value=\"1\">所属</option>");
            pw.println("<option value=\"2\">学籍番号</option>");
            pw.println("<option value=\"3\">氏名</option>");
            pw.println("<option value=\"4\">確認日時</option>");
            pw.println("</select>");
            pw.println("<button type=\"button\" onClick=\"sort();\">更新</button>");
            pw.println("</form>");
            pw.println("<table id=\"list_table\" border=\"1\" class=\"thin_table\"></table>");
            pw.println("<p id=\"notice\">読み込み中…</p>");
            pw.println("<p id=\"page_navi_bottom\"></p>");
            pw.println("</body>");
            pw.println("</html>");
            pw.flush();
            pw.close();
        }
        else {
            session.setAttribute("ErrorMessage", "検索条件が指定されていません。");

            response.sendRedirect("");
        }
    }
}