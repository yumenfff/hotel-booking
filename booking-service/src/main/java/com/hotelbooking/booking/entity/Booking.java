package com.hotelbooking.booking.entity;

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
    name = "bookings",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uk_bookings_user_request",
            columnNames = {"user_id", "request_id"}),
    indexes = {
      @Index(name = "idx_bookings_user_id", columnList = "user_id"),
      @Index(name = "idx_bookings_status", columnList = "status"),
      @Index(name = "idx_bookings_room_id", columnList = "room_id")
    })
@Getter
@Setter
@NoArgsConstructor
public class Booking {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Column(name = "room_id")
  private Long roomId;

  @Column(name = "start_date", nullable = false)
  private LocalDate startDate;

  @Column(name = "end_date", nullable = false)
  private LocalDate endDate;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 16)
  private BookingStatus status;

  @Column(name = "request_id", nullable = false, length = 100)
  private String requestId;

  @Column(name = "failure_reason", length = 500)
  private String failureReason;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt = Instant.now();

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt = Instant.now();
}
