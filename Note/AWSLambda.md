# Lambda S3 GetObject Function Deployment

## STEP 0: Navigate to Lambda Project Directory
cd C:\Users\Nguyen\Dropbox\Elective1-Cloud\LambdaGetListOfObjects

## STEP 1: Build Lambda JAR
mvn clean install

## STEP 2: Locate JAR File
target\LambdaGetListOfObjects-1.0-SNAPSHOT.jar

## STEP 3: Upload to AWS Lambda Console
- Go to AWS Lambda Console
- Select your function
- Upload JAR file from target directory

## STEP 4: Configure Lambda Handler
Handler: vgu.cloud26.LambdaGetObject::handleRequest

## STEP 5: Test Lambda Function
Test Event (JSON):
```json
{
  "body": "{\"key\":\"cyrene.png\"}"
}
```

## SUCCESS! Test Results:
- Status Code: 200
- Content-Type: image/png
- Body: Base64-encoded image data

## Additional Test Cases:
```json
{"body": "{\"key\":\"image.jpg\"}"}
{"body": "{\"key\":\"yuki.png\"}"}
```

## Lambda Function Features:
- S3 Bucket: public-miyachinenn
- Max File Size: 10MB
- Supported Types: PNG, HTML, others
- Input: JSON body with "key" parameter
- Output: Base64-encoded file data

## API Gateway Integration URL:
https://your-api-gateway-url/prod/getObject

## Test via API Gateway:
```bash
curl -X POST https://your-lambda-url \
  -H "Content-Type: application/json" \
  -d '{"key":"cyrene.png"}'
```