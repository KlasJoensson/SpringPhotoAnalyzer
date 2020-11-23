package com.example.SpringPhotoAnalyzer;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

/**
 * Used as the Spring Boot controller that handles HTTP requests.
 */
@Controller
public class PhotoController {

	private S3Service s3Client;
	private AnalyzePhotos photos;
	private WriteExcel excel;
	private SendMessages sendMessage;
	
	@Autowired
	public PhotoController(SendMessages sendMessage, WriteExcel excel, AnalyzePhotos photos, S3Service s3Client) {
		this.sendMessage = sendMessage;
		this.excel = excel;
		this.photos = photos;
		this.s3Client = s3Client;
	}
	
	private Logger logger = LoggerFactory.getLogger(PhotoController.class);

	@GetMapping("/")
	public String root() {
		return "index";
	}

	@GetMapping("/process")
	public String process() {
		return "process";
	}

	@GetMapping("/photo")
	public String photo() {
		return "upload";
	}

	@RequestMapping(value = "/getimages", method = RequestMethod.GET)
	@ResponseBody
	String getImages(HttpServletRequest request, HttpServletResponse response) {

		return s3Client.ListAllObjects("scottphoto");
	}

	// Generate a report that analyzes photos in a given bucket
	@RequestMapping(value = "/report", method = RequestMethod.POST)
	@ResponseBody
	String report(HttpServletRequest request, HttpServletResponse response) {

		String email = request.getParameter("email");

		// Get a list of key names in the given bucket
		List myKeys =  s3Client.ListBucketObjects("scottphoto");

		// Create a list to store the data
		List myList = new ArrayList<List>();

		// Loop through each element in the List
		int len = myKeys.size();
		for (int z=0 ; z < len; z++) {

			String key = (String) myKeys.get(z);
			byte[] keyData = s3Client.getObjectBytes ("scottphoto", key);
			//myMap.put(key, keyData);

			// Analyze the photo
			ArrayList item =  photos.DetectLabels(keyData, key);
			myList.add(item);
		}

		// Now we have a list of WorkItems that have all of the analytical data describing the photos in the S3 bucket
		InputStream excelData = excel.exportExcel(myList);

		try {
			// Email the report
			sendMessage.sendReport(excelData, email);

		} catch (Exception e) {
			logger.debug("Ops, somethong went wrong when sending the message: " + e.getLocalizedMessage());
		}
		return "The photos have been analyzed and the report is sent.";
	}

	// Upload an image to send to an S3 bucket
	@RequestMapping(value = "/upload", method = RequestMethod.POST)
	@ResponseBody
	public ModelAndView singleFileUpload(@RequestParam("file") MultipartFile file) {

		try {

			// Now you can add this to an S3 bucket
			byte[] bytes = file.getBytes();
			String name =  file.getOriginalFilename() ;

			// Put the file into the bucket
			s3Client.putObject(bytes, "scottphoto", name);

		} catch (IOException e) {
			logger.debug("Ops, somethong went wrong when trying to upload the file: " + e.getLocalizedMessage());
		}
		return new ModelAndView(new RedirectView("photo"));
	}
}
