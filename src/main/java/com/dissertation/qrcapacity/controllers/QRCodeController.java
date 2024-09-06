package com.dissertation.qrcapacity.controllers;


import com.dissertation.qrcapacity.algorithms.ConventionalQRCodeAlgorithm;
import com.dissertation.qrcapacity.algorithms.HuffmanCodingAlgorithm;
import com.dissertation.qrcapacity.algorithms.SegmentedSymbolAlgorithm;
import com.dissertation.qrcapacity.algorithms.TesselatedPatternQRCodeAlgorithm;
import com.dissertation.qrcapacity.interfaces.QRCodeGenerationAlgorithm;
import com.dissertation.qrcapacity.models.QRCodeAlgorithm;
import com.dissertation.qrcapacity.models.Qrcode;
import com.dissertation.qrcapacity.repositories.QRCodeRepository;
import com.dissertation.qrcapacity.services.HuffmanNode;
import com.dissertation.qrcapacity.services.QRCodeGenerator;
import com.google.zxing.*;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.GlobalHistogramBinarizer;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.google.zxing.qrcode.decoder.Version;
import com.google.zxing.qrcode.encoder.QRCode;
import jakarta.servlet.ServletContext;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.*;
//import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.List;
import java.util.zip.Deflater;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



@Controller
public class QRCodeController {

    private static final String QR_CODE_IMAGE_PATH = "./src/main/resources/QRCode.png";

    private static final Logger logger = LoggerFactory.getLogger(QRCodeController.class);


    @Value("${qrcode.image.directory}")
    private String qrCodeImageDirectory;
    private final QRCodeRepository qrCodeRepository;
    @Autowired
    private ServletContext servletContext;
    private final ConventionalQRCodeAlgorithm conventionalAlgorithm;
    private final TesselatedPatternQRCodeAlgorithm tesselatedAlgorithm;
    private final SegmentedSymbolAlgorithm segmentedSymbolAlgorithm;
    private final HuffmanCodingAlgorithm huffmanCodingAlgorithm;

    @Autowired
    public QRCodeController(QRCodeRepository qrCodeRepository, ConventionalQRCodeAlgorithm conventionalAlgorithm,
                            TesselatedPatternQRCodeAlgorithm tesselatedAlgorithm, SegmentedSymbolAlgorithm segmentedSymbolAlgorithm,
                            HuffmanCodingAlgorithm huffmanCodingAlgorithm
    ) {

        this.qrCodeRepository = qrCodeRepository;
        this.conventionalAlgorithm = conventionalAlgorithm;
        this.tesselatedAlgorithm = tesselatedAlgorithm;
        this.segmentedSymbolAlgorithm = segmentedSymbolAlgorithm;
        this.huffmanCodingAlgorithm = huffmanCodingAlgorithm;
    }
    static class Node {
        char character;
        int frequency;
        Node left, right;
        Node(char character, int frequency, Node left, Node right) {
            this.character = character;
            this.frequency = frequency;
            this.left = left;
            this.right = right;
        }
    }



    @GetMapping("/")
    public String home(Model model) {

        List<Qrcode> qrCodes = qrCodeRepository.findAll();
        model.addAttribute("qrCodes", qrCodes);
        return "index";
    }

    @GetMapping("/decoder")
    public String decode() {
        return "decoder";
    }

    @PostMapping("/upload")
    public String uploadFile(@RequestParam("file") MultipartFile file, Model model) {
        if (file.isEmpty()) {
            model.addAttribute("message", "Please select a valid file");
            return "decoder";
        }





        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            model.addAttribute("message", "File name is invalid");
            return "decoder";
        }

        String algorithmUsed = extractAlgorithmFromFilename(originalFilename);
        //boolean isHuffmanEncoded = originalFilename.startsWith("huffman_huff");
        boolean isHuffmanEncoded = originalFilename.contains("_huff");
        String algoritm_used;

        if (isHuffmanEncoded){
            algoritm_used = "Adaptive Huffman Coding";
        }else{
            algoritm_used = "Conventional Algorithm";
        }
        
