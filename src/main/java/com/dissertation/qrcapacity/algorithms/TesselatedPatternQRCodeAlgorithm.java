package com.dissertation.qrcapacity.algorithms;

import com.dissertation.qrcapacity.interfaces.QRCodeGenerationAlgorithm;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.Writer;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;


@Service
public class TesselatedPatternQRCodeAlgorithm implements QRCodeGenerationAlgorithm {

    String imagePath = "D:\\projects\\java\\qr-capacity\\src\\main\\resources\\static\\images\\beehive.png";
    private static final String QR_CODE_IMAGE_PATH = "./src/main/resources/QRCode.png";
    @Override
    public BitMatrix generateQRCode(String text, int width, int length, ErrorCorrectionLevel errorCorrectionLevel) throws WriterException, WriterException {
        Map<EncodeHintType, Object> hints = new HashMap<>();
        hints.put(EncodeHintType.ERROR_CORRECTION, errorCorrectionLevel);

        // Generate the QR code BitMatrix
        Writer writer = new QRCodeWriter();
        BitMatrix qrCodeBitMatrix = writer.encode(text, BarcodeFormat.QR_CODE, width, length, hints);

        // Load the tessellated pattern image
        BufferedImage patternImage = loadTessellatedPatternImage();

        // Create a combined image
        BufferedImage combinedImage = createCombinedImage(qrCodeBitMatrix, patternImage);

        // Save the combined image to a file (or return it as needed)
        try {

            ImageIO.write(combinedImage, "PNG", new File(QR_CODE_IMAGE_PATH));
        } catch (IOException e) {
            e.printStackTrace();
        }

        return qrCodeBitMatrix;
    }

    private BufferedImage loadTessellatedPatternImage() {
        BufferedImage patternImage = null;
        try {
            patternImage = ImageIO.read(new File(imagePath)); // Replace with your tessellated pattern image file
        } catch (IOException e) {
            e.printStackTrace();
        }
        return patternImage;
    }

    private BufferedImage createCombinedImage(BitMatrix qrCodeBitMatrix, BufferedImage patternImage) {
        int qrCodeWidth = qrCodeBitMatrix.getWidth();
        int qrCodeHeight = qrCodeBitMatrix.getHeight();

        BufferedImage combinedImage = new BufferedImage(qrCodeWidth, qrCodeHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = combinedImage.createGraphics();

        // Draw the QR code matrix onto the combined image
        for (int x = 0; x < qrCodeWidth; x++) {
            for (int y = 0; y < qrCodeHeight; y++) {
                int color = qrCodeBitMatrix.get(x, y) ? Color.BLACK.getRGB() : Color.WHITE.getRGB();
                combinedImage.setRGB(x, y, color);

            }
        }

        // Draw the tessellated pattern image around the QR code
        graphics.drawImage(patternImage, 0, 0, null);

        graphics.dispose();
        return combinedImage;
    }
}
