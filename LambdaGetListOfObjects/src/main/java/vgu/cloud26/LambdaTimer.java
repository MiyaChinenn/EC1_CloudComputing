package vgu.cloud26;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;

public class LambdaTimer implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent request, Context context) {

        try {
            Thread.sleep(30 * 1000); // 30 seconds * 1000 milliseconds/second
        } catch (InterruptedException e) {
            // Handle the InterruptedException, which occurs if another thread
            // interrupts the current thread while it is sleeping.
            Thread.currentThread().interrupt(); // Restore the interrupted status
            System.err.println("Thread sleep was interrupted: " + e.getMessage());
        }

        String message = "Finished";
        APIGatewayProxyResponseEvent response
                = new APIGatewayProxyResponseEvent();
        response.setStatusCode(200);
        response.setBody(message);
        //response.withIsBase64Encoded(true);
        response.setHeaders(java.util.Collections
                .singletonMap("Content-Type", "plain/text"));
        return response;
    }

}