        try {
            BufferedImage bufferedImage = ImageIO.read(file.getInputStream());
            String decodedData;

            Instant start = Instant.now();

            if (isHuffmanEncoded) {
                decodedData = decodeHuffmanEncodedQRCode(bufferedImage, model);
            } else {
                decodedData = decodeStandardQRCode(bufferedImage);
            }
            logger.info("Decoded data: " + decodedData);


            Instant end = Instant.now();
            long timeElapsed = Duration.between(start, end).toMillis();
           // model.addAttribute("scanningTime", timeElapsed > 0 ? timeElapsed + " ms" : "1 ms"); // Adding scanning time to the model
            model.addAttribute("scanningTime", timeElapsed + " ms");
            model.addAttribute("algorithmUsed", algoritm_used);
            populateModelWithFileDetails(model, file, decodedData, bufferedImage);
        } catch (IOException e) {
            model.addAttribute("message", "Error reading the file: " + e.getMessage());
        } catch (NotFoundException e) {
            model.addAttribute("message", "Could not decode QR Code: " + e.getMessage());
        } catch (Exception e) {
            model.addAttribute("message", "An unexpected error occurred: " + e.getMessage());
        }

        return "decoding_details";
    }

    private String extractAlgorithmFromFilename(String filename) {
        int underscoreIndex = filename.indexOf('_');
        if (underscoreIndex != -1) {
            return filename.substring(0, underscoreIndex);
        } else {
            return "unknown";
        }
    }

  private String decodeHuffmanEncodedQRCode(BufferedImage bufferedImage, Model model) throws NotFoundException {
      LuminanceSource source = new BufferedImageLuminanceSource(bufferedImage);
      BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
      Result result = new MultiFormatReader().decode(bitmap);

      String encodedDataWithCodes = result.getText();
      String[] parts = encodedDataWithCodes.split("#", 2);
      Map<Character, String> huffmanCodes = new HashMap<>();

      for (String part : parts[1].split(",")) {
          String[] codeParts = part.split(":");
          char character = codeParts[0].charAt(0);
          String code = codeParts[1];
          huffmanCodes.put(character, code);
      }

      String huffmanEncodedData = parts[0];
      String decodedData = huffmanCodingAlgorithm.decodeHuffmanData(huffmanEncodedData, huffmanCodingAlgorithm.reconstructHuffmanTree(huffmanCodes));

      Object errorCorrectionLevel = result.getResultMetadata().get(ResultMetadataType.ERROR_CORRECTION_LEVEL);
      model.addAttribute("errorCorrectionLevel", errorCorrectionLevel != null ? errorCorrectionLevel.toString() : "Unknown");



      Integer qrCodeVersion = (Integer) result.getResultMetadata().get(ResultMetadataType.PDF417_EXTRA_METADATA);
      model.addAttribute("qrCodeVersion", qrCodeVersion != null ? qrCodeVersion.toString() : "Unknown");

      return decodedData;
  }

    private String decodeStandardQRCode(BufferedImage bufferedImage) throws NotFoundException {
        LuminanceSource source = new BufferedImageLuminanceSource(bufferedImage);
        BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
        Result result = new MultiFormatReader().decode(bitmap);
        return result.getText();
    }


    private BufferedImage resizeImage(BufferedImage originalImage, int targetWidth) {
        int targetHeight = (int) (originalImage.getHeight() * (targetWidth / (double) originalImage.getWidth()));
        BufferedImage resizedImage = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = resizedImage.createGraphics();
        g.drawImage(originalImage, 0, 0, targetWidth, targetHeight, null);
        g.dispose();
        return resizedImage;
    }

    private BufferedImage preprocessImage(BufferedImage originalImage) {
        // Convert the image to grayscale
        BufferedImage grayscaleImage = new BufferedImage(originalImage.getWidth(), originalImage.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
        Graphics g = grayscaleImage.getGraphics();
        g.drawImage(originalImage, 0, 0, null);
        g.dispose();

        // Apply additional preprocessing steps if needed (e.g., contrast enhancement)
        // ...

        return grayscaleImage;
    }


    private void populateModelWithFileDetails(Model model, MultipartFile file, String decodedData, BufferedImage bufferedImage) {

        logger.info("Inside populate details function: ");
        model.addAttribute("result", decodedData);
        model.addAttribute("fileName", file.getOriginalFilename());
        model.addAttribute("fileSize", file.getSize());
        model.addAttribute("fileType", file.getContentType());

        System.out.println("result : " + decodedData);
        System.out.println("filesize : " + file.getSize());


        int width = bufferedImage.getWidth();
        int height = bufferedImage.getHeight();
        model.addAttribute("resolution", width + "x" + height);
    }


    @GetMapping(value = "/genrateAndDownloadQRCode/{codeText}/{width}/{height}")
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
                                 @RequestParam("algorithm") QRCodeAlgorithm algorithmName,

                                 Model model) {


        try {

            // Choose the compression level for Deflate (adjust as needed)
            Deflater deflater = new Deflater(Deflater.DEFAULT_COMPRESSION, true);
            byte[] inputBytes = text.getBytes();
            byte[] compressedBytes = new byte[inputBytes.length];

            deflater.setInput(inputBytes);
            deflater.finish();
            int compressedLength = deflater.deflate(compressedBytes);

            // Convert the compressed bytes to a Base64-encoded string
            String compressedData = Base64.getEncoder().encodeToString(compressedBytes);

            QRCodeGenerationAlgorithm algorithm;
            String algorithm_name;

            switch (algorithmName) {
                case TRADITIONAL:
                    algorithm = conventionalAlgorithm;
                    algorithm_name = "Conventional";
                    break;
                case TESSELATED:
                    algorithm = tesselatedAlgorithm;
                    algorithm_name = "Tesselated";
                    break;
                case SEGMENTED_SYMBOL:
                    algorithm = segmentedSymbolAlgorithm;
                    algorithm_name = "Segmented Symbol";
                    break;
                case HUFFMAN_CODING:
                    algorithm = huffmanCodingAlgorithm;
                    algorithm_name = "Huffman Coding";
                    break;

                default:
                    model.addAttribute("error", "Invalid QR code generation algorithm.");
                    return "redirect:/";
            }

            Qrcode.ErrorCorrectionLevel errorCorrectionLevel = Qrcode.ErrorCorrectionLevel.valueOf(errorCorrectionLevelParam);
            ErrorCorrectionLevel zxingErrorCorrectionLevel = getErrorCorrectionLevel(errorCorrectionLevel);


            BitMatrix bitMatrix = algorithm.generateQRCode(text, width, length, zxingErrorCorrectionLevel);

            int size = bitMatrix.getWidth(); // or getHeight(), they are the same
            System.out.println("Generated QR code width : " + size);
            System.out.println("Generated QR code height is: " + bitMatrix.getHeight());
            System.out.println("Algorithm  is: " + algorithm);

            int version = (size - 17) / 4;

            // Calculate the properties
            int storageCapacity = calculateStorageCapacity(bitMatrix);
            int dataCapacity = calculateDataCapacity(bitMatrix, errorCorrectionLevel);


            String currentTimestamp = String.valueOf(System.currentTimeMillis());
            String algorithmNameForFile = algorithmName.toString().toLowerCase().replace(' ', '_');

            // Save the QR code image to a directory
            String filename = algorithmNameForFile + "_" + currentTimestamp + ".png";
            Path imagePath = Paths.get(qrCodeImageDirectory, filename);
            Files.createDirectories(imagePath.getParent());
            // ImageIO.write(qrCodeImage, "PNG", imagePath.toFile());
            MatrixToImageWriter.writeToPath(bitMatrix, "PNG", imagePath);

            // Create a new QRCode entity and save it to the repository (you may need to adjust the code according to your entity structure)
            Qrcode qrCode = new Qrcode();
            qrCode.setText(text);
            qrCode.setStorageCapacity(storageCapacity);
            qrCode.setDataCapacity(dataCapacity);
            qrCode.setVersion(version);
            qrCode.setErrorCorrectionLevel(errorCorrectionLevel);
            qrCode.setImagePath("/images/" + filename);
            qrCode.setAlgorithm(algorithm_name);
            qrCodeRepository.save(qrCode);

            model.addAttribute("success", "QR code generated successfully and saved!");

        } catch (WriterException e) {
            e.printStackTrace();

        } catch (IOException e) {
            throw new RuntimeException(e);
        }


        return "redirect:/";
    }

    private int calculateQRCodeVersion(BitMatrix bitMatrix) {

        return 0;

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





































}
