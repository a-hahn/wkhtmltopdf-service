# Build and Run instructions

## Clone the github repo
```
cd
git clone https://github.com/a-hahn/wkhtmltopdf-service.git
```
If you don't wan't to modify the source there is the compiled `wkhtmltopdf.jar` in the `target` folder. This file is built with the spring-boot ecosystem and contains all the necessary dependencies including an embedded tomcat runtime.
You can simply run it by executing
```
java -jar wkthmltopdf.jar
```
Note that wkhtmltopdf must be already installed. 

## Docker deployment

For deployment with docker simply use the Dockerfile which installs all required components *including wkhtmltopdf*. 
Creating a new docker image from a spring-boot .jar file:
```
cd wkhtmltopdf-service
sudo docker build -t a-hahn/wkhtmltopdf-service .
```
Run the image
```
sudo docker run --name wkhtmltopdf \
-d -p 3000:3000 -e TZ='Europe/Berlin' --tmpfs /tmp \
--restart=always a-hahn/wkhtmltopdf-service
```
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
