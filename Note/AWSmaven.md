# Deploy Java Maven S3 Servlet to Tomcat

## STEP 1: Start EC2 Instance
aws ec2 start-instances --instance-ids i-08c7bb234832fbb3b

## STEP 2: Get Instance IP Address
aws ec2 describe-instances --instance-ids i-08c7bb234832fbb3b --query "Reservations[0].Instances[0].{State:State.Name,PublicIP:PublicIpAddress}" --output table

## STEP 3: Build Maven Project
cd C:\Users\Nguyen\Dropbox\Elective1-Cloud\JavaMavenProject
mvn clean package

## STEP 4: Upload WAR File to EC2
scp -o StrictHostKeyChecking=no -i "..\data\Cloud26.pem" target\java-maven-webapp.war ubuntu@18.141.225.147:~

## STEP 5: Verify WAR Upload
ssh -o StrictHostKeyChecking=no -i "..\data\Cloud26.pem" ubuntu@18.141.225.147 "ls -lh ~/java-maven-webapp.war"

## STEP 6: Deploy WAR to Tomcat
ssh -o StrictHostKeyChecking=no -i "..\data\Cloud26.pem" ubuntu@18.141.225.147 "sudo cp ~/java-maven-webapp.war /opt/tomcat/webapps/ && sudo chown tomcat:tomcat /opt/tomcat/webapps/java-maven-webapp.war && sudo systemctl restart tomcat && ls -lh /opt/tomcat/webapps/"

## SUCCESS! Access URLs:

# IP Address URL (App):
http://18.141.225.147:8080/java-maven-webapp/

# IP Address URL (S3 Servlet):
http://18.141.225.147:8080/java-maven-webapp/object/image.jpg

# AWS DNS URL (App):
http://ec2-18-141-225-147.ap-southeast-1.compute.amazonaws.com:8080/java-maven-webapp/

# AWS DNS URL (S3 Servlet):
http://ec2-18-141-225-147.ap-southeast-1.compute.amazonaws.com:8080/java-maven-webapp/object/image.jpg

## NEW JavaScript Features:

**JavaScript Functions Available:**
- `testServlet()` - Test servlet connectivity and show response details
- `loadImage('filename.jpg')` - Dynamically load single image from S3
- `loadMultipleImages(['file1.jpg', 'file2.png'])` - Load multiple images
- `fetchObjectWithRole('key')` - Direct API call to servlet

**Files Added:**
- `/js/s3-image-loader.js` - JavaScript module for S3 integration
- Updated `/index.html` - Interactive demo with buttons

## Notes:
- WAR file automatically extracts to `/opt/tomcat/webapps/java-maven-webapp/`
- S3 servlet serves images directly from AWS S3 bucket: `public-miyachinenn`
- Tomcat 10.1.28 supports Jakarta EE 6.0 (upgraded from Tomcat 9)
- Uses Instance Profile Credentials (no hardcoded credentials in code)
- Interactive JavaScript functionality with `s3-image-loader.js`
- Dynamic image loading with `fetchObjectWithRole()` function
- IAM permissions: S3 GetObject + EC2 Start/Stop (StartStopUser fallback)

cd JavaMavenProject && mvn clean package -q
cd .. && scp -o StrictHostKeyChecking=no -i "data\Cloud26.pem" JavaMavenProject\target\java-maven-webapp.war ubuntu@18.141.225.147:~/java-maven-webapp-table.war
ssh -o StrictHostKeyChecking=no -i "data\Cloud26.pem" ubuntu@18.141.225.147 "sudo rm -rf /opt/tomcat/webapps/java-maven-webapp* && sudo cp ~/java-maven-webapp-table.war /opt/tomcat/webapps/java-maven-webapp.war && sudo chown tomcat:tomcat /opt/tomcat/webapps/java-maven-webapp.war && sudo systemctl restart tomcat"
timeout /t 15 /nobreak
ssh -o StrictHostKeyChecking=no -i "data\Cloud26.pem" ubuntu@18.141.225.147 "sudo ls -la /opt/tomcat/webapps/ | grep java"
http://18.141.225.147:8080/java-maven-webapp/