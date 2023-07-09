package com.dissertation.qrcapacity.controllers;


import com.dissertation.qrcapacity.models.Qrcode;
import com.dissertation.qrcapacity.repositories.QRCodeRepository;
import com.dissertation.qrcapacity.services.QRCodeGenerator;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;


import jakarta.servlet.ServletContext;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;


@Controller
public class QRCodeController {

    private static final String QR_CODE_IMAGE_PATH = "./src/main/resources/QRCode.png";

    @Value("${qrcode.image.directory}")
    private String qrCodeImageDirectory;

    private final QRCodeRepository qrCodeRepository;

    @Autowired
    private ServletContext servletContext;

    public QRCodeController(QRCodeRepository qrCodeRepository) {
        this.qrCodeRepository = qrCodeRepository;
    }

    @GetMapping("/")
    public String home(Model model) {

        List<Qrcode> qrCodes = qrCodeRepository.findAll();

        model.addAttribute("qrCodes", qrCodes);


        return "index";
    }

    @GetMapping(value ="/genrateAndDownloadQRCode/{codeText}/{width}/{height}")
    public void download(
            @PathVariable("codeText") String codeText,
            @PathVariable("width") Integer width,
            @PathVariable("height") Integer height)
            throws Exception {
        QRCodeGenerator.generateQRCodeImage(codeText, width, height, QR_CODE_IMAGE_PATH);
    }

    @GetMapping("/generate-qr")
    public String generateQRCode(@RequestParam("text") String text,
                                 @RequestParam("image") MultipartFile imageFile,
                                 Model model) {
        try {
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, 200, 200);



            // Generate a unique filename for the QR code image
            String filename = UUID.randomUUID().toString() + ".png";
            Path imagePath = Path.of(qrCodeImageDirectory, filename);

            // Save the QR code image to the specified directory
            Files.createDirectories(imagePath.getParent());
            MatrixToImageWriter.writeToPath(bitMatrix, "PNG", imagePath);

            // Save the image file path to the database
            Qrcode qrCode = new Qrcode();
            qrCode.setText(text);
            qrCode.setImagePath(imagePath.toString());
            qrCodeRepository.save(qrCode);

            model.addAttribute("success", "QR code generated successfully!");
            return "redirect:/";
        } catch (WriterException | IOException e) {
            model.addAttribute("error", "Failed to generate QR code: " + e.getMessage());
            return "redirect:/";
        }
    }

    @PostMapping("/generate")
    public String generateQRCode(@RequestParam("text") String text,
                                 @RequestParam("width") int width,
                                 @RequestParam("length") int length,
                                 Model model) {
        try {
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, width, length);

            int storageCapacity = calculateStorageCapacity(bitMatrix);

            // Generate a unique filename for the QR code image
            String filename = UUID.randomUUID().toString() + ".png";
            //Path imagePath = Path.of(qrCodeImageDirectory, filename);
            Path imagePath = Path.of(servletContext.getRealPath("/images"), filename);

            // Save the QR code image to the specified directory
            Files.createDirectories(imagePath.getParent());
            MatrixToImageWriter.writeToPath(bitMatrix, "PNG", imagePath);

            // Save the image file path to the database
            Qrcode qrCode = new Qrcode();
            qrCode.setText(text);
           // qrCode.setImagePath(imagePath.toString());
            qrCode.setStorageCapacity(storageCapacity);
            qrCode.setImagePath("/images/" + filename);
            qrCodeRepository.save(qrCode);

            model.addAttribute("success", "QR code generated successfully!");
            return "redirect:/";
        } catch (WriterException | IOException e) {
            model.addAttribute("error", "Failed to generate QR code: " + e.getMessage());
            return "redirect:/";
        }

        
    }

    @GetMapping("/images/{filename:.+}")
        public ResponseEntity<Resource> serveImage(@PathVariable String filename) {
            try {
                Path imagePath = Path.of(servletContext.getRealPath("/images"), filename);
                Resource resource = new UrlResource(imagePath.toUri());

                if (resource.exists() && resource.isReadable()) {
                    return ResponseEntity.ok().body(resource);
                }
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }

            return ResponseEntity.notFound().build();
        }


    private int calculateStorageCapacity(BitMatrix bitMatrix) {
        int modulesCount = bitMatrix.getWidth() * bitMatrix.getHeight();
        int bitsCount = 0;
        for (int y = 0; y < bitMatrix.getHeight(); y++) {
            for (int x = 0; x < bitMatrix.getWidth(); x++) {
                if (bitMatrix.get(x, y)) {
                    bitsCount++;
                }
            }
        }
        return Math.round((float) bitsCount / (float) modulesCount * 100);
    }



}
