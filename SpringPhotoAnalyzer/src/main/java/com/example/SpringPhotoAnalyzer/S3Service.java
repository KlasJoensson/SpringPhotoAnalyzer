package com.example.SpringPhotoAnalyzer;

import java.io.StringWriter;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.S3Object;

/**
 * Uses the Amazon S3 API to perform S3 operations.
 */
@Component
public class S3Service {

	private S3Client s3;
	
	private Region region;
	
	private Logger logger = LoggerFactory.getLogger(AnalyzePhotos.class);
	
	@Autowired
	public S3Service(Environment env) {
		this.region = Region.of(env.getProperty("aws.region"));
	}

	private S3Client getClient() {
		
	    // Create the S3Client object
	    S3Client s3 = S3Client.builder()
	            .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
	            .region(region)
	            .build();

	    return s3;
	  }

	public byte[] getObjectBytes (String bucketName, String keyName) {

	    s3 = getClient();

	    try {
	        // Create a GetObjectRequest instance
	        GetObjectRequest objectRequest = GetObjectRequest
	                .builder()
	                .key(keyName)
	                .bucket(bucketName)
	                .build();

	        // Get the byte[] from this S3 object
	        ResponseBytes<GetObjectResponse> objectBytes = s3.getObjectAsBytes(objectRequest);
	        byte[] data = objectBytes.asByteArray();
	        return data;

	    } catch (S3Exception e) {
	    	logger.error("Ops, something didn't work out with the S3-backet: " + e.awsErrorDetails().errorMessage());
	        System.exit(1);
	    }
	    return null;
	 }

	// Return the names of all images and data within an XML document
	public String ListAllObjects(String bucketName) {

	    s3 = getClient();
	    long sizeLg;
	    Instant DateIn;
	    BucketItem myItem ;

	    List bucketItems = new ArrayList<BucketItem>();
	    try {
	        ListObjectsRequest listObjects = ListObjectsRequest
	                .builder()
	                .bucket(bucketName)
	                .build();

	        ListObjectsResponse res = s3.listObjects(listObjects);
	        List<S3Object> objects = res.contents();

	        for (ListIterator iterVals = objects.listIterator(); iterVals.hasNext(); ) {
	            S3Object myValue = (S3Object) iterVals.next();
	            myItem = new BucketItem();
	            myItem.setKey(myValue.key());
	            myItem.setOwner(myValue.owner().displayName());
	            sizeLg = myValue.size() / 1024 ;
	            myItem.setSize(String.valueOf(sizeLg));
	            DateIn = myValue.lastModified();
	            myItem.setDate(String.valueOf(DateIn));

	            // Push the items to the list
	            bucketItems.add(myItem);
	        }

	        return convertToString(toXml(bucketItems));

	    } catch (S3Exception e) {
	    	logger.error("Ops, something didn't work out with listing the objects in the S3-bucket: " 
	    			+ e.awsErrorDetails().errorMessage());
	        System.exit(1);
	    }
	    return null ;
	  }

	// Return the names of all images in the given bucket
	public List ListBucketObjects(String bucketName) {

	    s3 = getClient();
	    String keyName ;

	    List keys = new ArrayList<String>();

	    try {
	        ListObjectsRequest listObjects = ListObjectsRequest
	                .builder()
	                .bucket(bucketName)
	                .build();

	        ListObjectsResponse res = s3.listObjects(listObjects);
	        List<S3Object> objects = res.contents();

	        for (ListIterator iterVals = objects.listIterator(); iterVals.hasNext(); ) {
	            S3Object myValue = (S3Object) iterVals.next();
	            keyName = myValue.key();
	            keys.add(keyName);
	        }

	       return keys;

	    } catch (S3Exception e) {
	    	logger.error("Ops, something didn't work out with listing the objects in the S3-bucket '" + bucketName + "': " 
	    			+ e.awsErrorDetails().errorMessage());
	        System.exit(1);
	    }
	    return null ;
	}


	// Place an image into an S3 bucket
	public String putObject(byte[] data, String bucketName, String objectKey) {

	    s3 = getClient();

	    try {
	        // Put a file into the bucket
	        PutObjectResponse response = s3.putObject(PutObjectRequest.builder()
	                        .bucket(bucketName)
	                        .key(objectKey)
	                        .build(),
	                RequestBody.fromBytes(data));

	        return response.eTag();

	    } catch (S3Exception e) {
	    	logger.error("Ops, could not put the object in the S3-bucket: " + e.awsErrorDetails().errorMessage());
	        System.exit(1);
	    }
	    return "";
	 }

	// Convert bucket item data into XML to pass back to the view
	private Document toXml(List<BucketItem> itemList) {

	    try {
	        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
	        DocumentBuilder builder = factory.newDocumentBuilder();
	        Document doc = builder.newDocument();

	        // Start building the XML
	        Element root = doc.createElement( "Items" );
	        doc.appendChild( root );

	        // Get the elements from the collection
	        int custCount = itemList.size();

	        // Iterate through the collection
	        for ( int index=0; index < custCount; index++) {

	            // Get the WorkItem object from the collection
	            BucketItem myItem = itemList.get(index);

	            Element item = doc.createElement( "Item" );
	            root.appendChild( item );

	            // Set Key
	            Element id = doc.createElement( "Key" );
	            id.appendChild( doc.createTextNode(myItem.getKey()) );
	            item.appendChild( id );

	            // Set Owner
	            Element name = doc.createElement( "Owner" );
	            name.appendChild( doc.createTextNode(myItem.getOwner() ) );
	            item.appendChild( name );

	            // Set Date
	            Element date = doc.createElement( "Date" );
	            date.appendChild( doc.createTextNode(myItem.getDate() ) );
	            item.appendChild( date );

	            // Set Size
	            Element desc = doc.createElement( "Size" );
	            desc.appendChild( doc.createTextNode(myItem.getSize() ) );
	            item.appendChild( desc );
	      }

	        return doc;
	    } catch(ParserConfigurationException e) {
	    	logger.error("Could not parse the xml: " + e.getLocalizedMessage());
	    }
	    return null;
	  }

	private String convertToString(Document xml) {
	    try {
	        Transformer transformer = TransformerFactory.newInstance().newTransformer();
	        StreamResult result = new StreamResult(new StringWriter());
	        DOMSource source = new DOMSource(xml);
	        transformer.transform(source, result);
	        return result.getWriter().toString();

	    } catch(TransformerException ex) {
	    	logger.error("Could not convert the xml to a string: " + ex.getLocalizedMessage());
	    }
	    return null;
	    }
}
