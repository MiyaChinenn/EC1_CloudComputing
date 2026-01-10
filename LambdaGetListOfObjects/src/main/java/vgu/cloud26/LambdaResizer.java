package vgu.cloud26;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;

import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.S3Client;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.S3Event;
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification.S3EventNotificationRecord;

// Handler value: vgu.cloud26.LambdaResizer::handleRequest
public class LambdaResizer implements RequestHandler<S3Event, String> {

    private static final float MAX_DIMENSION = 100;
    private final String REGEX = ".*\\.([^\\.]*)";
    private final String JPG_TYPE = "jpg";
    private final String JPG_MIME = "image/jpeg";
    private final String PNG_TYPE = "png";
    private final String PNG_MIME = "image/png";

    @Override
    public String handleRequest(S3Event s3event, Context context) {
        LambdaLogger logger = context.getLogger();

        try {
            logger.log("Started resizing image...");
            S3EventNotificationRecord record = s3event.getRecords().get(0);

            String srcBucket = "public-miyachinenn";

            // Object key may have spaces or unicode non-ASCII characters.
            String srcKey = record.getS3().getObject().getUrlDecodedKey();

            // Create thumbnail bucket and key
            String dstBucket = "public-miyachinenn-thumbnails";
            String dstKey = "thumbnail-" + srcKey;

            // Infer the image type.
            Matcher matcher = Pattern.compile(REGEX).matcher(srcKey);
            if (!matcher.matches()) {
                logger.log("Unable to infer image type for key " + srcKey);
                return "Unable to infer image type for key " + srcKey;
            }
            String imageType = matcher.group(1).toLowerCase();
            if (!(JPG_TYPE.equals(imageType)) && !(PNG_TYPE.equals(imageType)) && !("jpeg".equals(imageType))) {
                logger.log("Skipping non-image " + srcKey + " (type: " + imageType + ")");
                return "Skipped non-image file: " + srcKey;
            }

            // Download the image from S3 into a stream
            S3Client s3Client = S3Client.builder()
                    .region(Region.AP_SOUTHEAST_1)
                    .build();
            InputStream s3Object = getObject(s3Client, srcBucket, srcKey);

            // Read the source image and resize it
            BufferedImage srcImage = ImageIO.read(s3Object);
            if (srcImage == null) {
                logger.log("Failed to read image: " + srcKey);
                return "Failed to read image: " + srcKey;
            }
            
            BufferedImage newImage = resizeImage(srcImage);

            // Re-encode image to target format (normalize to jpg/png)
            String outputType = "jpeg".equals(imageType) ? "jpg" : imageType;
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ImageIO.write(newImage, outputType, outputStream);

            // Upload thumbnail to S3
            try {
                putObject(s3Client, outputStream, dstBucket, dstKey, outputType, logger);
                logger.log("Thumbnail successfully created: " + dstBucket + "/" + dstKey);
                return "Thumbnail successfully created: " + dstBucket + "/" + dstKey;
            } catch (AwsServiceException e) {
                logger.log("AWS Error: " + e.awsErrorDetails().errorMessage());
                return "Error: " + e.awsErrorDetails().errorMessage();
            }

        } catch (IOException e) {
            logger.log("IO Error: " + e.getMessage());
            throw new RuntimeException("IO Error: " + e.getMessage(), e);
        } catch (Exception e) {
            logger.log("Unexpected error: " + e.getMessage());
            throw new RuntimeException("Unexpected error: " + e.getMessage(), e);
        }
    }

    private InputStream getObject(S3Client s3Client, String bucket, String key) {
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build();
        return s3Client.getObject(getObjectRequest);
    }

    private void putObject(S3Client s3Client, ByteArrayOutputStream outputStream,
            String bucket, String key, String imageType, LambdaLogger logger) {
        Map<String, String> metadata = new HashMap<>();
        metadata.put("Content-Length", Integer.toString(outputStream.size()));
        if (JPG_TYPE.equals(imageType) || "jpeg".equals(imageType)) {
            metadata.put("Content-Type", JPG_MIME);
        } else if (PNG_TYPE.equals(imageType)) {
            metadata.put("Content-Type", PNG_MIME);
        }

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .metadata(metadata)
                .build();

        // Uploading to S3 destination bucket
        logger.log("Writing thumbnail to: " + bucket + "/" + key);
        s3Client.putObject(putObjectRequest,
                RequestBody.fromBytes(outputStream.toByteArray()));
    }

    /**
     * Resizes (shrinks) an image into a small, thumbnail-sized image.
     *
     * The new image is scaled down proportionally based on the source image.
     * The scaling factor is determined based on the value of MAX_DIMENSION. The
     * resulting new image has max(height, width) = MAX_DIMENSION.
     *
     * @param srcImage BufferedImage to resize.
     * @return New BufferedImage that is scaled down to thumbnail size.
     */
    private BufferedImage resizeImage(BufferedImage srcImage) {
        int srcHeight = srcImage.getHeight();
        int srcWidth = srcImage.getWidth();
        
        // Skip resizing if image is already small enough
        if (srcWidth <= MAX_DIMENSION && srcHeight <= MAX_DIMENSION) {
            return srcImage;
        }
        
        // Infer scaling factor to avoid stretching image unnaturally
        float scalingFactor = Math.min(
                MAX_DIMENSION / srcWidth, MAX_DIMENSION / srcHeight);
        int width = (int) (scalingFactor * srcWidth);
        int height = (int) (scalingFactor * srcHeight);

        BufferedImage resizedImage = new BufferedImage(width, height,
                BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = resizedImage.createGraphics();
        // Fill with white before applying semi-transparent (alpha) images
        graphics.setPaint(Color.white);
        graphics.fillRect(0, 0, width, height);
        // Simple bilinear resize for better quality
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        graphics.setRenderingHint(RenderingHints.KEY_RENDERING,
                RenderingHints.VALUE_RENDER_QUALITY);
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.drawImage(srcImage, 0, 0, width, height, null);
        graphics.dispose();
        return resizedImage;
    }
}