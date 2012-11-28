package jp.ddo.kingdragon.attendance.servlet;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 動画の表示を行うサーブレット
 * @author 杉本祐介
 */
public class ShowMovieServlet extends HttpServlet {
    // 定数の宣言
    /** シリアルバージョンUID */
    private static final long serialVersionUID = 5458814166959685378L;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String moviePath = request.getParameter("path");

        response.setContentType("text/html");
        response.setCharacterEncoding("UTF-8");
        PrintWriter pw = response.getWriter();
        pw.println("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">");
        pw.println("<html xmlns=\"http://www.w3.org/1999/xhtml\" xml:lang=\"ja\">");
        pw.println("<head>");
        pw.println("<meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\" />");
        pw.println("<meta http-equiv=\"Content-Style-Type\" content=\"text/css\" />");
        pw.println("<link rel=\"stylesheet\" type=\"text/css\" href=\"style.css\" />");
        pw.println("");
        pw.println("<title>動画再生画面</title>");
        pw.println("</head>");
        pw.println("<body>");
        pw.println("<p>");
        if (moviePath != null) {
            pw.println("<script type=\"text/javascript\" src=\"/jwplayer/jwplayer.js\"></script>");
            pw.println("<div id=\"mediaspace\">読み込み中…</div>");
            pw.println("<script type=\"text/javascript\">");
            pw.println("<!--");
            pw.println("jwplayer('mediaspace').setup({");
            pw.println("'flashplayer':  '/jwplayer/player.swf',");
            pw.println("'file':         '" + moviePath + "',");
            pw.println("'image':        '',");
            pw.println("'bufferlength': '3',");
            pw.println("'volume':       '50',");
            pw.println("'controlbar':   'bottom',");
            pw.println("'width':        '1000',");
            pw.println("'height':       '524'");
            pw.println("});");
            pw.println("-->");
            pw.println("</script>");
        }
        else {
            pw.println("表示する内容がありません。<br />");
        }
        pw.println("</p>");
        pw.println("</body>");
        pw.println("</html>");
        pw.flush();
        pw.close();
    }
}