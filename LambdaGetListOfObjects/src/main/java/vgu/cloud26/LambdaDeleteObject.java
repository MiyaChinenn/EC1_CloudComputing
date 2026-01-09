package vgu.cloud26;

import java.util.Base64;

import org.json.JSONObject;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

public class LambdaDeleteObject implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent event, Context context) {
        
        String bucketName = "public-miyachinenn";
        String requestBody = event.getBody();
        
        JSONObject bodyJSON = new JSONObject(requestBody);
        String objName = bodyJSON.getString("key");
        
        S3Client s3Client = S3Client.builder()
                .region(Region.AP_SOUTHEAST_1)
                .build();
        
        String message = "";
        int statusCode = 200;
        
        try {
            // Delete the original object
            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(objName)
                    .build();
            
            s3Client.deleteObject(deleteObjectRequest);
            
            // Delete the corresponding thumbnail (if it exists)
            String thumbnailBucket = "public-miyachinenn-thumbnails";
            String thumbnailKey = "thumbnail-" + objName;
            
            try {
                DeleteObjectRequest deleteThumbnailRequest = DeleteObjectRequest.builder()
                        .bucket(thumbnailBucket)
                        .key(thumbnailKey)
                        .build();
                
                s3Client.deleteObject(deleteThumbnailRequest);
                context.getLogger().log("Thumbnail also deleted: " + thumbnailKey);
                message = "Object and thumbnail deleted successfully: " + objName;
                
            } catch (S3Exception e) {
                // Thumbnail might not exist, log but continue
                context.getLogger().log("Thumbnail deletion failed (may not exist): " + e.awsErrorDetails().errorMessage());
                message = "Object deleted successfully: " + objName + " (thumbnail may not have existed)";
            }
            
        } catch (S3Exception e) {
            context.getLogger().log("S3Exception: " + e.awsErrorDetails().errorMessage());
            message = "Error deleting object: " + e.awsErrorDetails().errorMessage();
            statusCode = 500;
        } catch (Exception e) {
            context.getLogger().log("Exception: " + e.getMessage());
            message = "Error deleting object: " + e.getMessage();
            statusCode = 500;
        }
        
        String encodedString = Base64.getEncoder().encodeToString(message.getBytes());
        
        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
        response.setStatusCode(statusCode);
        response.setBody(encodedString);
        response.withIsBase64Encoded(true);
        response.setHeaders(java.util.Collections.singletonMap("Content-Type", "text/plain"));
        
        return response;
    }
}