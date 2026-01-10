package vgu.cloud26;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.util.Base64;
import java.util.Properties;

import org.json.JSONObject;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;

import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.rds.RdsUtilities;
import software.amazon.awssdk.services.rds.model.GenerateAuthenticationTokenRequest;

public class LambdaInsertPhotoDB implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private static final String RDS_INSTANCE_HOSTNAME
            = "clouddb26.cvm8gi4qkev2.ap-southeast-1.rds.amazonaws.com";
    private static final int RDS_INSTANCE_PORT = 3306;
    private static final String DB_USER = "cloud26";
    private static final String JDBC_URL
            = "jdbc:mysql://" + RDS_INSTANCE_HOSTNAME
            + ":" + RDS_INSTANCE_PORT + "/Cloud26";

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent request, Context context) {
        LambdaLogger logger = context.getLogger();
        JSONObject response = new JSONObject();
        
        try {
            // Parse input from request body
            String requestBody = request.getBody();
            if (requestBody.startsWith("eyJ")) {
                // Decode base64 if needed
                requestBody = new String(Base64.getDecoder().decode(requestBody));
            }
            
            JSONObject inputData = new JSONObject(requestBody);
            
            // Extract required fields
            String description = inputData.getString("Description");
            String s3Key = inputData.getString("S3Key");
            
            // Connect to database
            Class.forName("com.mysql.cj.jdbc.Driver");
            Connection mySQLClient = DriverManager.getConnection(JDBC_URL, setMySqlConnectionProperties());
            
            // Insert new photo record
            PreparedStatement st = mySQLClient.prepareStatement(
                    "INSERT INTO Photos (Description, S3Key) VALUES (?, ?)",
                    PreparedStatement.RETURN_GENERATED_KEYS
            );
            st.setString(1, description);
            st.setString(2, s3Key);
            
            int rowsAffected = st.executeUpdate();
            
            if (rowsAffected > 0) {
                // Get the generated ID
                var generatedKeys = st.getGeneratedKeys();
                int newId = -1;
                if (generatedKeys.next()) {
                    newId = generatedKeys.getInt(1);
                }
                
                response.put("success", true);
                response.put("message", "Photo inserted successfully");
                response.put("id", newId);
                response.put("description", description);
                response.put("s3Key", s3Key);
            } else {
                response.put("success", false);
                response.put("message", "Failed to insert photo");
            }
            
            st.close();
            mySQLClient.close();
            
            
        } catch (Exception ex) {
            logger.log(ex.toString());
        }
        
        String encodedResult = Base64.getEncoder().encodeToString(response.toString().getBytes());

        APIGatewayProxyResponseEvent apiResponse = new APIGatewayProxyResponseEvent();
        apiResponse.setStatusCode(200);
        apiResponse.setBody(encodedResult);
        apiResponse.withIsBase64Encoded(true);
        apiResponse.setHeaders(java.util.Collections.singletonMap("Content-Type", "application/json"));
        return apiResponse;
    }

    private static Properties setMySqlConnectionProperties() throws Exception {
        Properties mysqlConnectionProperties = new Properties();
        mysqlConnectionProperties.setProperty("useSSL", "true");
        mysqlConnectionProperties.setProperty("user", DB_USER);
        mysqlConnectionProperties.setProperty("password", generateAuthToken());
        return mysqlConnectionProperties;
    }

    private static String generateAuthToken() throws Exception {
        RdsUtilities rdsUtilities = RdsUtilities.builder().build();

        String authToken = rdsUtilities.generateAuthenticationToken(
                GenerateAuthenticationTokenRequest.builder()
                        .hostname(RDS_INSTANCE_HOSTNAME)
                        .port(RDS_INSTANCE_PORT)
                        .username(DB_USER)
                        .region(Region.AP_SOUTHEAST_1)
                        .credentialsProvider(DefaultCredentialsProvider.create())
                        .build());
        return authToken;
    }
}