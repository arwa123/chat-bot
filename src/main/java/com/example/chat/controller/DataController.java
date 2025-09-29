package com.example.chat.controller;

import com.example.chat.dto.ChatModelDto;
import com.example.chat.dto.ChatModelDto.DataIngestionResponse;
import com.example.chat.dto.DocumentDto.DocumentResponse;
import com.example.chat.service.DataIngestionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/v1/data")
@RequiredArgsConstructor
public class DataController {

    private static final Logger logger = LoggerFactory.getLogger(DataController.class);

    private final DataIngestionService dataIngestionService;



    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DataIngestionResponse> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "source", required = false) String source) {

        logger.info("Processing knowledge file upload: {}", file.getOriginalFilename());
        Map<String, Object> metadata = null ;
        if(source != null) {
            metadata = new HashMap<>();
            metadata.put("source", source);
        }
        DocumentResponse docResponse = dataIngestionService.processFileAsync(
                file, metadata);

        DataIngestionResponse response;
        if (docResponse.success()) {
            response = DataIngestionResponse.success(
                    docResponse.documentId(),
                    "Document successfully processed"
            );
        } else {
            response = DataIngestionResponse.error(docResponse.message());
        }

        if (response.success()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }


    @PostMapping("/insert")
    public ResponseEntity<DataIngestionResponse> insert(@Valid @RequestBody ChatModelDto.DataIngestionRequest req) {
        logger.info("Processing data insert request");
      DataIngestionResponse response = dataIngestionService.insertData(req);
        if (response.success()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }
}
