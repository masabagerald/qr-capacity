package com.dissertation.qrcapacity.repositories;

import com.dissertation.qrcapacity.models.Qrcode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface QRCodeRepository  extends JpaRepository<Qrcode, Long> {

}
