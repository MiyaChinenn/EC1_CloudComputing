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

public class LambdaDeletePhotoDB implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

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
            String requestBody = request.getBody();
            if (requestBody.startsWith("eyJ")) {
                requestBody = new String(Base64.getDecoder().decode(requestBody));
            }
            
            JSONObject inputData = new JSONObject(requestBody);
            String s3Key = inputData.getString("S3Key");
            
            Class.forName("com.mysql.cj.jdbc.Driver");
            Connection mySQLClient = DriverManager.getConnection(JDBC_URL, setMySqlConnectionProperties());
            
            PreparedStatement st = mySQLClient.prepareStatement(
                    "DELETE FROM Photos WHERE S3Key = ?"
            );
            st.setString(1, s3Key);
            
            int rowsAffected = st.executeUpdate();
            
            if (rowsAffected > 0) {
                response.put("success", true);
                response.put("message", "Photo deleted successfully");
                response.put("rowsDeleted", rowsAffected);
                response.put("s3Key", s3Key);
            } else {
                response.put("success", false);
                response.put("message", "No photo found with that S3Key");
                response.put("s3Key", s3Key);
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