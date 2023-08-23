package com.dissertation.qrcapacity.models;

import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;


@Entity
public class Qrcode {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String text;

    private String imagePath;

    private Integer storageCapacity=0;

    private Integer dataCapacity=0;

    private ErrorCorrectionLevel errorCorrectionLevel;

    private Integer version =0;

    // Add getters and setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }

    public void setStorageCapacity(Integer storageCapacity) {

        if (storageCapacity != null) {
            // Assign the value to the primitive type property
            this.storageCapacity = storageCapacity;
        }
    }
    public int getStorageCapacity() {
        return storageCapacity;
    }

   public void setErrorCorrectionLevel(ErrorCorrectionLevel errorCorrectionLevel) {
        this.errorCorrectionLevel = errorCorrectionLevel;
    }

    public int getDataCapacity() {
        return dataCapacity;
    }

    public  void  setDataCapacity(Integer dataCapacity){

        if (dataCapacity != null) {
            // Assign the value to the primitive type property
            this.dataCapacity = dataCapacity;
        }


    }

    public  int getVersion(){return version;}

    public  void setVersion(int version){

        this.version = version;
    }


    

    public ErrorCorrectionLevel getErrorCorrectionLevel() {
        return errorCorrectionLevel;
    } 

    public enum ErrorCorrectionLevel {
        LOW, MEDIUM, QUARTILE, HIGH
    }
}
