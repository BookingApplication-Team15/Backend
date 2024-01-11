package com.booking.project.service;

import com.booking.project.dto.CreateReservationDTO;
import com.booking.project.dto.ReservationDTO;
import com.booking.project.model.Accommodation;
import com.booking.project.model.Guest;
import com.booking.project.model.Reservation;
import com.booking.project.model.enums.AccommodationStatus;
import com.booking.project.model.enums.CancellationPolicy;
import com.booking.project.model.enums.ReservationMethod;
import com.booking.project.model.enums.ReservationStatus;
import com.booking.project.model.enums.AccommodationStatus;
import com.booking.project.repository.inteface.IGuestRepository;
import com.booking.project.repository.inteface.IReservationRepository;
import com.booking.project.service.interfaces.IAccommodationService;
import com.booking.project.service.interfaces.IGuestService;
import com.booking.project.service.interfaces.IReservationService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cglib.core.Local;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;

@Service
public class ReservationService implements IReservationService {

    @Autowired
    private IReservationRepository reservationRepository;

    @Autowired
    private IAccommodationService accommodationService;

    @Autowired
    private IGuestService guestService;

    @Autowired
    EntityManager em;
    @Override
    public Collection<Reservation> findAll() {
        return reservationRepository.findAll();
    }

    @Override
    public Optional<Reservation> findById(Long id) {
        return reservationRepository.findById(id);
    }

    @Override
    public Reservation save(Reservation reservation) throws Exception {
        return reservationRepository.save(reservation);
    }
    @Override
    public Reservation create(CreateReservationDTO createReservationDTO, double price, ReservationMethod reservationMethod) throws Exception {
        Reservation reservation = new Reservation();

        Optional<Accommodation> accommodation = accommodationService.findById(createReservationDTO.getAccommodationId());
        if (accommodation.isEmpty()) return null;
        reservation.setAccommodation(accommodation.get());

        Optional<Guest> guest = guestService.findByUser(createReservationDTO.getGuestId());
        if (guest.isEmpty()) return null;
        reservation.setGuest(guest.get());

        reservation.setPrice(price);
        if(reservationMethod == ReservationMethod.AUTOMATIC){
            reservation.setStatus(ReservationStatus.ACCEPTED);
        }else{
            reservation.setStatus(ReservationStatus.PENDING);
        }
        reservation.setStartDate(createReservationDTO.getStartDate());
        reservation.setEndDate(createReservationDTO.getEndDate());
        reservation.setNumberOfGuests(createReservationDTO.getNumberOfGuests());
        reservation.setHostReported(false);

        save(reservation);
        return  reservation;
    }
    @Override
    public Boolean deleteById(Long id) {
        Optional<Reservation> reservation = reservationRepository.findById(id);

        if(reservation.isEmpty()){
            return false;
        }
        if (reservation.get().getStatus().equals(ReservationStatus.PENDING)){
            reservationRepository.deleteById(id);
            return true;
        }
        return false;
    }
    @Override
    public Collection<Reservation> findByGuestId(Long id){
        return reservationRepository.findByGuest(id);
    }
    @Override
    public Collection<Reservation> findByHostId(Long id){
        return reservationRepository.findByHost(id);
    }
    @Override
    public List<ReservationDTO> filterGuestReservations(String title, LocalDate startDate, LocalDate endDate, ReservationStatus reservationStatus, Integer guestUserId){
        Query q = em.createQuery("SELECT r FROM Reservation r JOIN FETCH r.accommodation a JOIN FETCH r.guest WHERE (LOWER(a.title) LIKE LOWER(:pattern) OR :pattern is Null)" +
                " AND ((r.startDate >= :startDate AND r.endDate <= :endDate) OR cast(:startDate as date) is null) " +
                "AND (r.status = :reservationStatus OR :reservationStatus is Null) AND (:guestUserId is Null OR r.guest.user.id = :guestUserId)");
        if(title == null){
            q.setParameter("pattern", null);
        }else{
            q.setParameter("pattern", "%" + title + "%");
        }
        q.setParameter("startDate" , startDate);
        q.setParameter("endDate", endDate);
        q.setParameter("reservationStatus", reservationStatus);
        q.setParameter("guestUserId", guestUserId);

        List<ReservationDTO> reservationDTOs = new ArrayList<ReservationDTO>();
        List<Reservation> reservations = q.getResultList();
        for(Reservation reservation : reservations){
            reservationDTOs.add(new ReservationDTO(reservation));
        }
        return reservationDTOs;
    }

