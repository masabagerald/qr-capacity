# Custom QR Code Simulator

## Overview

This project is a custom QR code simulator developed using the ZXing library. The simulator supports encoding and decoding QR codes, particularly those enhanced with Huffman coding techniques. This allows for increased data storage within QR codes, making the simulator a valuable tool for research and practical applications in data compression and QR code technology.

## Features

- **QR Code Generation**: Encode data into QR codes, including those using Huffman coding for increased data capacity.
- **QR Code Decoding**: Decode standard QR codes and those using custom Huffman coding.
- **Error Correction**: Support for error correction levels to improve the reliability of QR codes.
- **Customization**: The simulator can be extended to support different encoding schemes and custom data formats.

## Installation

### Prerequisites

- **Java**: Ensure that Java is installed on your system. The project was built using Java 8 or later.
- **Maven**: The project uses Maven for dependency management.

### Steps

1. **Clone the repository:**

    ```bash
    git clone https://github.com/masabagerald/qr-capacity.git
    cd qr-capacity
    ```

2. **Build the project:**

   Use Maven to build the project and download dependencies.

    ```bash
    mvn clean install
    ```

3. **Run the Application:**
   Deploy the `.war` file generated in the `target/` directory to your web server.

4. **Access the Web Interface:**
   - To generate a QR code, navigate to `http://localhost:8080/`.
   - To decode a QR code, navigate to `http://localhost:8080/decoder`.

## Usage

### Generating a QR Code
1. Enter the text  you want to encode.
2. Enter the dimension i.e the length and the width
3. Choose the desired error correction level.
4. Choose the algorithm e.g Conventional or Huffman
5. Click "Generate QR Code" to create the QR code.

### Decoding a QR Code
1. navigate to `http://localhost:8080/decoder`
2. Upload a QR code image.
3. Click "Decode QR Code" to retrieve the encoded data.
4. Decording details display with info about the QR Code

## Contributions

Contributions are welcome! Please feel free to submit a Pull Request.

## License

This project is licensed under the MIT License. See the [LICENSE](LICENSE) file for details.