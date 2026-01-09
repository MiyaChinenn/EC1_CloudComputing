# Tomcat Installation on Ubuntu EC2

## Check EC2 instances
```bash
aws ec2 describe-instances --query "Reservations[*].Instances[*].[InstanceId,State.Name,PublicIpAddress,Tags[?Key=='Name'].Value|[0]]" --output table
```

## Connect to EC2 instance
```bash
ssh -i data/cloud26.pem ubuntu@18.141.225.147
```

## Verify environment and start installation
```bash
whoami && pwd && echo "Starting Tomcat Installation..."
```

## Update system and install Java
```bash
sudo apt update && sudo apt install -y default-jdk
java -version
```

## Create tomcat user and group
```bash
sudo groupadd tomcat && sudo useradd -s /bin/false -g tomcat -d /opt/tomcat tomcat
```

## Download Tomcat 9.0.65
```bash
cd /tmp
wget https://archive.apache.org/dist/tomcat/tomcat-9/v9.0.65/bin/apache-tomcat-9.0.65.tar.gz
ls -lh apache-tomcat-9.0.65.tar.gz
```

## Create installation directory and extract Tomcat
```bash
sudo mkdir -p /opt/tomcat
sudo tar xzvf /tmp/apache-tomcat-9.0.65.tar.gz -C /opt/tomcat --strip-components=1
```

## Set permissions
```bash
sudo chown -R tomcat:tomcat /opt/tomcat/
sudo chmod -R 755 /opt/tomcat/
```

## Verify installation
```bash
ls -la /opt/tomcat/
```

## Get JAVA_HOME path
```bash
readlink -f /usr/bin/java | sed "s:bin/java::"
```

sudo tee /etc/systemd/system/tomcat.service > /dev/null << 'EOF'
[Unit]
Description=Tomcat webs servlet container
After=network.target

[Service]
Type=forking

User=tomcat
Group=tomcat

Environment="JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64"
Environment="JAVA_OPTS=-Djava.security.egd=file:///dev/urandom"
Environment="CATALINA_BASE=/opt/tomcat"
Environment="CATALINA_HOME=/opt/tomcat"
Environment="CATALINA_PID=/opt/tomcat/temp/tomcat.pid"
Environment="CATALINA_OPTS=-Xms512M -Xmx1024M -server -XX:+UseParallelGC"

ExecStart=/opt/tomcat/bin/startup.sh
ExecStop=/opt/tomcat/bin/shutdown.sh

RestartSec=10
Restart=always

[Install]
WantedBy=multi-user.target
EOF

## Create systemd service file
```bash

sudo cp /opt/tomcat/conf/tomcat-users.xml /opt/tomcat/conf/tomcat-users.xml.bak

sudo tee /opt/tomcat/conf/tomcat-users.xml > /dev/null << 'EOF'
<?xml version="1.0" encoding="UTF-8"?>
<tomcat-users xmlns="http://tomcat.apache.org/xml"
              xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
              xsi:schemaLocation="http://tomcat.apache.org/xml tomcat-users.xsd"
              version="1.0">
  <role rolename="manager-gui"/>
  <role rolename="manager-script"/>
  <role rolename="manager-jmx"/>
  <role rolename="manager-status"/>
  <role rolename="admin-gui"/>
  <role rolename="admin-script"/>
  <user username="admin" password="admin123" roles="manager-gui,manager-script,manager-jmx,manager-status,admin-gui,admin-script"/>
</tomcat-users>
EOF

sudo tee /opt/tomcat/webapps/manager/META-INF/context.xml > /dev/null << 'EOF'
<?xml version="1.0" encoding="UTF-8"?>
<Context antiResourceLocking="false" privileged="true" >
  <CookieProcessor className="org.apache.tomcat.util.http.Rfc6265CookieProcessor"
                   sameSiteCookies="strict" />
<!--
  <Valve className="org.apache.catalina.valves.RemoteAddrValve"
         allow="127\.0\.0\.1|::1|0:0:0:0:0:0:0:1" />
-->
  <Manager sessionAttributeValueClassNameFilter="java\.lang\.(?:Boolean|Integer|Long|Number|String)|org\.apache\.catalina\.filters\.CsrfPreventionFilter\$LruCache(?:\$1)?|java\.util\.(?:Linked)?HashMap"/>
</Context>
EOF

sudo tee /opt/tomcat/webapps/host-manager/META-INF/context.xml > /dev/null << 'EOF'
<?xml version="1.0" encoding="UTF-8"?>
<Context antiResourceLocking="false" privileged="true" >
  <CookieProcessor className="org.apache.tomcat.util.http.Rfc6265CookieProcessor"
                   sameSiteCookies="strict" />
<!--
  <Valve className="org.apache.catalina.valves.RemoteAddrValve"
         allow="127\.0\.0\.1|::1|0:0:0:0:0:0:0:1" />
-->
  <Manager sessionAttributeValueClassNameFilter="java\.lang\.(?:Boolean|Integer|Long|Number|String)|org\.apache\.catalina\.filters\.CsrfPreventionFilter\$LruCache(?:\$1)?|java\.util\.(?:Linked)?HashMap"/>
</Context>
EOF

# Apply configuration changes
sudo systemctl restart tomcat

# Final verification
sudo systemctl status tomcat --no-pager
curl -s http://localhost:8080 | head -5

# Clean up
rm /tmp/apache-tomcat-9.0.65.tar.gz

# Access URLs:
# Main: http://18.141.225.147:8080
# Manager: http://18.141.225.147:8080/manager/html (admin/admin123)
# Examples: http://18.141.225.147:8080/examples/