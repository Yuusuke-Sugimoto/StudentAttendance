package jp.ddo.kingdragon.attendance;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class TestServlet extends HttpServlet {
    /**
     * シリアルバージョンUID
     */
    private static final long serialVersionUID = -8862915576392194796L;

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
        pw.println("<style type=\"text/css\">");
        pw.println(".multiplication td {");
        pw.println("width: 20px;");
        pw.println("text-align: right;");
        pw.println("}");
        pw.println("</style>");
        pw.println("");
        pw.println("<title>TestServlet</title>");
        pw.println("</head>");
        pw.println("<body>");
        pw.println("<p>");
        pw.println("TestServletです。<br />");
        pw.println("<a href=\"index.html\" title=\"index.htmlへ\">index.htmlへ</a><br />");
        pw.println("<a href=\"another.html\" title=\"another.htmlへ\">another.htmlへ</a><br />");
        pw.println("<table border=\"1\" class=\"multiplication\">");
        pw.println("<tr>");
        for (int i = 0; i < 10; i++) {
            pw.print("<td>");
            if (i != 0) {
                pw.print(i);
            }
            pw.println("</td>");
        }
        pw.println("</tr>");
        for (int i = 1; i < 10; i++) {
            pw.println("<tr>");
            for (int j = 0; j < 10; j++) {
                pw.print("<td>");
                if (j != 0) {
                    pw.print(i * j);
                }
                else {
                    pw.print(i);
                }
                pw.println("</td>");
            }
            pw.println("</tr>");
        }
        pw.println("</table>");
        pw.println("</p>");
        pw.println("</body>");
        pw.println("</html>");
        pw.close();
    }
}