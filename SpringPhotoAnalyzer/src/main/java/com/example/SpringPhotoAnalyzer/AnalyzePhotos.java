package com.example.SpringPhotoAnalyzer;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.rekognition.RekognitionClient;
import software.amazon.awssdk.services.rekognition.model.DetectLabelsRequest;
import software.amazon.awssdk.services.rekognition.model.DetectLabelsResponse;
import software.amazon.awssdk.services.rekognition.model.Image;
import software.amazon.awssdk.services.rekognition.model.Label;
import software.amazon.awssdk.services.rekognition.model.RekognitionException;

/**
 * Uses the Amazon Recognition API to analyze the images.
 * The API is only availably from this regions:
	Asia Pacific (Mumbai)
	Europe (London)
	Europe (Ireland)
	Asia Pacific (Seoul)
	Asia Pacific (Tokyo)
	Asia Pacific (Singapore)
	Asia Pacific (Sydney)
	Europe (Frankfurt)
	US East (N. Virginia)
	US East (Ohio)
	US West (N. California)
	US West (Oregon)
 */
@Component
public class AnalyzePhotos {
	
	private Region region;
	
	private Logger logger = LoggerFactory.getLogger(AnalyzePhotos.class);
	
	@Autowired
	public AnalyzePhotos(Environment env) {
		this.region = Region.of(env.getProperty("aws.recognition.region"));
	}
	
	public ArrayList DetectLabels(byte[] bytes, String key) {

	    RekognitionClient rekClient = RekognitionClient.builder()
	            .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
	            .region(region)
	            .build();

	    try {

	        SdkBytes sourceBytes = SdkBytes.fromByteArray(bytes);
	        
	        // Create an Image object for the source image
	        Image souImage = Image.builder()
	                .bytes(sourceBytes)
	                .build();
	        
	        DetectLabelsRequest detectLabelsRequest = DetectLabelsRequest.builder()
	                .image(souImage)
	                .maxLabels(10)
	                .build();

		    DetectLabelsResponse labelsResponse = rekClient.detectLabels(detectLabelsRequest);

	        // Write the results to a WorkItem instance
	        List<Label> labels = labelsResponse.labels();

	        ArrayList list = new ArrayList<WorkItem>();
	        WorkItem item ;
	        for (Label label: labels) {
	            item = new WorkItem();
	            item.setKey(key); // identifies the photo
	            item.setConfidence(label.confidence().toString());
	            item.setName(label.name());
	            list.add(item);
	        }
	        return list;

	    } catch (RekognitionException e) {
	        logger.error("Ops, something didn't work out right: " + e.getMessage());
	        System.exit(1);
	    }
	    return null ;
	 }
}
