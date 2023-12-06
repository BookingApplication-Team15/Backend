package com.booking.project.controller;

import com.booking.project.service.ImageService;
import com.booking.project.service.interfaces.IAccommodationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/images")
public class ImageController {

    @Autowired
    private ImageService imageService;

    @Autowired
    private IAccommodationService accommodationService;

    @PostMapping("/{accommodationId}")
    public ResponseEntity<?> createAd (@RequestParam("image") MultipartFile[] accImages, @PathVariable Long accommodationId) throws Exception {
        String uploadDirectory = "src/main/resources/static/images/accommodations";
        StringBuilder accImagesString = new StringBuilder();

        for (MultipartFile imageFile : accImages) {
            accImagesString.append(imageService.saveImageToStorage(uploadDirectory, imageFile)).append(",");
        }

        accommodationService.saveImages(accImagesString.toString(), accommodationId);

        return ResponseEntity.status(HttpStatus.OK).body(null);
    }

    @GetMapping("/{accommodationId}")
    public ResponseEntity<List<byte[]>> getImages(@PathVariable Long accommodationId) throws IOException {
        try {
            String imageDirectory = "src/main/resources/static/images/accommodations";

            // Retrieve image filenames associated with the specified entity (adsId) from the database
            String[] imageNames = accommodationService.getImages(accommodationId).split(",");
            List<byte[]> imageBytesList = new ArrayList<>();

            // Fetch image data as byte arrays
            for (String imageName : imageNames) {
                byte[] imageBytes = imageService.getImage(imageDirectory, imageName);
                imageBytesList.add(imageBytes);
            }

            // Respond with the image data and an OK status code
            return ResponseEntity.ok().body(imageBytesList);
        } catch (Exception e) {
            // Handle exceptions and provide appropriate error responses
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }
}