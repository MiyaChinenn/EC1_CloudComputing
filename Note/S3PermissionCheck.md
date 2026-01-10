# S3 Permission Testing Guide

## Method 1: Test Specific S3 Operations

### Check GetObject Permission (READ)
```bash
# Test downloading existing object
aws s3api get-object --bucket public-miyachinenn --key image.jpg test-download.jpg

```

### Check PutObject Permission (UPLOAD)
```bash
# Test uploading new object
aws s3api put-object --bucket public-miyachinenn --key test-upload.txt --body existing-file.jpg

```

### Check ListBucket Permission (LIST)
```bash
# Test listing bucket contents
aws s3 ls s3://public-miyachinenn/
```

## Method 2: IAM Policy Simulation (Admin Required)
```bash
# Simulate permissions (requires IAM admin access)
aws iam simulate-principal-policy \
  --policy-source-arn arn:aws:iam::051208505168:user/StartStopUser \
  --action-names s3:GetObject s3:PutObject s3:ListBucket \
  --resource-arns arn:aws:s3:::public-miyachinenn/* arn:aws:s3:::public-miyachinenn
```

## Method 3: Check Current Identity
```bash
# Verify which IAM user/role you're using
aws sts get-caller-identity
```

## Method 4: Test via AWS CLI S3 Commands
```bash
# Test high-level S3 operations
aws s3 cp s3://public-miyachinenn/image.jpg ./test.jpg    # Download test
aws s3 cp ./test.jpg s3://public-miyachinenn/test2.jpg    # Upload test
aws s3 ls s3://public-miyachinenn/                        # List test
```

## CURRENT StartStopUser PERMISSIONS SUMMARY:
- ✅ **s3:GetObject** - Can read/download objects (SERVLET WORKS!)
- ❌ **s3:ListBucket** - Cannot list bucket contents
- ❌ **s3:PutObject** - Cannot upload new files
- ✅ **ec2:StartInstances, ec2:StopInstances** - EC2 control

## FOR YOUR SERVLET:
Your Java servlet uses **GetObject** operation, which is ✅ **WORKING CORRECTLY**.

The servlet URL should work: http://18.141.225.147:8080/java-maven-webapp/object/image.jpg

## TO ADD UPLOAD CAPABILITY:
If you need upload functionality, the IAM policy needs to include:
```json
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Effect": "Allow",
            "Action": [
                "s3:PutObject",
                "s3:PutObjectAcl"
            ],
            "Resource": "arn:aws:s3:::public-miyachinenn/*"
        }
    ]
}
```