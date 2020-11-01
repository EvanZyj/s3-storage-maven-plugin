s3-storage-maven-plugin
======================
用于上传文件到S3的maven插件

配置说明
------------------------
plugin configuration
----------------------
```xml
    <plugin>
        <groupId>com.github.evanzyj</groupId>
        <artifactId>s3-storage-maven-plugin</artifactId>
        <version>1.0.3</version>
        <executions>
            <execution>
                <phase>package</phase>
                <goals>
                    <goal>s3-storage</goal>
                </goals>
                <configuration>
                    <!-- 是否启用文件上传操作 -->
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

plugin repository 使用jitpack.io提供的服务
------------------------------------
```xml
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
```

