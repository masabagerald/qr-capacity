package com.dissertation.qrcapacity.algorithms;

import com.dissertation.qrcapacity.interfaces.QRCodeGenerationAlgorithm;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class ConventionalQRCodeAlgorithm implements QRCodeGenerationAlgorithm {


    @Override
    public BitMatrix generateQRCode(String text, int width, int length, ErrorCorrectionLevel errorCorrectionLevel) throws WriterException {
        Map<EncodeHintType, Object> hints = new HashMap<>();
        hints.put(EncodeHintType.ERROR_CORRECTION, errorCorrectionLevel);

        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        return qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, width, length, hints);
    }
}
