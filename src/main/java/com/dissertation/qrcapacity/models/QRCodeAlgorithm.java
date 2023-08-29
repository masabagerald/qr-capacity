package com.dissertation.qrcapacity.models;

public enum QRCodeAlgorithm {
    TRADITIONAL,
    TESSELATED;

    public static QRCodeAlgorithm fromString(String algorithmName) {
        return QRCodeAlgorithm.valueOf(algorithmName.toUpperCase());
    }
}
