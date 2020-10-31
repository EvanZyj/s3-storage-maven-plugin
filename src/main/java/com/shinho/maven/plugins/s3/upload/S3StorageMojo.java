package com.shinho.maven.plugins.s3.upload;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.internal.StaticCredentialsProvider;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.Headers;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.transfer.ObjectMetadataProvider;
import com.amazonaws.services.s3.transfer.Transfer;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.sun.org.apache.bcel.internal.generic.NOP;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Mojo(name = "s3-storage")
public class S3StorageMojo extends AbstractMojo {
    @Parameter(property = "s3-storage.accessKey")
    private String accessKey;

    @Parameter(property = "s3-storage.secretKey")
    private String secretKey;

    @Parameter(property = "s3-storage.region")
    private String region;

    /**
     * enable是否允许上传下载操作.
     */
    @Parameter(property = "s3-storage.enable", defaultValue = "false")
    private boolean enable;

    /**
     * The file to upload.
     */
    @Parameter(property = "s3-storage.source", required = true)
    private String source;

    /**
     * S3 bucket
     */
    @Parameter(property = "s3-storage.bucketName", required = true)
    private String bucketName;

    /**
     * destination.
     */
    @Parameter(property = "s3-storage.destination", required = true)
    private String destination;

    /**
     * endpoint for S3 regions.
     */
    @Parameter(property = "s3-storage.endpoint")
    private String endpoint;

    @Override
    public void execute() throws MojoExecutionException {

        if (!enable) {
            getLog().info("s3-storage is disabled.");
            return;
        }
        AmazonS3 s3 = getS3Client(accessKey, secretKey);
        s3.setRegion(Region.getRegion(Regions.fromName(region)));

        if (endpoint != null) {
            s3.setEndpoint(endpoint);
        }

        if (!s3.doesBucketExistV2(bucketName)) {
            throw new MojoExecutionException("Bucket doesn't exist: " + bucketName);
        }

        boolean success = upload(s3, source);
        if (!success) {
            throw new MojoExecutionException("Unable to upload file to S3.");
        }

        getLog().info(String.format("File %s uploaded to s3://%s/%s",
                source, bucketName, destination));
    }

    private static AmazonS3 getS3Client(String accessKey, String secretKey) {
        AWSCredentialsProvider provider;
        if (accessKey != null && secretKey != null) {
            AWSCredentials credentials = new BasicAWSCredentials(accessKey, secretKey);
            provider = new StaticCredentialsProvider(credentials);
        } else {
            provider = new DefaultAWSCredentialsProviderChain();
        }

        return new AmazonS3Client(provider);
    }

    private boolean upload(AmazonS3 s3, String source) throws MojoExecutionException {
        TransferManager mgr = new TransferManager(s3);

        Transfer transfer;
        List<File> files = getFiles(source);
        for(File file : files){
            if (file.isFile()) {
                String keyName = destination + "/" + file.getName();
                keyName = keyName.replaceAll("[\\\\/]+", "/");

                transfer = mgr.upload(new PutObjectRequest(bucketName, keyName, file)
                        .withCannedAcl(CannedAccessControlList.BucketOwnerFullControl));

                try {
                    getLog().debug("Transferring " + transfer.getProgress().getTotalBytesToTransfer() + " bytes...");
                    transfer.waitForCompletion();
                    getLog().info("Transferred " + transfer.getProgress().getBytesTransferred() + " bytes.");
                } catch (InterruptedException e) {
                    getLog().info(String.format("File %s File transfer failed.",
                            file.getName()));
                    return false;
                }
                if(transfer.getState() != Transfer.TransferState.Completed){
                    getLog().info(String.format("File %s File transfer failed.",
                            file.getName()));
                    return false;
                }
            }
        }

        return true;
    }

    private static List<File> getFiles(String source){
        source = source.replaceAll("[\\\\|/]+", "/");
        String pathReg = getRegPath(source);
        ArrayList<File> files = new ArrayList<File>();
        int lidx = source.lastIndexOf("/");
        String sourceDir = source.substring(0,lidx);

        File dir = new File(sourceDir);
        if (!dir.exists()) {
            return files;
        }

        for (File f : dir.listFiles()) {
            if (f.isFile()
                    && Pattern.matches(pathReg, f.getAbsolutePath())) {
                files.add(f);
            }
            /**
            else {
                getFiles(f.getAbsolutePath());
            }
             */
        }
        return files;
    }

    /**
     * 将通配符表达式转化为正则表达式
     *
     * @param path
     * @return
     */
    private static String getRegPath(String path) {
        char[] chars = path.toCharArray();
        int len = chars.length;
        StringBuilder sb = new StringBuilder();
        boolean preX = false;

        for (int i = 0; i < len; i++) {
            if (chars[i] == '*') {
                if (preX) {
                    sb.append(".*");
                    preX = false;
                } else if (i + 1 == len) {
                    sb.append("[^/\\\\]*");
                } else {
                    preX = true;
                    continue;
                }
            } else {
                if (preX) {
                    sb.append("[^/\\\\]*");
                    preX = false;
                }
                if (chars[i] == '?') {
                    sb.append('.');
                }else if(chars[i] == '.'){
                    sb.append("\\.");
                }else if(chars[i] == '\\'){
                    sb.append("/");
                }else if(chars[i] == '/'){
                    sb.append("[\\\\|/]+");
                }
                else {
                    sb.append(chars[i]);
                }
            }
        }
        return sb.toString();
    }
}
