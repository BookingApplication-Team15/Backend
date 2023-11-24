package com.booking.project.model;

import com.booking.project.model.enums.AccomodationStatus;
import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
public class PriceList {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, columnDefinition = "DATE")
    private LocalDate date;

    @Column(nullable = false)
    private double price;
    @Enumerated(EnumType.STRING)
    private AccomodationStatus status;
}
