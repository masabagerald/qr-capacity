package com.dissertation.qrcapacity.controllers;


import com.dissertation.qrcapacity.models.Qrcode;
import com.dissertation.qrcapacity.repositories.QRCodeRepository;
import com.dissertation.qrcapacity.services.QRCodeGenerator;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.Mode;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.google.zxing.qrcode.encoder.Encoder;
import com.google.zxing.qrcode.encoder.QRCode;
import jakarta.servlet.ServletContext;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    @PostMapping("/generate")
    public String generateQRCode(@RequestParam("text") String text,
                                 @RequestParam("width") int width,
                               //  @RequestParam("errorCorrectionLevel") Qrcode.ErrorCorrectionLevel errorCorrectionLevel,
                                 @RequestParam("errorCorrectionLevel") String errorCorrectionLevelParam,
                                 @RequestParam("length") int length,

                                 Model model) {
        try {

         Qrcode.ErrorCorrectionLevel errorCorrectionLevel = Qrcode.ErrorCorrectionLevel.valueOf(errorCorrectionLevelParam);
            ErrorCorrectionLevel zxingErrorCorrectionLevel = getErrorCorrectionLevel(errorCorrectionLevel);

           // BitMatrix improvedBitMatrix = generateImprovedQRCode(text, zxingErrorCorrectionLevel, width, length);

            Map<EncodeHintType, Object> hints = new HashMap<>();
            hints.put(EncodeHintType.ERROR_CORRECTION,  zxingErrorCorrectionLevel);

            QRCode qrCodeObject = Encoder.encode(text,zxingErrorCorrectionLevel,hints);



            int qrCodeVersion = qrCodeObject.getVersion().getVersionNumber();

            // Generate capacity tesselated patterns
            int[][] tesselatedPatterns = createTesselatedPatterns(qrCodeObject);

            // Merge capacity tesselated patterns with the QR code matrix
            BitMatrix improvedQRCodeMatrix = merge(bitMatrix, tesselatedPatterns);

            QRCodeWriter qrCodeWriter = new QRCodeWriter();


            BitMatrix bitMatrix = qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, width, length,hints);

            // Calculate the QR code version

            int storageCapacity = calculateStorageCapacity(bitMatrix);
           // int qrCodeVersion = calculateQRCodeVersion(bitMatrix);

            int dataCapacity = calculateDataCapacity(bitMatrix, errorCorrectionLevel);

            // Generate improved QR code with tesselated patterns
            BitMatrix improvedBitMatrix = generateImprovedQRCode(bitMatrix);

            // Generate a unique filename for the QR code image
            String filename = UUID.randomUUID().toString() + ".png";

            Path imagePath = Paths.get(qrCodeImageDirectory, filename);

            // Save the QR code image to the specified directory
            Files.createDirectories(imagePath.getParent());
           // MatrixToImageWriter.writeToPath(bitMatrix, "PNG", imagePath);

            MatrixToImageWriter.writeToPath(improvedBitMatrix, "PNG", imagePath);

            // Save the image file path to the database
            Qrcode qrCode = new Qrcode();
            qrCode.setText(text);
           // qrCode.setImagePath(imagePath.toString());
            qrCode.setStorageCapacity(storageCapacity);
            qrCode.setDataCapacity(dataCapacity);
            qrCode.setImagePath("/images/" + filename);
            qrCode.setErrorCorrectionLevel(errorCorrectionLevel);
            qrCode.setVersion(qrCodeVersion);
            qrCode.setVersion(qrCodeVersion);
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
            }//
        }
        return Math.round((float) bitsCount / (float) modulesCount * 100);
    }

    private com.google.zxing.qrcode.decoder.ErrorCorrectionLevel getErrorCorrectionLevel(Qrcode.ErrorCorrectionLevel level) {
        switch (level) {
            case LOW:
                return com.google.zxing.qrcode.decoder.ErrorCorrectionLevel.L;
            case MEDIUM:
                return com.google.zxing.qrcode.decoder.ErrorCorrectionLevel.M;
            case QUARTILE:
                return com.google.zxing.qrcode.decoder.ErrorCorrectionLevel.Q;
            case HIGH:
                return com.google.zxing.qrcode.decoder.ErrorCorrectionLevel.H;
            default:
                throw new IllegalArgumentException("Invalid error correction level");
        }

    }

    @GetMapping("/download/{filename:.+}")
    public ResponseEntity<Resource> downloadImage(@PathVariable String filename) {
        try {

            Path imagePath = Path.of(qrCodeImageDirectory, filename);
            Resource resource = new UrlResource(imagePath.toUri());

            if (resource.exists() && resource.isReadable()) {
                return ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                        .body(resource);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return ResponseEntity.notFound().build();
        }
    }

    private int calculateDataCapacity(BitMatrix bitMatrix, Qrcode.ErrorCorrectionLevel errorCorrectionLevel) {
        int dataCapacity = 0;
        int totalDataCodewords = getTotalDataCodewords(bitMatrix);
        int errorCorrectionCodewords = getErrorCorrectionCodewords(totalDataCodewords, errorCorrectionLevel);
        if (totalDataCodewords > 0) {
            dataCapacity = (totalDataCodewords - errorCorrectionCodewords) * 6;
        }
        return dataCapacity;
    }

    private int getTotalDataCodewords(BitMatrix bitMatrix) {
        int totalDataCodewords = 0;
        for (int y = 0; y < bitMatrix.getHeight(); y++) {
            for (int x = 0; x < bitMatrix.getWidth(); x++) {
                if (bitMatrix.get(x, y)) {
                    totalDataCodewords++;
                }
            }
        }
        return totalDataCodewords;
    }
    private int getErrorCorrectionCodewords(int totalDataCodewords, Qrcode.ErrorCorrectionLevel errorCorrectionLevel) {
        int errorCorrectionCodewords = 0;
        switch (errorCorrectionLevel) {
            case LOW:
                errorCorrectionCodewords = totalDataCodewords * 7 / 100;
                break;
            case MEDIUM:
                errorCorrectionCodewords = totalDataCodewords * 15 / 100;
                break;
            case QUARTILE:
                errorCorrectionCodewords = totalDataCodewords * 25 / 100;
                break;
            case HIGH:
                errorCorrectionCodewords = totalDataCodewords * 30 / 100;
                break;
        }
        return errorCorrectionCodewords;
    }

    private int[][] createTesselatedPatterns(BitMatrix bitMatrix) {
        int n = bitMatrix.getHeight();
        int[][] tesselatedPatterns = new int[n][n];

        // Generate tesselated patterns
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                if (i % 2 == 0 && j % 2 == 0) {
                    tesselatedPatterns[i][j] = 1;
                } else if (i % 2 == 1 && j % 2 == 1) {
                    tesselatedPatterns[i][j] = 1;
                } else {
                    tesselatedPatterns[i][j] = 0;
                }
            }
        }
        return tesselatedPatterns;
    }

    private BitMatrix merge(BitMatrix qrCodeMatrix, int[][] tesselatedPatterns) {
        int n = qrCodeMatrix.getHeight();
        BitMatrix improvedQRCodeMatrix = new BitMatrix(n);

        // Merge QR code matrix and tesselated patterns
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                if (tesselatedPatterns[i][j] == 1) {
                    improvedQRCodeMatrix.set(j, i);
                } else {
                    improvedQRCodeMatrix.set(j, i);
                }
            }
        }
        return improvedQRCodeMatrix;
    }

  /*  public static int[][] merge(int[][] qrCode, int[][] tesselatedPatterns) {
        int n = qrCode.length;
        int[][] improvedQRCode = new int[n][n];

        BitMatrix qrCodeMatrix = new BitMatrix(n, n); // Create a BitMatrix for the QR code
        BitMatrix improvedQRCodeMatrix = new BitMatrix(n, n); // Create a BitMatrix for the improved QR code

        // Convert qrCode array to qrCodeMatrix (BitMatrix)
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                qrCodeMatrix.set(j, i, qrCode[i][j] == 1); // Convert int to boolean
            }
        }

        // Convert tesselatedPatterns array to improvedQRCodeMatrix (BitMatrix)
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                if (tesselatedPatterns[i][j] == 1) {
                    improvedQRCodeMatrix.set(j, i, 1); // Set as integer 1
                } else {
                    improvedQRCodeMatrix.set(j, i, qrCodeMatrix.get(j, i) ? 1 : 0); // Convert boolean to integer
                }
            }
        }

        // Convert improvedQRCodeMatrix (BitMatrix) back to int[][] array
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                improvedQRCode[i][j] = improvedQRCodeMatrix.get(j, i) ? 1 : 0; // Convert boolean to int
            }
        }

        return improvedQRCode;
    }*/

    private BitMatrix generateImprovedQRCode(BitMatrix qrCodeMatrix) {
        int n = qrCodeMatrix.getHeight();

        // Create Capacity Tesselated Patterns
        int[][] tesselatedPatterns = createTesselatedPatterns(n);

        // Merge Capacity Tesselated Patterns with QR Code
        int[][] improvedQRCodeArray = merge(qrCodeMatrix, tesselatedPatterns);

        BitMatrix improvedBitMatrix = new BitMatrix(improvedQRCodeArray.length, improvedQRCodeArray[0].length);
        for (int y = 0; y < improvedBitMatrix.getHeight(); y++) {
            for (int x = 0; x < improvedBitMatrix.getWidth(); x++) {
                improvedBitMatrix.set(x, y, improvedQRCodeArray[y][x] == 1);
            }
        }

        return improvedBitMatrix;
    }

























}
