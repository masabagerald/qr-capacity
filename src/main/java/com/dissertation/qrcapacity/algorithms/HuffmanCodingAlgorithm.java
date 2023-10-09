package com.dissertation.qrcapacity.algorithms;

import com.dissertation.qrcapacity.interfaces.QRCodeGenerationAlgorithm;
import com.dissertation.qrcapacity.services.HuffmanNode;
import com.google.zxing.*;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class HuffmanCodingAlgorithm implements QRCodeGenerationAlgorithm {

    /*public class HuffmanNode {
        public char character;
        int frequency;
        public HuffmanNode left;
        public HuffmanNode right;
    }*/

    @Override
    public BitMatrix generateQRCode(String text, int width, int height, ErrorCorrectionLevel errorCorrectionLevel) throws WriterException {
        Map<EncodeHintType, Object> hints = new HashMap<>();
        hints.put(EncodeHintType.ERROR_CORRECTION, errorCorrectionLevel);
        // Calculate the frequency of each character in the input text
        Map<Character, Integer> frequencyMap = calculateFrequency(text);

        // Build the Huffman Tree based on character frequencies
        HuffmanNode root = buildTree(frequencyMap);

        // Generate Huffman Codes for each character
        Map<Character, String> huffmanCodes = generateCodes(root);

        // Encode the input text using the generated Huffman codes
        StringBuilder encodedData = new StringBuilder();
        for (char character : text.toCharArray()) {
            encodedData.append(huffmanCodes.get(character));
        }

        // Generate the QR Code with the Huffman encoded data
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        try {

            return qrCodeWriter.encode(encodedData.toString(), BarcodeFormat.QR_CODE, width, height,hints);
        } catch (WriterException e) {
            throw new WriterException("Could not generate QR code: " + e.getMessage());
        }
    }

    private Map<Character, Integer> calculateFrequency(String data) {
        Map<Character, Integer> frequencyMap = new HashMap<>();
        for (char character : data.toCharArray()) {
            frequencyMap.put(character, frequencyMap.getOrDefault(character, 0) + 1);
        }
        return frequencyMap;
    }

    private HuffmanNode buildTree(Map<Character, Integer> frequencyMap) {
        PriorityQueue<HuffmanNode> priorityQueue = new PriorityQueue<>(Comparator.comparingInt(o -> o.frequency));
        for (Map.Entry<Character, Integer> entry : frequencyMap.entrySet()) {
            HuffmanNode node = new HuffmanNode();
            node.character = entry.getKey();
            node.frequency = entry.getValue();
            priorityQueue.add(node);
        }

        while (priorityQueue.size() > 1) {
            HuffmanNode x = priorityQueue.poll();
            HuffmanNode y = priorityQueue.poll();
            HuffmanNode sum = new HuffmanNode();
            sum.frequency = x.frequency + y.frequency;
            sum.left = x;
            sum.right = y;
            priorityQueue.add(sum);
        }

        return priorityQueue.poll();
    }

    private Map<Character, String> generateCodes(HuffmanNode root) {
        Map<Character, String> huffmanCodes = new HashMap<>();
        generateCodesRecursive(root, "", huffmanCodes);
        return huffmanCodes;
    }

    private void generateCodesRecursive(HuffmanNode node, String code, Map<Character, String> huffmanCodes) {
        if (node == null) return;
        if (node.left == null && node.right == null) {
            huffmanCodes.put(node.character, code);
            return;
        }
        generateCodesRecursive(node.left, code + '0', huffmanCodes);
        generateCodesRecursive(node.right, code + '1', huffmanCodes);
    }

    // In HuffmanCodingAlgorithm

    private Map<Character, String> huffmanCodes = new HashMap<>();

    public Map<Character, String> getHuffmanCodes() {
     /*   if (this.huffmanCodes == null) {
            throw new IllegalStateException("Huffman codes have not been generated yet.");
        }*/
        return this.huffmanCodes;
    }


    public HuffmanNode reconstructHuffmanTree(Map<Character, String> huffmanCodes) {
        HuffmanNode root = new HuffmanNode();
        for (Map.Entry<Character, String> entry : huffmanCodes.entrySet()) {
            HuffmanNode currentNode = root;
            String code = entry.getValue();

            for (int i = 0; i < code.length(); i++) {
                if (code.charAt(i) == '0') {
                    if (currentNode.left == null) {
                        currentNode.left = new HuffmanNode();
                    }
                    currentNode = currentNode.left;
                } else if (code.charAt(i) == '1') {
                    if (currentNode.right == null) {
                        currentNode.right = new HuffmanNode();
                    }
                    currentNode = currentNode.right;
                }
            }
            currentNode.character = entry.getKey();
        }
        return root;
    }

    public String decodeHuffmanData(String huffmanEncodedData, HuffmanNode root) {
        StringBuilder decodedData = new StringBuilder();
        HuffmanNode currentNode = root;

        for (int i = 0; i < huffmanEncodedData.length(); i++) {
            if (huffmanEncodedData.charAt(i) == '0') {
                currentNode = currentNode.left;
            } else if (huffmanEncodedData.charAt(i) == '1') {
                currentNode = currentNode.right;
            }

            if (currentNode.left == null && currentNode.right == null) {
                decodedData.append(currentNode.character);
                currentNode = root;
            }
        }

        return decodedData.toString();
    }




}
