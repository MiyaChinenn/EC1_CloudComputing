package com.miyachinenn;

import java.io.IOException;
import java.io.OutputStream;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import software.amazon.awssdk.auth.credentials.InstanceProfileCredentialsProvider;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

@WebServlet(urlPatterns = {"/object/*"})
public class GetObjectFixedBucket extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String pathInfo = req.getPathInfo();
        String[] pathParts = pathInfo.split("/");
        String key = pathParts[1];

        // Set content type based on file extension
        String contentType = getContentType(key);
        resp.setContentType(contentType);
        String bucketName = "public-miyachinenn";

        // Use IAM Instance Profile (SECURE)
        S3Client s3Client
                = S3Client.builder()
                        .credentialsProvider(InstanceProfileCredentialsProvider.create())
                        .region(Region.AP_SOUTHEAST_1)
                        .build();

        GetObjectRequest request
                = GetObjectRequest.builder()
                        .bucket(bucketName)
                        .key(key)
                        .build();

        try {
            ResponseInputStream<GetObjectResponse> response
                    = s3Client.getObject(request);
            OutputStream outputStream = resp.getOutputStream();

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = response.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            outputStream.flush();

            response.close();
            outputStream.close();
        } catch (Exception e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().println("Error retrieving object: " + e.getMessage());
        }
    }

    private String getContentType(String fileName) {
        String extension = fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
        switch (extension) {
            case "jpg":
            case "jpeg":
                return "image/jpeg";
            case "png":
                return "image/png";
            case "gif":
                return "image/gif";
            case "pdf":
                return "application/pdf";
            case "txt":
                return "text/plain";
            case "html":
                return "text/html";
            default:
                return "application/octet-stream";
        }
    }
}
