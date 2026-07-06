package com.chi.ethernetprint.service;




import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import com.chi.ethernetprint.dto.ReceiptRequest;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ReceiptService {
	
	 private static final String PRINTER_IP = "192.168.192.168";
	    private static final int PORT = 9100;
	    private static final int MAX_WIDTH = 384; // 80mm printer (use 384 for 58mm)

    public byte[] generateReceipt(ReceiptRequest request) throws Exception {

        InputStream inputStream =
                new ClassPathResource("/receipt1.jrxml").getInputStream();

        JasperReport jasperReport =
                JasperCompileManager.compileReport(inputStream);

        Map<String, Object> params = new HashMap<>();

        params.put("receiptNo", request.getReceiptNo());
        params.put("billNo", request.getBillNo());
        params.put("citizenName", request.getCitizenName());
        params.put("serviceType", request.getServiceType());
        params.put("amount", request.getAmount());
        params.put("paymentMode", request.getPaymentMode());
        params.put("transactionId", request.getTransactionId());
        params.put("receiptDate", request.getReceiptDate());

        JasperPrint jasperPrint = JasperFillManager.fillReport(
                jasperReport,
                params,
                new JREmptyDataSource()
        );

        return JasperExportManager.exportReportToPdf(jasperPrint);
    }
    
    public JasperPrint buildReport(ReceiptRequest receiptRequest) throws Exception {
    	
    	
    	  Map<String, Object> params = new HashMap<>();

          params.put("receiptNo", receiptRequest.getReceiptNo());
          params.put("billNo", receiptRequest.getBillNo());
          params.put("citizenName", receiptRequest.getCitizenName());
          params.put("serviceType", receiptRequest.getServiceType());
          params.put("amount", receiptRequest.getAmount());
          params.put("paymentMode", receiptRequest.getPaymentMode());
          params.put("transactionId", receiptRequest.getTransactionId());
          params.put("receiptDate", receiptRequest.getReceiptDate());

         
          
    	//JRBeanCollectionDataSource ds = new JRBeanCollectionDataSource(items);
		
    	
		/*
		 * List<Map<String, Object>> items = new ArrayList<>(); items.add(Map.of("item",
		 * "Burger", "price", "6.00")); items.add(Map.of("item", "Cola", "price",
		 * "2.00"));
		 */ 
		/*
		 * JRBeanCollectionDataSource ds = new JRBeanCollectionDataSource(items);
		 */ 
         

    	InputStream jrxml =  getClass().getResourceAsStream("/receipt1.jrxml"); JasperReport report =
		  JasperCompileManager.compileReport(jrxml); 
    	return  JasperFillManager.fillReport(report,params,  new JREmptyDataSource());
		 
    }
    
    
    public void printEscPos(ReceiptRequest receiptRequest) throws Exception {
        JasperPrint print = buildReport(receiptRequest);
        //convert pdf to byte code
        byte[] pdf = JasperExportManager.exportReportToPdf(print);
        
        
        // 2. PDF → Image
        BufferedImage img = pdfToImage(pdf);
        
        printImage(PRINTER_IP,img);
        
        
        /*
        
        // Convert report data into simple ESC/POS text
        StringBuilder sb = new StringBuilder();
       // sb.append("==== RECEIPT ====\n\n");
        for (JRPrintPage page : print.getPages()) {
        	for (JRPrintElement element : page.getElements()) {
                System.out.println(element.getClass());
            }
        	sb.append(page.toString());
           // sb.append("Printed via Spring Boot\n");
        }
        //sb.append("\nTHANK YOU\n\n");
        byte[] data = sb.toString().getBytes();
        try (Socket socket = new Socket(PRINTER_IP, PORT);
             OutputStream os = socket.getOutputStream()) {
            os.write(data);
            os.flush();
        }
        */
    }
    
public byte[] generatePdf(JasperPrint print) throws Exception {
    	
        return JasperExportManager.exportReportToPdf(print);
       
    }

public void printImage(String ip, BufferedImage image) {
    try (Socket socket = new Socket(ip, 9100);
         OutputStream os = socket.getOutputStream()) {

        BufferedImage resized = resize(image, MAX_WIDTH);
        BufferedImage mono = toBlackAndWhite(resized);

        byte[] raster = convertToESCPosRaster(mono);

        os.write(initPrinter());
        os.write(raster);
        os.write(new byte[]{0x1D, 0x56, 0x00}); // cut paper
        os.flush();

    } catch (Exception e) {
        throw new RuntimeException("ESC/POS print failed", e);
    }
}
    
    private byte[] initPrinter() {
        return new byte[]{
                0x1B, 0x40 // ESC @ (initialize)
        };
    }
    
    private BufferedImage resize(BufferedImage original, int width) {
        int height = (int) ((double) original.getHeight() / original.getWidth() * width);

        BufferedImage resized = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        Graphics2D g = resized.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BILINEAR);

        g.drawImage(original, 0, 0, width, height, null);
        g.dispose();

        return resized;
    }
    
    private BufferedImage toBlackAndWhite(BufferedImage img) {
        BufferedImage bw = new BufferedImage(
                img.getWidth(),
                img.getHeight(),
                BufferedImage.TYPE_BYTE_BINARY
        );

        Graphics2D g = bw.createGraphics();
        g.drawImage(img, 0, 0, null);
        g.dispose();

        return bw;
    }
    private byte[] convertToESCPosRaster(BufferedImage image) {

        int width = image.getWidth();
        int height = image.getHeight();

        int widthBytes = (width + 7) / 8;
        byte[] data = new byte[8 + widthBytes * height];

        int i = 0;

        // GS v 0
        data[i++] = 0x1D;
        data[i++] = 0x76;
        data[i++] = 0x30;
        data[i++] = 0x00;

        // width in bytes (low byte, high byte)
        data[i++] = (byte) (widthBytes % 256);
        data[i++] = (byte) (widthBytes / 256);

        // height (low byte, high byte)
        data[i++] = (byte) (height % 256);
        data[i++] = (byte) (height / 256);

        // image data
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < widthBytes * 8; x += 8) {

                byte b = 0;

                for (int bit = 0; bit < 8; bit++) {
                    int px = x + bit;

                    if (px < width) {
                        int rgb = image.getRGB(px, y);
                        int gray = (rgb >> 16) & 0xff;

                        // threshold (adjust if needed)
                        if (gray < 128) {
                            b |= (byte) (1 << (7 - bit));
                        }
                    }
                }

                data[i++] = b;
            }
        }

        return data;
    }
    public BufferedImage pdfToImage(byte[] pdfBytes) {
        try (PDDocument document = PDDocument.load(new ByteArrayInputStream(pdfBytes))) {

            PDFRenderer renderer = new PDFRenderer(document);

            // Jasper usually creates single-page receipts
            BufferedImage image = renderer.renderImageWithDPI(
                    0,   // page index
                    203  // DPI (203 is good for thermal printers)
            );

            return image;

        } catch (Exception e) {
            throw new RuntimeException("Failed to convert PDF to image", e);
        }
    }

    
}
