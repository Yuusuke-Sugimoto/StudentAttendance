package jp.ddo.kingdragon.attendance.servlet;

import android.content.Context;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import jp.ddo.kingdragon.attendance.DisasterModeActivity;
import jp.ddo.kingdragon.attendance.util.PreferenceUtil;

/**
 * ログイン時のチェックを行うサーブレット
 * @author 杉本祐介
 */
public class LoginCheckServlet extends HttpServlet {
    // 定数の宣言
    /** シリアルバージョンUID */
    private static final long serialVersionUID = 8997007372198004252L;

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String password = request.getParameter("password");
        Context applicationContext = DisasterModeActivity.getApplicationContextForServlet();

        if (applicationContext != null) {
            PreferenceUtil mPreferenceUtil = new PreferenceUtil(applicationContext);
            if (password != null && password.equals(mPreferenceUtil.getPassword(PreferenceUtil.DEFAULT_PASSWORD))) {
                HttpSession session = request.getSession(true);
                session.setAttribute("IsAuthorized", true);

                response.sendRedirect("StudentList");
            }
            else {
                HttpSession session = request.getSession(true);
                session.setAttribute("ErrorMessage", "パスワードが間違っています。");

                response.sendRedirect("");
            }
        }
        else {
            HttpSession session = request.getSession(true);
            session.setAttribute("ErrorMessage", "このページは現在利用できません。");

            response.sendRedirect("");
        }
    }
}