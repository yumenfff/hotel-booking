package com.hotelbooking.hotel.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
    name = "rooms",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uk_rooms_hotel_number",
            columnNames = {"hotel_id", "number"}),
    indexes = @Index(name = "idx_rooms_times_booked", columnList = "times_booked"))
@Getter
@Setter
@NoArgsConstructor
public class Room {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "hotel_id", nullable = false)
  private Hotel hotel;

  @Column(nullable = false)
  private String number;

  @Column(nullable = false)
  private boolean available = true;

  @Column(name = "times_booked", nullable = false)
  private int timesBooked;

  @Column(nullable = false)
  private int capacity;

  @Column(name = "price_per_night", nullable = false, precision = 10, scale = 2)
  private BigDecimal pricePerNight;
}
