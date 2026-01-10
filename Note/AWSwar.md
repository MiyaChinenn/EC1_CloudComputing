# WAR File Deployment to Tomcat on AWS EC2

## STEP 0: Navigate to Project Directory
cd C:\Users\Nguyen\Dropbox\Elective1-Cloud\HelloWorldApp

## STEP 1: Build WAR File
jar -cvf HelloWorldApp.war index.html WEB-INF

## STEP 2: Upload WAR to EC2
scp -o StrictHostKeyChecking=no -i "..\data\Cloud26.pem" HelloWorldApp.war ubuntu@18.141.225.147:~

## STEP 3: Remove Old Deployment (Clean Start)
ssh -o StrictHostKeyChecking=no -i "..\data\Cloud26.pem" ubuntu@18.141.225.147 "sudo rm -rf /opt/tomcat/webapps/HelloWorldApp && sudo rm -f /opt/tomcat/webapps/HelloWorldApp.war"

## STEP 4: Deploy WAR to Tomcat webapps
ssh -o StrictHostKeyChecking=no -i "..\data\Cloud26.pem" ubuntu@18.141.225.147 "sudo cp ~/HelloWorldApp.war /opt/tomcat/webapps/ && sudo chown tomcat:tomcat /opt/tomcat/webapps/HelloWorldApp.war && ls -lh /opt/tomcat/webapps/"

## STEP 5: Wait for Tomcat to Auto-Extract WAR
timeout /t 10 /nobreak

## STEP 6: Compile Servlet + Fix Permissions
ssh -o StrictHostKeyChecking=no -i "..\data\Cloud26.pem" ubuntu@18.141.225.147 "sudo javac -cp /opt/tomcat/lib/servlet-api.jar /opt/tomcat/webapps/HelloWorldApp/WEB-INF/classes/HelloWorldServlet.java && sudo chown tomcat:tomcat /opt/tomcat/webapps/HelloWorldApp/WEB-INF/classes/HelloWorldServlet.class && sudo chmod 644 /opt/tomcat/webapps/HelloWorldApp/WEB-INF/classes/HelloWorldServlet.class"

## STEP 7: Restart Tomcat (Load Compiled Servlet)
ssh -o StrictHostKeyChecking=no -i "..\data\Cloud26.pem" ubuntu@18.141.225.147 "sudo systemctl restart tomcat"

## SUCCESS! Access URLs:
# Main Page:
http://18.141.225.147:8080/HelloWorldApp/

# Servlet:
http://18.141.225.147:8080/HelloWorldApp/hello

# AWS DNS URLs:
http://ec2-18-141-225-147.ap-southeast-1.compute.amazonaws.com:8080/HelloWorldApp/
http://ec2-18-141-225-147.ap-southeast-1.compute.amazonaws.com:8080/HelloWorldApp/hello

## Verify WAR Deployment:
ssh -o StrictHostKeyChecking=no -i "..\data\Cloud26.pem" ubuntu@18.141.225.147 "ls -lh /opt/tomcat/webapps/"