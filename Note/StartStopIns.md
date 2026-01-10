# AWS EC2 Start/Stop - IAM User Method

## STEP 1: Create IAM User and Group
# User: StartStopUser
# Group: Cloud26
# Policy: StartStopPolicy (Start/Stop EC2 permissions)

## STEP 2: Create Access Keys
# IAM Console → Users → StartStopUser → Security credentials → Create access key
# Download: StartStopUser_accessKeys.csv

## STEP 3: Configure AWS CLI
aws configure
# Enter Access Key ID, Secret Access Key, region: ap-southeast-1, format: json

## STEP 4: Verify IAM User Identity
aws sts get-caller-identity

## STEP 5: List EC2 Instances
aws ec2 describe-instances --query "Reservations[*].Instances[*].[InstanceId,State.Name,InstanceType,Tags[?Key=='Name'].Value|[0]]" --output table

## STEP 6: Stop EC2 Instance
aws ec2 stop-instances --instance-ids i-08c7bb234832fbb3b

# Verify stopped:
aws ec2 describe-instances --instance-ids i-08c7bb234832fbb3b --query "Reservations[*].Instances[*].[InstanceId,State.Name]" --output table

## STEP 7: Start EC2 Instance
aws ec2 start-instances --instance-ids i-08c7bb234832fbb3b

# Verify started:
aws ec2 describe-instances --instance-ids i-08c7bb234832fbb3b --query "Reservations[*].Instances[*].[InstanceId,State.Name]" --output table

## STEP 8: Get New Public IP (After Start)
aws ec2 describe-instances --instance-ids i-08c7bb234832fbb3b --query "Reservations[0].Instances[0].PublicIpAddress" --output text

## Quick Check Instance Status:
aws ec2 describe-instances --instance-ids i-08c7bb234832fbb3b --query "Reservations[0].Instances[0].[InstanceId,State.Name,PublicIpAddress]" --output table
