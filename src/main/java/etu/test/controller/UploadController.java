package etu.test.controller;

import etu.sprint.framework.ModelView;
import etu.sprint.framework.annotation.FileParam;
import etu.sprint.framework.annotation.HttpMethod;
import etu.sprint.framework.annotation.MyUrl;
import etu.sprint.framework.controller.Controller;

import javax.servlet.http.Part;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

@Controller
public class UploadController {

    @MyUrl("/upload")
    @HttpMethod("GET")
    public ModelView showForm() {
        return new ModelView("upload-form.jsp");
    }

    // Exemple 1 — recevoir javax.servlet.http.Part (streaming possible)
    @MyUrl("/upload/part")
    @HttpMethod("POST")
    public ModelView uploadWithPart(Part file) {
        try {
            String filename = file.getSubmittedFileName();
            long size = file.getSize();

            // Sauvegarde exemple (stream → fichier)
            File target = Files.createTempFile("upload-part-", "_" + filename).toFile();
            try (InputStream in = file.getInputStream(); FileOutputStream out = new FileOutputStream(target)) {
                byte[] buf = new byte[8192];
                int r;
                while ((r = in.read(buf)) != -1) out.write(buf, 0, r);
            }

            System.out.println("[UploadController] part saved to: " + target.getAbsolutePath() + " (" + size + " bytes)");
            ModelView mv = new ModelView("upload-success.jsp");
            mv.addItem("msg", "Upload reçu via Part: " + filename + " (" + size + " bytes)");
            return mv;
        } catch (Exception e) {
            e.printStackTrace();
            ModelView mv = new ModelView("upload-success.jsp");
            mv.addItem("msg", "Erreur: " + e.getMessage());
            return mv;
        }
    }

    // Exemple 2 — recevoir via @FileParam en byte[] (petits fichiers)
    @MyUrl("/upload/bytes")
    @HttpMethod("POST")
    public ModelView uploadBytes(@FileParam("file") byte[] fileBytes) {
        try {
            if (fileBytes == null) {
                ModelView mv = new ModelView("upload-success.jsp");
                mv.addItem("msg", "Aucun fichier reçu (null)");
                return mv;
            }

            File target = Files.createTempFile("upload-bytes-", ".bin").toFile();
            Files.write(target.toPath(), fileBytes);

            System.out.println("[UploadController] byte[] saved to: " + target.getAbsolutePath() + " (" + fileBytes.length + " bytes)");
            ModelView mv = new ModelView("upload-success.jsp");
            mv.addItem("msg", "Upload reçu en mémoire → sauvegardé: " + target.getName() + " (" + fileBytes.length + " bytes)");
            return mv;
        } catch (Exception e) {
            e.printStackTrace();
            ModelView mv = new ModelView("upload-success.jsp");
            mv.addItem("msg", "Erreur: " + e.getMessage());
            return mv;
        }
    }

    // DEBUG — retourne le java.io.tmpdir (texte pour curl)
    @MyUrl("/debug/tmpdir")
    @HttpMethod("GET")
    public String getJvmTempDir() {
        String tmp = System.getProperty("java.io.tmpdir");
        System.out.println("[UploadController] java.io.tmpdir = " + tmp);
        return tmp == null ? "(null)" : tmp;
    }

    // Vue HTML simple pour affichage dans le navigateur
    @MyUrl("/debug/tmpdir/view")
    @HttpMethod("GET")
    public ModelView getJvmTempDirView() {
        ModelView mv = new ModelView("tmpdir.jsp");
        mv.addItem("tmpdir", System.getProperty("java.io.tmpdir"));
        return mv;
    }
}

