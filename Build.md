# Build and Run instructions

## Clone and build the github repo
```
git clone https://github.com/a-hahn/wkhtmltopdf-service.git
```

For deployment with docker simply use the Dockerfile which installs all required components *including wkhtmltopdf*. 
Creating a new docker image from a spring-boot .jar file:
```
cd wkhtmltopdf-service
sudo docker build -t <your-image-name> .
```

**OR**

## pull from github packages

```
sudo docker pull ghcr.io/a-hahn/wkhtmltopdf-service/wkhtmltopdf-service:1.0.0
```

## Docker deployment
Run the image
```
sudo docker run --name wkhtmltopdf \
-d -p 3000:3000 -e TZ='Europe/Berlin' --tmpfs /tmp \
--restart=always \
ghcr.io/a-hahn/wkhtmltopdf-service/wkhtmltopdf-service:latest
```
(Replace docker image name with your own <your-image-name> in case you build yourself)

Now just send your pdf conversion requests to
```
http:/<ip_of_your_host>:3000
```
See the [Examples](Examples.md) 

## Start / Stop the container
```
sudo docker stop wkhmtltopdf
sudo docker start wkhmtltopdf
```

## Look into the log files

Once the container is running its possible to inspect log
```
sudo docker exec -it wkhtmltopdf tailf wkhtmltopdf.log
```

## Usage without Docker

For ad-hoc usage where wkhtmltopdf is already installed there is a compiled `wkhtmltopdf.jar` in the `target` folder. This file is built with the spring-boot ecosystem and contains all the necessary dependencies including an embedded tomcat runtime.
You can simply run it by executing
```
java -jar wkthmltopdf.jar
```
