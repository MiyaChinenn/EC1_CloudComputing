# EC1_CloudComputing

## Project Description

Built a serverless image management web app with AWS Lambda (Java 21) and RESTful APIs for photo CRUD operations.

Orchestrated microservices + RDS/IAM auth on MySQL for photos, thumbnails, and synchronized S3/database operations.

Added automated resizing (100x100px), base64 image handling, coordinated Lambda workflows, and responsive HTML frontend.

**Stack:** Java 21, AWS Lambda, S3, RDS MySQL, API Gateway, Maven, HTML/JavaScript, IAM, RESTful APIs

## Contents

**LambdaGetListOfObjects/** – Main Lambda microservices project with Java source code
- Multiple Lambda functions for S3 operations, image resizing, and database management
- Maven-based build with AWS SDK v2 dependencies

**JavaMavenProject/** – Example Java webapp for S3 file retrieval

**HelloWorldApp/** – Simple servlet example

**Note/** – Documentation and reference guides (AWS setup, Lambda, Maven, WAR deployment)

**data/** – Configuration and credentials (excluded from Git)

## Branches

- **master** (this branch): Full working code with documented Lambda functions and HTML frontend
- **main**: Official release branch with stable version

## Prerequisites

- **JDK 21** (or JDK 17+)
- **Maven 3.8+** for building Lambda packages
- **AWS Account** with:
  - IAM role configured for Lambda execution
  - S3 buckets created (`public-miyachinenn`, `public-miyachinenn-thumbnails`, `cloud26-mgc`)
  - RDS MySQL instance with proper security groups
  - API Gateway REST API configured
- **Git** for version control

## Quick Start

Clone and checkout master:
```bash
git clone https://github.com/MiyaChinenn/EC1_CloudComputing.git
cd EC1_CloudComputing
git checkout master
```

Build Lambda packages with Maven:
```bash
cd LambdaGetListOfObjects
mvn clean package
```

Deploy to AWS Lambda:
```bash
# Use AWS CLI or AWS Console to upload the JAR from target/ directory
aws lambda update-function-code \
  --function-name LambdaGetListOfObjects \
  --zip-file fileb://target/LambdaEntryPoint-1.0-SNAPSHOT.jar
```

## Build & Test

From the repo root or inside `LambdaGetListOfObjects/`:

```bash
cd LambdaGetListOfObjects

# Build all Lambda functions
mvn clean package

# Run unit tests
mvn test

# View compiled classes
ls target/classes/vgu/cloud26/
```

On Windows cmd, use the same Maven commands (Maven is cross-platform).

## App Notes

**Lambda Functions:**
- `LambdaEntryPoint`: API Gateway entry point; invokes other Lambdas synchronously
- `LambdaGetListOfObjects`: Lists S3 objects from `cloud26-mgc` bucket
- `LambdaUploadObject`: Uploads base64-encoded files to S3
- `LambdaGetObject`: Retrieves files (handles thumbnail routing)
- `LambdaDeleteObject`: Deletes original and thumbnail objects
- `LambdaResizer`: Event-driven; triggers on S3 upload to auto-generate thumbnails
- `LambdaResizerManual`: Manual thumbnail generation for orchestrated workflows
- `LambdaOrchestrator`: Coordinates multi-step photo upload + DB insert + thumbnail creation
- `LambdaGetPhotosDB`, `LambdaInsertPhotoDB`, `LambdaDeletePhotoDB`: RDS MySQL operations with IAM auth
- `LambdaTimer`: Simple 30-second sleep function for testing

**Database Configuration:**
- RDS Endpoint: `clouddb26.cvm8gi4qkev2.ap-southeast-1.rds.amazonaws.com:3306`
- Database: `Cloud26`
- Auth: IAM database authentication (no hardcoded passwords)
- Table: `Photos` (ID, Description, S3Key)

**API Gateway:**
- Base URL: Configure in your frontend to point to API Gateway REST endpoint
- Content encoding: Base64 for image uploads/downloads
- CORS: May need to enable for web frontend

**Region:** ap-southeast-1 (Singapore)

**Thumbnail Size:** 100x100px (proportional scaling)

## Frontend

HTML frontend is located in `LambdaGetListOfObjects/index.html`. It provides:
- Photo upload with base64 encoding
- Gallery view of all photos
- Thumbnail display
- Delete photo functionality
- Audio/TTS integration (if configured)

Serve via API Gateway or S3 static website hosting.

## Notes

- All Lambda functions use AWS SDK v2 (software.amazon.awssdk)
- MySQL JDBC driver is bundled in the Maven POM for RDS connectivity
- Ensure Lambda execution role has S3, RDS, and Lambda invoke permissions
- Large artifacts (compiled WAR, JAR files) are in `target/` and `.gitignore`d