package com.dissertation.qrcapacity.algorithms;

import com.dissertation.qrcapacity.interfaces.QRCodeGenerationAlgorithm;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class TesselatedPatternQRCodeAlgorithm implements QRCodeGenerationAlgorithm {


    @Override
    public BitMatrix generateQRCode(String text, int width, int length, ErrorCorrectionLevel errorCorrectionLevel) throws WriterException {

        Map<EncodeHintType, Object> hints = new HashMap<>();
        hints.put(EncodeHintType.ERROR_CORRECTION, errorCorrectionLevel);

        // Implement your tesselated pattern QR code generation logic here
        int qrCodeSize = calculateQRCodeSize(width, length);
        int[][] qrCodeArray = generateTesselatedPattern(qrCodeSize);

        // Create a BitMatrix from the generated array
        BitMatrix bitMatrix = new BitMatrix(qrCodeSize, qrCodeSize);
        for (int i = 0; i < qrCodeSize; i++) {
            for (int j = 0; j < qrCodeSize; j++) {
                bitMatrix.set(j, i);
            }
        }


        return bitMatrix;
    }
    private int calculateQRCodeSize(int width, int length) {
        // Calculate QR code size based on width and length
        // Adjust as needed based on your requirements
        return Math.min(width, length);
    }
    private int[][] generateTesselatedPattern(int size) {
        int[][] tesselatedPattern = new int[size][size];
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                tesselatedPattern[i][j] = (i % 2) ^ (j % 2);
            }
        }

        return tesselatedPattern;
    }
}
