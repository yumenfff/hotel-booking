package com.hotelbooking.hotel.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
    name = "room_reservations",
    uniqueConstraints =
        @UniqueConstraint(name = "uk_reservations_request_id", columnNames = "request_id"),
    indexes =
        @Index(name = "idx_reservations_room_dates", columnList = "room_id,start_date,end_date"))
@Getter
@Setter
@NoArgsConstructor
public class RoomReservation {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "room_id", nullable = false)
  private Room room;

  @Column(name = "request_id", nullable = false, length = 64)
  private String requestId;

  @Column(name = "booking_id")
  private Long bookingId;

  @Column(name = "start_date", nullable = false)
  private LocalDate startDate;

  @Column(name = "end_date", nullable = false)
  private LocalDate endDate;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 16)
  private ReservationStatus status;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt = Instant.now();
}
