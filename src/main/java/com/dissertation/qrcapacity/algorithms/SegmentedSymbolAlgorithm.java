package com.dissertation.qrcapacity.algorithms;

import com.dissertation.qrcapacity.interfaces.QRCodeGenerationAlgorithm;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class SegmentedSymbolAlgorithm implements QRCodeGenerationAlgorithm {

    private int[] segments = new int[]{16, 8, 4}; // Default values for segments as an array of three elements.
    private int numSegments = 3;

    /*public SegmentedSymbolAlgorithm(int[] segments) {
        this.segments = segments;
    }*/
    @Override
    public BitMatrix generateQRCode(String text, int width, int length, ErrorCorrectionLevel errorCorrectionLevel) throws WriterException {
       // Map<EncodeHintType, Object> hints = QRCodeHelper.createHints(errorCorrectionLevel);

        Map<EncodeHintType, Object> hints = new HashMap<>();
        hints.put(EncodeHintType.ERROR_CORRECTION, errorCorrectionLevel);

        try {
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix qrCode = qrCodeWriter.encode(text, com.google.zxing.BarcodeFormat.QR_CODE, width, length, hints);

            return applySequentialModuleModulation(qrCode,  numSegments, segments);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public BitMatrix applySequentialModuleModulation(BitMatrix qrCode, int numSegments, int[] segments) {
        BitMatrix modifiedQRCode = new BitMatrix(qrCode.getWidth(), qrCode.getHeight());

        int segmentIndex = 0;

        // Loop through each module in the QR code
        for (int y = 0; y < qrCode.getHeight(); y++) {
            for (int x = 0; x < qrCode.getWidth(); x++) {
                // Check if we have used all segments
                if (segmentIndex >= numSegments) {
                    segmentIndex = 0; // Restart from the first segment
                }

                // Calculate the modulation value based on the segment
                int modulationValue = segments[segmentIndex];

                // Determine whether to set the module to black or white
                boolean isBlack = (x + y) % modulationValue == 0;


                // Set the module in the modified QR code
                // Set the module in the modified QR code
                if (isBlack) {
                    modifiedQRCode.set(x, y);
                } else {
                    // You can optionally set it to white, but the library may handle white as the default.
                    // modifiedQRCode.clear(x, y);
                }



                segmentIndex++;
            }
        }

        return modifiedQRCode;
    }


}
