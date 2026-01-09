# Deploy HTML to Tomcat ROOT Directory

## STEP 1: Start EC2 Instance
aws ec2 start-instances --instance-ids i-08c7bb234832fbb3b

## STEP 2: Get Instance IP Address
aws ec2 describe-instances --instance-ids i-08c7bb234832fbb3b --query "Reservations[0].Instances[0].{State:State.Name,PublicIP:PublicIpAddress}" --output table

## STEP 3: Upload HTML File to EC2
cd C:\Users\Nguyen\Dropbox\Elective1-Cloud\HelloWorldApp
scp -o StrictHostKeyChecking=no -i "..\data\Cloud26.pem" index.html ubuntu@18.141.225.147:~

## STEP 4: Verify File Upload
ssh -o StrictHostKeyChecking=no -i "..\data\Cloud26.pem" ubuntu@18.141.225.147 "ls -lh ~/"

## STEP 5: Move HTML to Tomcat ROOT Directory
ssh -o StrictHostKeyChecking=no -i "..\data\Cloud26.pem" ubuntu@18.141.225.147 "sudo cp ~/index.html /opt/tomcat/webapps/ROOT/ && sudo chown tomcat:tomcat /opt/tomcat/webapps/ROOT/index.html && sudo chmod 644 /opt/tomcat/webapps/ROOT/index.html && ls -lh /opt/tomcat/webapps/ROOT/index.html"

## SUCCESS! Access URLs:

# IP Address URL (Root):
http://18.141.225.147:8080/

# IP Address URL (Direct):
http://18.141.225.147:8080/index.html

# AWS DNS URL (Root):
http://ec2-18-141-225-147.ap-southeast-1.compute.amazonaws.com:8080/

# AWS DNS URL (Direct):
http://ec2-18-141-225-147.ap-southeast-1.compute.amazonaws.com:8080/index.html

## Notes:
- ROOT directory = Default Tomcat web application
- Files in ROOT are accessible at http://server:8080/ (no app name needed)
- This is the simplest deployment method for single HTML files