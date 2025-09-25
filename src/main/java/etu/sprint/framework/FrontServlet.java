package etu.sprint.framework;

import java.io.*;
import javax.servlet.*;
import javax.servlet.http.*;

public class FrontServlet extends HttpServlet {

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String uri = request.getRequestURI();
        String ctx = request.getContextPath();
        String path = uri.substring(ctx.length());

        String realPath = getServletContext().getRealPath(path);
        File file = new File(realPath);

        if (file.exists() && file.isFile()) {
            ServletContext context = getServletContext();
            String mime = context.getMimeType(realPath);
            if (mime == null) {
                mime = "application/octet-stream";
            }
            response.setContentType(mime);

            try (FileInputStream fis = new FileInputStream(file);
                 OutputStream os = response.getOutputStream()) {
                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = fis.read(buffer)) != -1) {
                    os.write(buffer, 0, bytesRead);
                }
            }

        } else {
            response.setContentType("text/html;charset=UTF-8");
            try (PrintWriter out = response.getWriter()) {
                out.println("<html><body>");
                out.println("<h1>Page non trouvée</h1>");
                out.println("<p>Vous avez demandé : " + path + "</p>");
                out.println("</body></html>");
            }
        }
    }
}
