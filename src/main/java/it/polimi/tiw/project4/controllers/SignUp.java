package it.polimi.tiw.project4.controllers;

import it.polimi.tiw.project4.beans.User;
import it.polimi.tiw.project4.dao.AccountDAO;
import it.polimi.tiw.project4.dao.UserDAO;
import it.polimi.tiw.project4.utils.ConnectionHandler;
import org.apache.commons.text.StringEscapeUtils;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ServletContextTemplateResolver;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

@WebServlet("/signup")
public class SignUp extends HttpServlet {
    private Connection connection = null;
    private TemplateEngine templateEngine;

    public SignUp() {
        super();
    }

    @Override
    public void init() throws ServletException {
        connection = ConnectionHandler.getConnection(getServletContext());
        ServletContext servletContext = getServletContext();
        ServletContextTemplateResolver templateResolver = new ServletContextTemplateResolver(servletContext);
        templateResolver.setTemplateMode(TemplateMode.HTML);
        this.templateEngine = new TemplateEngine();
        this.templateEngine.setTemplateResolver(templateResolver);
        templateResolver.setSuffix(".html");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        // Obtain and escape params
        String name = StringEscapeUtils.escapeJava(request.getParameter("name"));
        String surname = StringEscapeUtils.escapeJava(request.getParameter("surname"));
        String email = StringEscapeUtils.escapeJava(request.getParameter("email"));
        String password = StringEscapeUtils.escapeJava(request.getParameter("password"));
        String confirmPassword = StringEscapeUtils.escapeJava(request.getParameter("confirmPassword"));

        if (name == null || surname == null || email == null || password == null || confirmPassword == null ||
                name.isBlank() || surname.isBlank() || email.isBlank() || password.isBlank() || confirmPassword.isBlank()) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid registration credentials");
            return;
        }
        if (!password.equals(confirmPassword)) {
            ServletContext servletContext = getServletContext();
            final WebContext ctx = new WebContext(request, response, servletContext, request.getLocale());
            ctx.setVariable("emailSignup", email);
            ctx.setVariable("nameSignup", name);
            ctx.setVariable("surnameSignup", surname);
            ctx.setVariable("errorMsgSignup", "Passwords did not match");
            templateEngine.process("/index.html", ctx, response.getWriter());
            return;
        }

        // Query DB to check if the user already exists
        UserDAO userDao = new UserDAO(connection);
        AccountDAO accountDao = new AccountDAO(connection);
        try {
            String path;
            User user = userDao.getUser(email, password);

            // If the user exists, show the signup page with an error message
            // Otherwise, create a user and an associated account and return to the login page
            if (user == null) {
                userDao.createUser(name, surname, email, password);
                user = userDao.getUser(email, password);
                accountDao.createAccount(user);
                path = getServletContext().getContextPath() + "/index.html";
                response.sendRedirect(path);
            } else {
                ServletContext servletContext = getServletContext();
                final WebContext ctx = new WebContext(request, response, servletContext, request.getLocale());
                ctx.setVariable("errorMsgSignup", "Incorrect username or password");
                path = "/index.html";
                templateEngine.process(path, ctx, response.getWriter());
            }
        } catch (SQLException e) {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to create a new user");
        }
    }

    @Override
    public void destroy() {
        try {
            ConnectionHandler.closeConnection(connection);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}