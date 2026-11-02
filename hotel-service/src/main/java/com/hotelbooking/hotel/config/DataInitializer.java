package com.hotelbooking.hotel.config;

import com.hotelbooking.hotel.entity.Hotel;
import com.hotelbooking.hotel.entity.ReservationStatus;
import com.hotelbooking.hotel.entity.Room;
import com.hotelbooking.hotel.entity.RoomReservation;
import com.hotelbooking.hotel.repository.HotelRepository;
import com.hotelbooking.hotel.repository.RoomRepository;
import com.hotelbooking.hotel.repository.RoomReservationRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Profile("!test")
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

  private final HotelRepository hotelRepository;
  private final RoomRepository roomRepository;
  private final RoomReservationRepository reservationRepository;

  @Override
  public void run(ApplicationArguments args) {
    Hotel moscow = hotel("Grand Hotel Moscow", "Tverskaya 1", "Moscow");
    Hotel sochi = hotel("Sea View Sochi", "Kurortny pr. 10", "Sochi");

    Room r1 = room(moscow, "101", 2, "100.00", 5, true);
    room(moscow, "102", 2, "120.00", 0, true);
    Room r3 = room(moscow, "201", 4, "200.00", 3, true);
    room(sochi, "101", 2, "90.00", 3, true);
    room(sochi, "102", 3, "110.00", 1, true);
    room(sochi, "201", 2, "95.00", 7, false);

    LocalDate today = LocalDate.now();
    reservation(r1, today.plusDays(5), today.plusDays(8), 1L);
    reservation(r3, today.plusDays(10), today.plusDays(12), 2L);

    log.info("Hotel Service demo data loaded: 2 hotels, 6 rooms, 2 active reservations");
  }

  private Hotel hotel(String name, String address, String city) {
    Hotel hotel = new Hotel();
    hotel.setName(name);
    hotel.setAddress(address);
    hotel.setCity(city);
    return hotelRepository.save(hotel);
  }

  private Room room(
      Hotel hotel, String number, int capacity, String price, int timesBooked, boolean available) {
    Room room = new Room();
    room.setHotel(hotel);
    room.setNumber(number);
    room.setCapacity(capacity);
    room.setPricePerNight(new BigDecimal(price));
    room.setTimesBooked(timesBooked);
    room.setAvailable(available);
    return roomRepository.save(room);
  }

  private void reservation(Room room, LocalDate start, LocalDate end, long bookingId) {
    RoomReservation reservation = new RoomReservation();
    reservation.setRoom(room);
    reservation.setRequestId("booking-" + bookingId);
    reservation.setBookingId(bookingId);
    reservation.setStartDate(start);
    reservation.setEndDate(end);
    reservation.setStatus(ReservationStatus.HELD);
    reservationRepository.save(reservation);
  }
}
