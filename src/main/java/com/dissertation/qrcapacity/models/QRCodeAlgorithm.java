package com.dissertation.qrcapacity.models;

public enum QRCodeAlgorithm {
    TRADITIONAL,
    SEGMENTED_SYMBOL,
    HUFFMAN_CODING,
    TESSELATED;

    public static QRCodeAlgorithm fromString(String algorithmName) {
        return QRCodeAlgorithm.valueOf(algorithmName.toUpperCase());
    }
}
