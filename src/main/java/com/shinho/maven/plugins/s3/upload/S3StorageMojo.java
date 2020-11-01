package com.shinho.maven.plugins.s3.upload;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.*;
import com.amazonaws.event.ProgressEvent;
import com.amazonaws.event.ProgressListenerChain;
import com.amazonaws.internal.StaticCredentialsProvider;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.Headers;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.transfer.*;
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
    /**
     * enable是否允许上传下载操作.
     */
    @Parameter(property = "s3-storage.enable", defaultValue = "false")
    private boolean enable;

    @Parameter(property = "s3-storage.accessKey")
    private String accessKey;

    @Parameter(property = "s3-storage.secretKey")
    private String secretKey;

    @Parameter(property = "s3-storage.region")
    private String region;

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
        AmazonS3 s3 = getS3Client(accessKey, secretKey, region);
//        if (region != null) {
//            s3.setRegion(Region.getRegion(Regions.fromName(region)));
//        }

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

    private static AmazonS3 getS3Client(String accessKey, String secretKey, String region) {
        AWSCredentialsProvider provider;
        if (accessKey != null && secretKey != null) {
            BasicAWSCredentials awsCreds = new BasicAWSCredentials(accessKey, secretKey);
            provider = new AWSStaticCredentialsProvider(awsCreds);
        } else {
            provider = new DefaultAWSCredentialsProviderChain();
        }

        AmazonS3 s3 = AmazonS3ClientBuilder.standard()
                .withCredentials(provider)
                .withRegion(region)
                .build();
        return s3;
    }

    private boolean upload(AmazonS3 s3, String source) throws MojoExecutionException {
        List<File> files = getFiles(source);

        try {
            TransferManager mgr = TransferManagerBuilder.standard().withS3Client(s3).build();
            Transfer transfer;
            for (File file : files) {
                if (file.isFile()) {
                    String keyName = destination + "/" + file.getName();
                    keyName = keyName.replaceAll("[\\\\/]+", "/");

                    transfer = mgr.upload(new PutObjectRequest(bucketName, keyName, file)
                            .withCannedAcl(CannedAccessControlList.BucketOwnerFullControl));

                    printProgressBar(0.0);
                    transfer.addProgressListener(new ProgressListenerChain() {
                        public void progressChanged(ProgressEvent e) {
                            double pct = e.getBytesTransferred() * 100.0 / e.getBytes();
                            eraseProgressBar();
                            printProgressBar(pct);
                        }
                    });
                    transfer.waitForCompletion();
                    eraseProgressBar();
                    printProgressBar(transfer.getProgress().getPercentTransferred());
                    getLog().info(String.format("%s transferred %s bytes.",
                            file.getName(), transfer.getProgress().getBytesTransferred()));
                    if (transfer.getState() != Transfer.TransferState.Completed) {
                        getLog().info(String.format("File %s File transfer failed.",
                                file.getName()));
                        return false;
                    }
                }
            }
        } catch (AmazonServiceException e) {
            e.printStackTrace();
            getLog().error("Amazon service error: " + e.getMessage());
            return false;
        } catch (AmazonClientException e) {
            e.printStackTrace();
            getLog().error("Amazon client error: " + e.getMessage());
            return false;
        } catch (InterruptedException e) {
            getLog().info(String.format("File %s File transfer failed.",
                    source));
            return false;
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

    public static void printProgressBar(double pct) {
        // if bar_size changes, then change erase_bar (in eraseProgressBar) to
        // match.
        final int bar_size = 40;
        final String empty_bar = "                                        ";
        final String filled_bar = "########################################";
        int amt_full = (int) (bar_size * (pct / 100.0));
        System.out.format("  [%s%s]", filled_bar.substring(0, amt_full),
                empty_bar.substring(0, bar_size - amt_full));
    }

    // erases the progress bar.
    public static void eraseProgressBar() {
        // erase_bar is bar_size (from printProgressBar) + 4 chars.
        final String erase_bar = "\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b";
        System.out.format(erase_bar);
    }
}