    @Override
    public List<ReservationDTO> filterHostReservations(String title, LocalDate startDate, LocalDate endDate, ReservationStatus reservationStatus, Integer hostUserId){
        Query q = em.createQuery("SELECT r FROM Reservation r JOIN FETCH r.accommodation a JOIN FETCH r.guest WHERE (LOWER(a.title) LIKE LOWER(:pattern) OR :pattern is Null)" +
                " AND ((r.startDate >= :startDate AND r.endDate <= :endDate) OR cast(:startDate as date) is null) " +
                "AND (r.status = :reservationStatus OR :reservationStatus is Null) AND (:hostUserId is Null OR a.host.user.id = :hostUserId)");
        if(title == null){
            q.setParameter("pattern", null);
        }else{
            q.setParameter("pattern", "%" + title + "%");
        }
        q.setParameter("startDate" , startDate);
        q.setParameter("endDate", endDate);
        q.setParameter("reservationStatus", reservationStatus);
        q.setParameter("hostUserId", hostUserId);

        List<ReservationDTO> reservationDTOs = new ArrayList<ReservationDTO>();
        List<Reservation> reservations = q.getResultList();
        for(Reservation reservation : reservations){
            reservationDTOs.add(new ReservationDTO(reservation));
        }
        return reservationDTOs;
    }

    @Override
    public Reservation updateStatus(Long id, ReservationStatus reservationStatus) throws Exception {
        Optional<Reservation> reservation = reservationRepository.findById(id);

        if(reservation.isEmpty()) return null;

        if(reservationStatus.equals(ReservationStatus.ACCEPTED)){
            List<Reservation> overlapsReservations = getOverlaps(reservation.get().getStartDate(), reservation.get().getEndDate(), ReservationStatus.PENDING);
            for(Reservation reservationToDecline : overlapsReservations){
                reservationToDecline.setStatus(ReservationStatus.DECLINED);
                reservationRepository.save(reservationToDecline);
            }
            accommodationService.changePriceList(reservation.get().getStartDate(),reservation.get().getEndDate(), reservation.get().getAccommodation().getId(), AccommodationStatus.RESERVED);
        }

        reservation.get().setStatus(reservationStatus);
        reservationRepository.save(reservation.get());

        return reservation.get();
    }

    @Override
    public Reservation cancelAcceptedReservation(Long id){
        Optional<Reservation> reservation = reservationRepository.findById(id);

        if(reservation.isEmpty()) return null;

        if(reservation.get().getAccommodation().getCancellationPolicy().equals(CancellationPolicy.HOURS24)){
            if (!reservation.get().getStartDate().minusDays(1).isEqual(LocalDate.now())){
                reservation.get().setStatus(ReservationStatus.CANCELLED);
                return reservation.get();
            }
        }else if(reservation.get().getAccommodation().getCancellationPolicy().equals(CancellationPolicy.HOURS48)){
            if (!reservation.get().getStartDate().minusDays(2).isEqual(LocalDate.now())){
                reservation.get().setStatus(ReservationStatus.CANCELLED);
                return reservation.get();
            }
        }else if(reservation.get().getAccommodation().getCancellationPolicy().equals(CancellationPolicy.HOURS72)){
            if (!reservation.get().getStartDate().minusDays(3).isEqual(LocalDate.now())){
                reservation.get().setStatus(ReservationStatus.CANCELLED);
                return reservation.get();
            }
        }
        return null;
    }

    private List<Reservation> getOverlaps(LocalDate startDate, LocalDate endDate, ReservationStatus reservationStatus){
        Query q = em.createQuery("SELECT r FROM Reservation  r WHERE (:startDate < r.endDate AND :endDate > r.startDate) AND r.status = :reservationStatus");
        q.setParameter("startDate", startDate);
        q.setParameter("endDate", endDate);
        q.setParameter("reservationStatus", reservationStatus);
        return q.getResultList();
    }

}
