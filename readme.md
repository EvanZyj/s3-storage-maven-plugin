s3-storage-maven-plugin
======================
Maven plug-in for uploading files to S3.Support any wildcard path,such as `*,**,?`

用于上传文件到S3的maven插件,支持任何通配符路径.
```shell script
/opt/*/*.jar
/opt/**.jar
```

Plugin configuration
----------------------------
```xml
    <plugin>
        <groupId>com.github.evanzyj</groupId>
        <artifactId>s3-storage-maven-plugin</artifactId>
        <version>1.0.5</version>
        <executions>
            <execution>
                <phase>package</phase>
                <goals>
                    <goal>s3-storage</goal>
                </goals>
                <configuration>
                    <enable>true</enable> 
                    <accessKey>AWS accessKey</accessKey>
                    <secretKey>AWS secretKey</secretKey>
                    <region>cn-north-1</region>
                    <bucketName>s3-001-aio-prd-bjs</bucketName>
                    <source>${basedir}/target/dailysalary-*.jar</source>
                    <destination>aiodp/</destination>
                </configuration>
            </execution>
        </executions>
    </plugin>
```

Plugin repository.
----------------------------
**Before version 1.0.4, you need to use the following configuration.**
Use the service provided by jitpack.io.
```xml
    <pluginRepositories>
        <pluginRepository>
            <id>jitpack.io</id>
            <url>https://jitpack.io</url>
        </pluginRepository>
    </pluginRepositories>
```