package com.chi.ethernetprint.controller;



import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.chi.ethernetprint.dto.ReceiptRequest;
import com.chi.ethernetprint.service.ReceiptService;

import jakarta.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/api/receipt")
public class ReceiptController {

    private final ReceiptService receiptService;

    public ReceiptController(ReceiptService receiptService) {
        this.receiptService = receiptService;
    }

    @PostMapping(value = "/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> generateReceipt(
            @RequestBody ReceiptRequest request) throws Exception {

        byte[] pdf = receiptService.generateReceipt(request);

        HttpHeaders headers = new HttpHeaders();

        headers.setContentType(MediaType.APPLICATION_PDF);

        headers.setContentDisposition(
                ContentDisposition.inline()
                        .filename("receipt.pdf")
                        .build());

        return ResponseEntity.ok()
                .headers(headers)
                .body(pdf);
    }
    
    @PostMapping(value = "/print", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> thermalReceiptPrint(
            @RequestBody ReceiptRequest request , HttpServletResponse response ) throws Exception {
        
    	response.setContentType("application/pdf");
    	DateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd:hh:mm:ss");
    	String currentDateTime = dateFormatter.format(new Date());
    	String headerKey ="Content_Deposition";
    	String headerValue = "attachement; filename=pdf_"+currentDateTime+".pdf";
    	response.setHeader(headerKey, headerValue);
    	
    	
    	
    	
    	
       receiptService.printEscPos(request);
        
       byte[] pdf = receiptService.generateReceipt(request);

        HttpHeaders headers = new HttpHeaders();

        headers.setContentType(MediaType.APPLICATION_PDF);

        headers.setContentDisposition(
                ContentDisposition.inline()
                        .filename("receipt.pdf")
                        .build());

        return ResponseEntity.ok()
                .headers(headers)
                .body(pdf);
    }
    
    
    
    
}
