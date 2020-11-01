package com.shinho.maven.plugins.s3.upload;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.*;
import com.amazonaws.event.ProgressEvent;
import com.amazonaws.event.ProgressListenerChain;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.transfer.*;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;
import java.util.List;

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

    @Parameter(property = "s3-storage.region", required = true)
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

        return AmazonS3ClientBuilder.standard()
                .withCredentials(provider)
                .withRegion(region)
                .build();
    }

    private boolean upload(AmazonS3 s3, String source) {

        FilePatternResolver fpr = new FilePatternResolver(source);
        List<File> files = fpr.files();
        try {
            TransferManager mgr = TransferManagerBuilder.standard().withS3Client(s3).build();
            Transfer transfer;
            for (File file : files) {
                if (file.isFile()) {
                    String keyName = destination + "/" + file.getName();
                    keyName = keyName.replaceAll("[\\\\/]+", "/");

                    transfer = mgr.upload(new PutObjectRequest(bucketName, keyName, file)
                            .withCannedAcl(CannedAccessControlList.BucketOwnerFullControl));

                    ProgressBar.printProgressBar(0.0);
                    transfer.addProgressListener(new ProgressListenerChain() {
                        public void progressChanged(ProgressEvent e) {
                            double pct = e.getBytesTransferred() * 100.0 / e.getBytes();
                            ProgressBar.eraseProgressBar();
                            ProgressBar.printProgressBar(pct);
                        }
                    });
                    transfer.waitForCompletion();
                    ProgressBar.eraseProgressBar();
                    ProgressBar.printProgressBar(transfer.getProgress().getPercentTransferred());
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
            getLog().error("Amazon service error: " + e.getMessage());
            return false;
        } catch (AmazonClientException e) {
            getLog().error("Amazon client error: " + e.getMessage());
            return false;
        } catch (InterruptedException e) {
            getLog().info(String.format("File %s File transfer failed.",
                    source));
            return false;
        }

        return true;
    }

}
