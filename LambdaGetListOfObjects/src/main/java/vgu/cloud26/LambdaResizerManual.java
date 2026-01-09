package vgu.cloud26;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

import org.json.JSONObject;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;

// Handler value: vgu.cloud26.LambdaResizerManual::handleRequest
public class LambdaResizerManual implements RequestHandler<Map<String, Object>, String> {

    private static final float MAX_DIMENSION = 100;
    private final String REGEX = ".*\\.([^\\.]*)";
    private final String JPG_TYPE = "jpg";
    private final String JPG_MIME = "image/jpeg";
    private final String PNG_TYPE = "png";
    private final String PNG_MIME = "image/png";

    @Override
    public String handleRequest(Map<String, Object> event, Context context) {
        LambdaLogger logger = context.getLogger();
        JSONObject response = new JSONObject();
        try {
            // Expect: { body: '{ "key": "filename.jpg", "content": "base64..." }' }
            String requestBody = (String) event.get("body");
            if (requestBody == null) {
                response.put("error", "Missing request body");
                return createResponse(400, response.toString());
            }
            JSONObject json = new JSONObject(requestBody);
            String srcKey = json.optString("key", null);
            String base64Content = json.optString("content", null);
            if (srcKey == null || base64Content == null) {
                response.put("error", "Missing 'key' or 'content' parameter");
                return createResponse(400, response.toString());
            }

            // Infer image type
            String imageType = "jpg";
            if (srcKey.toLowerCase().endsWith(".png")) imageType = "png";
            if (srcKey.toLowerCase().endsWith(".jpeg")) imageType = "jpg";
            if (!("jpg".equals(imageType) || "png".equals(imageType))) {
                response.put("error", "Unsupported image type: " + imageType);
                return createResponse(400, response.toString());
            }

            // Decode image
            byte[] imageBytes = Base64.getDecoder().decode(base64Content);
            InputStream inStream = new java.io.ByteArrayInputStream(imageBytes);
            BufferedImage srcImage = javax.imageio.ImageIO.read(inStream);
            if (srcImage == null) {
                response.put("error", "Failed to decode image");
                return createResponse(500, response.toString());
            }

            // Resize
            BufferedImage resized = resizeImage(srcImage);
            ByteArrayOutputStream outStream = new ByteArrayOutputStream();
            javax.imageio.ImageIO.write(resized, imageType, outStream);
            byte[] thumbBytes = outStream.toByteArray();

            // Upload to thumbnails bucket
            String dstBucket = "public-miyachinenn-thumbnails";
            String dstKey = "thumbnail-" + srcKey;
            software.amazon.awssdk.services.s3.S3Client s3Client = software.amazon.awssdk.services.s3.S3Client.builder()
                .region(software.amazon.awssdk.regions.Region.AP_SOUTHEAST_1)
                .build();
            software.amazon.awssdk.services.s3.model.PutObjectRequest putObjectRequest =
                software.amazon.awssdk.services.s3.model.PutObjectRequest.builder()
                    .bucket(dstBucket)
                    .key(dstKey)
                    .build();
            s3Client.putObject(putObjectRequest, software.amazon.awssdk.core.sync.RequestBody.fromBytes(thumbBytes));

            response.put("message", "Thumbnail created successfully");
            response.put("thumbnailKey", dstKey);
            response.put("bucket", dstBucket);
            return createResponse(200, response.toString());
        } catch (Exception e) {
            response.put("error", e.getMessage());
            return createResponse(500, response.toString());
        }
    }

    private String createResponse(int statusCode, String body) {
        JSONObject response = new JSONObject();
        response.put("statusCode", statusCode);
        response.put("body", body);
        return Base64.getEncoder().encodeToString(response.toString().getBytes(StandardCharsets.UTF_8));
    }


    /**
     * Resizes (shrinks) an image into a small, thumbnail-sized image.
     */
    private BufferedImage resizeImage(BufferedImage srcImage) {
        int srcHeight = srcImage.getHeight();
        int srcWidth = srcImage.getWidth();
        if (srcWidth <= MAX_DIMENSION && srcHeight <= MAX_DIMENSION) {
            return srcImage;
        }
        float scalingFactor = Math.min(MAX_DIMENSION / srcWidth, MAX_DIMENSION / srcHeight);
        int width = (int) (scalingFactor * srcWidth);
        int height = (int) (scalingFactor * srcHeight);
        BufferedImage resizedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        java.awt.Graphics2D graphics = resizedImage.createGraphics();
        graphics.setPaint(java.awt.Color.white);
        graphics.fillRect(0, 0, width, height);
        graphics.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION,
            java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        graphics.setRenderingHint(java.awt.RenderingHints.KEY_RENDERING,
            java.awt.RenderingHints.VALUE_RENDER_QUALITY);
        graphics.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING,
            java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.drawImage(srcImage, 0, 0, width, height, null);
        graphics.dispose();
        return resizedImage;
    }
}
