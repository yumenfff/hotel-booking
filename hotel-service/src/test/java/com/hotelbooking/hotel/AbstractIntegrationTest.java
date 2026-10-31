package com.hotelbooking.hotel;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

import com.hotelbooking.hotel.entity.Hotel;
import com.hotelbooking.hotel.entity.ReservationStatus;
import com.hotelbooking.hotel.entity.Room;
import com.hotelbooking.hotel.entity.RoomReservation;
import com.hotelbooking.hotel.repository.HotelRepository;
import com.hotelbooking.hotel.repository.RoomRepository;
import com.hotelbooking.hotel.repository.RoomReservationRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class AbstractIntegrationTest {

  @Autowired protected MockMvc mockMvc;
  @Autowired protected HotelRepository hotelRepository;
  @Autowired protected RoomRepository roomRepository;
  @Autowired protected RoomReservationRepository reservationRepository;

  @BeforeEach
  protected void cleanDatabase() {
    reservationRepository.deleteAll();
    roomRepository.deleteAll();
    hotelRepository.deleteAll();
  }

  protected static RequestPostProcessor admin() {
    return jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"));
  }

  protected static RequestPostProcessor user() {
    return jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"));
  }

  protected Hotel createHotel(String name) {
    Hotel hotel = new Hotel();
    hotel.setName(name);
    hotel.setAddress("Test street 1");
    hotel.setCity("Moscow");
    return hotelRepository.save(hotel);
  }

  protected Room createRoom(Hotel hotel, String number, int timesBooked) {
    return createRoom(hotel, number, timesBooked, true);
  }

  protected Room createRoom(Hotel hotel, String number, int timesBooked, boolean available) {
    Room room = new Room();
    room.setHotel(hotel);
    room.setNumber(number);
    room.setCapacity(2);
    room.setPricePerNight(new BigDecimal("100.00"));
    room.setTimesBooked(timesBooked);
    room.setAvailable(available);
    return roomRepository.save(room);
  }

  protected RoomReservation holdRoom(Room room, LocalDate start, LocalDate end, String requestId) {
    RoomReservation reservation = new RoomReservation();
    reservation.setRoom(room);
    reservation.setRequestId(requestId);
    reservation.setStartDate(start);
    reservation.setEndDate(end);
    reservation.setStatus(ReservationStatus.HELD);
    return reservationRepository.save(reservation);
  }

  protected int timesBooked(Room room) {
    return roomRepository.findById(room.getId()).orElseThrow().getTimesBooked();
  }
}
