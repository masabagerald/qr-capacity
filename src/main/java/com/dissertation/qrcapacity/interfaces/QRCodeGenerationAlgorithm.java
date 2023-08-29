package com.dissertation.qrcapacity.interfaces;

import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

public interface QRCodeGenerationAlgorithm {
    BitMatrix generateQRCode(String text, int width,int length,ErrorCorrectionLevel errorCorrectionLevel) throws WriterException;
}
