package vgu.cloud26;

import java.util.Base64;
import java.util.concurrent.TimeUnit;

import org.json.JSONObject;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;

import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.lambda.model.InvokeRequest;
import software.amazon.awssdk.services.lambda.model.InvokeResponse;

public class LambdaOrchestrator implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private static final String UPLOAD_FUNCTION_NAME = "LambdaUploadObject";
    private static final String RESIZE_FUNCTION_NAME = "LambdaResizerManual";
    private static final String INSERT_FUNCTION_NAME = "LambdaInsertPhotoDB";
    private static final int WAIT_TIME_SECONDS = 3;

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent request, Context context) {
        LambdaLogger logger = context.getLogger();
        JSONObject response = new JSONObject();
        LambdaClient lambdaClient = LambdaClient.builder().build();
        try {
            String requestBody = request.getBody();
            JSONObject inputData = new JSONObject(requestBody);
            String description = inputData.getString("Description");
            String s3Key = inputData.getString("S3Key");
            String content = inputData.getString("content");
            logger.log("Orchestrating upload for: " + description + " - " + s3Key);
            JSONObject uploadPayload = new JSONObject();
            uploadPayload.put("content", content);
            uploadPayload.put("key", s3Key);
            JSONObject uploadRequest = new JSONObject();
            uploadRequest.put("body", uploadPayload.toString());
            InvokeRequest uploadInvoke = InvokeRequest.builder()
                .functionName(UPLOAD_FUNCTION_NAME)
                .payload(SdkBytes.fromUtf8String(uploadRequest.toString()))
                .build();
            InvokeResponse uploadResponseRaw = lambdaClient.invoke(uploadInvoke);
            JSONObject uploadParsed = parseLambdaProxyResponse(uploadResponseRaw.payload().asUtf8String());
            JSONObject uploadResponse = new JSONObject();
            if (uploadParsed.has("decodedBody")) {
                uploadResponse.put("result", uploadParsed.getString("decodedBody"));
            } else {
                uploadResponse.put("error", "Upload lambda response parse error");
            }

            // Activity 3: Resize and upload thumbnail
            JSONObject resizePayload = new JSONObject();
            resizePayload.put("key", s3Key);
            resizePayload.put("content", content);
            JSONObject resizeRequest = new JSONObject();
            resizeRequest.put("body", resizePayload.toString());
            InvokeRequest resizeInvoke = InvokeRequest.builder()
                .functionName(RESIZE_FUNCTION_NAME)
                .payload(SdkBytes.fromUtf8String(resizeRequest.toString()))
                .build();
            InvokeResponse resizeResponseRaw = lambdaClient.invoke(resizeInvoke);
            JSONObject resizeParsed = parseLambdaProxyResponse(resizeResponseRaw.payload().asUtf8String());
            JSONObject resizeResponse = new JSONObject();
            if (resizeParsed.has("decodedBody")) {
                try {
                    resizeResponse = new JSONObject(resizeParsed.getString("decodedBody"));
                } catch (Exception e) {
                    resizeResponse.put("error", "Resize lambda response parse error");
                }
            } else {
                resizeResponse.put("error", "Resize lambda response parse error");
            }

            // Activity 1: Insert DB row
            logger.log("Waiting " + WAIT_TIME_SECONDS + " seconds before DB insert...");
            Thread.sleep(TimeUnit.SECONDS.toMillis(WAIT_TIME_SECONDS));
            JSONObject insertPayload = new JSONObject();
            insertPayload.put("Description", description);
            insertPayload.put("S3Key", s3Key);
            JSONObject insertRequest = new JSONObject();
            insertRequest.put("body", insertPayload.toString());
            InvokeRequest insertInvoke = InvokeRequest.builder()
                .functionName(INSERT_FUNCTION_NAME)
                .payload(SdkBytes.fromUtf8String(insertRequest.toString()))
                .build();
            InvokeResponse insertResponseRaw = lambdaClient.invoke(insertInvoke);
            JSONObject insertParsed = parseLambdaProxyResponse(insertResponseRaw.payload().asUtf8String());
            JSONObject insertResponse = new JSONObject();
            if (insertParsed.has("decodedBody")) {
                try {
                    insertResponse = new JSONObject(insertParsed.getString("decodedBody"));
                } catch (Exception e) {
                    insertResponse.put("error", "Insert lambda response parse error");
                }
            } else {
                insertResponse.put("error", "Insert lambda response parse error");
            }

            // Build orchestrator response
            response.put("orchestration", "completed");
            response.put("waitTime", WAIT_TIME_SECONDS + " seconds");
            response.put("activity1Result", insertResponse); // DB insert
            response.put("activity2Result", uploadResponse); // S3 upload
            response.put("activity3Result", resizeResponse); // Resize
            response.put("description", description);
            response.put("s3Key", s3Key);

            lambdaClient.close();
        } catch (Exception ex) {
            logger.log(ex.toString());
            response.put("orchestration", "failed");
            response.put("error", ex.getMessage());
        }
        String encodedResult = Base64.getEncoder().encodeToString(response.toString().getBytes());
        APIGatewayProxyResponseEvent apiResponse = new APIGatewayProxyResponseEvent();
        apiResponse.setStatusCode(200);
        apiResponse.setBody(encodedResult);
        apiResponse.withIsBase64Encoded(true);
        apiResponse.setHeaders(java.util.Collections.singletonMap("Content-Type", "application/json"));
        return apiResponse;
    }

    private JSONObject parseLambdaProxyResponse(String rawPayload) {
        JSONObject result = new JSONObject();
        try {
            JSONObject proxy = new JSONObject(rawPayload);
            String body = proxy.optString("body", "");
            boolean isBase64 = proxy.optBoolean("isBase64Encoded", false);
            if (isBase64) {
                byte[] decoded = Base64.getDecoder().decode(body);
                result.put("decodedBody", new String(decoded));
            } else {
                result.put("decodedBody", body);
            }
        } catch (Exception e) {
            // Fallback: maybe the rawPayload itself is already a base64 string
            try {
                byte[] decoded = Base64.getDecoder().decode(rawPayload);
                result.put("decodedBody", new String(decoded));
            } catch (Exception ignore) {
                result.put("error", "Unable to parse lambda response");
            }
        }
        return result;
    }
}