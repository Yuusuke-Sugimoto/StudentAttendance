package jp.ddo.kingdragon.attendance.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * ログアウトを行うサーブレット
 * @author 杉本祐介
 */
public class LogoutServlet extends HttpServlet {
    // 定数の宣言
    /** シリアルバージョンUID */
    private static final long serialVersionUID = -6013398913270670830L;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        HttpSession session = request.getSession(true);
        session.setAttribute("Logout", true);

        response.sendRedirect("");
    }
}