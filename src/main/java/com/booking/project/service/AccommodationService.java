package com.booking.project.service;

import com.booking.project.dto.AccommodationDTO;
import com.booking.project.dto.AccommodationCardDTO;
import com.booking.project.model.Accommodation;
import com.booking.project.model.PriceList;
import com.booking.project.model.enums.*;
import com.booking.project.repository.inteface.IAccommodationRepository;
import com.booking.project.service.interfaces.IAccommodationService;
import com.booking.project.service.interfaces.ICommentAboutAccService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;
import java.util.*;

@Service
public class AccommodationService implements IAccommodationService {
    @Autowired
    private IAccommodationRepository accommodationRepository;
    @Autowired
    private EntityManager em;
    @Autowired
    private ImageService imageService;
    @Autowired
    private ICommentAboutAccService commentAboutAccService;
    private static final Double avgRateCriterium = 3.5;
    @Override
    public Collection<AccommodationDTO> findAll() {

        Collection<Accommodation> accommodations = accommodationRepository.findAll();
        Collection<AccommodationDTO> accommodationDTOS = new ArrayList<>();
        for(Accommodation acc: accommodations){
            AccommodationDTO accomodationDTO = new AccommodationDTO(acc);
            accommodationDTOS.add(accomodationDTO);
        }
        return accommodationDTOS;
    }
    public Collection<AccommodationCardDTO> findAllCards() throws IOException {
        Collection<Accommodation> accommodations = accommodationRepository.findAccommodationsByApprovalStatus(AccommodationApprovalStatus.APPROVED);
        Collection<AccommodationCardDTO> accommodationDTOS = new ArrayList<>();
        for(Accommodation acc : accommodations){
            AccommodationCardDTO accomodationDTO = new AccommodationCardDTO(acc);
            accomodationDTO.setImage(imageService.getCoverImage(acc.getImages().split(",")[0]));
            accommodationDTOS.add(accomodationDTO);
        }
        return accommodationDTOS;
    }
    public Optional<AccommodationDTO> changeAccommodations(AccommodationDTO accommodationDTO,Long id) throws Exception {
        Optional<Accommodation> accommodation = findById(id);

        if(accommodation.isEmpty()) return null;

        accommodation.get().setTitle(accommodationDTO.getTitle());
        accommodation.get().setDescription(accommodationDTO.getDescription());
        accommodation.get().setAddress(accommodationDTO.getAddress());
        accommodation.get().setAmenities(accommodationDTO.getAmenities());
//        accommodation.get().setImages(accommodationDTO.getImages());
        accommodation.get().setMinGuests(accommodationDTO.getMinGuest());
        accommodation.get().setMaxGuests(accommodationDTO.getMaxGuest());
        accommodation.get().setType(accommodationDTO.getType());
        accommodation.get().setCancellationPolicy(accommodationDTO.getCancellationPolicy());
        accommodation.get().setReservationMethod(accommodationDTO.getReservationMethod());
        accommodation.get().setAccommodationApprovalStatus(accommodationDTO.getAccommodationApprovalStatus());
        accommodation.get().setPriceForEntireAcc(accommodationDTO.isPriceForEntireAcc());
        accommodation.get().setPrices(accommodationDTO.getPrices());

        save(accommodation.get());

        return Optional.of(accommodationDTO);
    }

    @Override
    public Optional<Accommodation> findById(Long id) {
        return accommodationRepository.findById(id);
    }

    @Override
    public  Accommodation save(Accommodation accommodation) throws Exception {
        return accommodationRepository.save(accommodation);
    }
    @Override
    public List<Object> reservate(Long accommodationId, LocalDate startDate, LocalDate endDate, int numberOfGuests) throws Exception {
        Optional<Accommodation> accommodation = accommodationRepository.findById(accommodationId);

        if(accommodation.isEmpty())  return new ArrayList<Object>(List.of(false));

        double price = 0;
        if(accommodation.get().getAccommodationApprovalStatus().equals(AccommodationApprovalStatus.APPROVED) &&
                (accommodation.get().getMinGuests() <= numberOfGuests && accommodation.get().getMaxGuests() >= numberOfGuests) ){
            int reservationDays = 0;
            List<PriceList> priceLists = new ArrayList<PriceList>();
            for(PriceList priceList : accommodation.get().getPrices()){
                if((priceList.getDate().isAfter(startDate) || priceList.getDate().isEqual(startDate))  &&
                        (priceList.getDate().isBefore(endDate) || priceList.getDate().isEqual(endDate))){
                    if (priceList.getStatus() == AccommodationStatus.AVAILABLE){
                        reservationDays += 1;
                        price += priceList.getPrice();
                        if(accommodation.get().getReservationMethod().equals(ReservationMethod.AUTOMATIC)){
                            priceLists.add(priceList);
                        }
                    }else{
                        return new ArrayList<Object>(List.of(false));
                    }
                }
            }
            Period period = Period.between(startDate, endDate);
            int daysDifference = period.getDays() + 1;

            if (reservationDays != daysDifference){
                return new ArrayList<Object>(List.of(false));
            }

            for(PriceList priceList : priceLists){
                priceList.setStatus(AccommodationStatus.RESERVED);
            }
            if(!accommodation.get().isPriceForEntireAcc()){
                price = price * numberOfGuests;
            }
        }else{
            return new ArrayList<Object>(List.of(false));
        }
        accommodationRepository.save(accommodation.get());
        return new ArrayList<Object>(List.of(false, price, accommodation.get().getReservationMethod()));
    }

    @Override
    public ArrayList<Object> calculateReservationPrice(LocalDate startDate, LocalDate endDate, Long id, int numberOfGuests){
        Optional<Accommodation> accommodation = accommodationRepository.findById(id);

        if(accommodation.isEmpty()) return new ArrayList<Object>(List.of(false));

        double price = 0;
        int reservationDays = 0;
        for(PriceList priceList : accommodation.get().getPrices()){
            if ((priceList.getDate().isAfter(startDate) || priceList.getDate().isEqual(startDate) ) && (priceList.getDate().isBefore(endDate) || priceList.getDate().isEqual(endDate))){
                if(priceList.getStatus().equals(AccommodationStatus.AVAILABLE)){
                    reservationDays += 1;
                    price += priceList.getPrice();
                }else{
                    return new ArrayList<Object>(List.of(false));
                }
            }
        }
        Period period = Period.between(startDate, endDate);
        int daysDifference = period.getDays() + 1;

        if (reservationDays != daysDifference){
            return new ArrayList<Object>(List.of(false));
        }

        if(!accommodation.get().isPriceForEntireAcc()){
            price = price * numberOfGuests;
        }

        return new ArrayList<Object>(List.of(true, price));
    }

    @Override
    public Boolean changePriceList(LocalDate startDate, LocalDate endDate, Long id, AccommodationStatus accommodationStatus) throws Exception {

        Optional<Accommodation> accommodation = accommodationRepository.findById(id);

        if(accommodation.isEmpty()) return false;

        changeAccommodationStatus(accommodation.get(), startDate, endDate, accommodationStatus);
        accommodationRepository.save(accommodation.get());

        return true;
    }


    private void changeAccommodationStatus(Accommodation accommodation, LocalDate startDate, LocalDate endDate, AccommodationStatus accommodationStatus){

        for(PriceList priceList : accommodation.getPrices()){
            if ((priceList.getDate().isAfter(startDate) || priceList.getDate().isEqual(startDate) ) && (priceList.getDate().isBefore(endDate) || priceList.getDate().isEqual(endDate))){
                priceList.setStatus(accommodationStatus);
            }
        }
    }

    @Override
    public List<LocalDate> getAvailableDates(Long id){
        List<LocalDate> availableDates = new ArrayList<LocalDate>();
        Optional<Accommodation> accommodation = accommodationRepository.findById(id);

        if(accommodation.isPresent()) {
            for(PriceList priceList : accommodation.get().getPrices()){
                if (priceList.getStatus().equals(AccommodationStatus.AVAILABLE) && (priceList.getDate().isAfter(LocalDate.now()) || priceList.getDate().isEqual(LocalDate.now()))){
                    availableDates.add(priceList.getDate());
                }
            }
        }
        return availableDates;
     }

    @Override
    public void deleteById(Long id) {
        accommodationRepository.deleteById(id);
    }

    @Override
    public AccommodationDTO changeAvailableStatus(Long id, AccommodationApprovalStatus approvalStatus) throws Exception {
        Optional<Accommodation> accommodation = findById(id);

        if(accommodation.isEmpty()) return null;

        accommodation.get().setAccommodationApprovalStatus(approvalStatus);
        save(accommodation.get());

        AccommodationDTO accommodationDTO = new AccommodationDTO(accommodation.get());

        return accommodationDTO;
    }
    @Override
    public Collection<AccommodationCardDTO> findAccomodationsByHostId(Long id) throws IOException {
        Collection<Accommodation> accommodations = accommodationRepository.findAccommodationsByHost(id);

        Collection<AccommodationCardDTO> accommodationDTOS = new ArrayList<>();
        for(Accommodation accommodation: accommodations){
            AccommodationCardDTO accommodationDTO = new AccommodationCardDTO(accommodation);
            accommodationDTO.setImage(imageService.getCoverImage(accommodation.getImages().split(",")[0]));

            accommodationDTOS.add(accommodationDTO);
        }
        return accommodationDTOS;
    }

    @Override
    public Collection<AccommodationCardDTO> filterAccommodations(LocalDate startDate, LocalDate endDate, Integer numOfGuests,
                                                                 String city, Integer startPrice, Integer endPrice,
                                                                 Collection<Amenities> amenities,
                                                                 AccommodationType accommodationType)
    throws IOException {

        int amenitiesSize = 0;
        if(amenities != null){
            amenitiesSize = amenities.size();
        }
        Collection<Accommodation> accommodations = accommodationRepository.filterAccommodations(startDate,endDate,
                city,numOfGuests,startPrice,endPrice,amenities, amenitiesSize,accommodationType,
                AccommodationApprovalStatus.APPROVED);
        Collection<AccommodationCardDTO> accommodationDTOS = new ArrayList<>();
        for(Accommodation acc: accommodations){

            AccommodationCardDTO accomodationDTO = new AccommodationCardDTO(acc);
            Double avgRate = commentAboutAccService.findAvgRateById(acc.getId());
            accomodationDTO.setAvgRate(avgRate);

            if(startDate != null && endDate != null){
                Double totalPrice = accommodationRepository.findTotalPriceForDateInterval(acc.getId(),startDate,endDate);
                Double pricePerNight = accommodationRepository.findOneNightPrice(startDate.plusDays(1),acc.getId());
                accomodationDTO.setTotalPrice(totalPrice);
                accomodationDTO.setPricePerNight(pricePerNight);
            }



            accomodationDTO.setImage(imageService.getCoverImage(acc.getImages().split(",")[0]));
            accommodationDTOS.add(accomodationDTO);
        }

        return accommodationDTOS;
    }
    @Override
    public AccommodationDTO findAccommodationsDetails(Long id) throws IOException {
        Optional<Accommodation> accommodation = findById(id);

        if(accommodation.isEmpty()) return null;

        AccommodationDTO accommodationDTO = new AccommodationDTO(accommodation.get());
        accommodationDTO.setImages(imageService.getImages(accommodation.get().getImages().split(",")));
        return accommodationDTO;
    }

    @Override
    public Collection<AccommodationCardDTO> findApprovalPendingAccommodations() throws IOException {
        Collection<Accommodation> accommodations = accommodationRepository.findAccommodationsByAccommodationApprovalStatus(AccommodationApprovalStatus.PENDING);

        List<AccommodationCardDTO> accommodationCards = new ArrayList<AccommodationCardDTO>();
        for(Accommodation accommodation : accommodations){
            AccommodationCardDTO card = new AccommodationCardDTO(accommodation);
            card.setImage(imageService.getCoverImage(accommodation.getImages().split(",")[0]));
            accommodationCards.add(card);
        }

        return accommodationCards;
    }
    @Override
    public Collection<AccommodationCardDTO> findPopularAccommodations() throws IOException {
        Collection<Object[]> popularAccommodations = commentAboutAccService.findAccommodationsByRating();
        List<AccommodationCardDTO> accommodationCards = new ArrayList<AccommodationCardDTO>();
        for(Object[] accommodationAndAvgRate : popularAccommodations){
            Double avgRate =(Double) accommodationAndAvgRate[1];

            if(avgRate < avgRateCriterium) continue;

            Accommodation accommodation = (Accommodation) accommodationAndAvgRate[0];
            AccommodationCardDTO card = new AccommodationCardDTO(accommodation);
            card.setImage(imageService.getCoverImage(accommodation.getImages().split(",")[0]));
            card.setAvgRate(avgRate);
            accommodationCards.add(card);
        }

        return accommodationCards;
    }

    @Override
    public AccommodationDTO changeAccommodationReservationMethod(Long id, ReservationMethod reservationMethod) throws Exception {
        Optional<Accommodation> accommodation = findById(id);

        if(accommodation.isEmpty()) return null;

        accommodation.get().setReservationMethod(reservationMethod);
        save(accommodation.get());

        AccommodationDTO accommodationDTO = new AccommodationDTO(accommodation.get());

        return accommodationDTO;
    }

    @Override
    public AccommodationDTO saveImages(String images, Long accommodationId) throws Exception {
        Optional<Accommodation> accommodation = findById(accommodationId);

        if(accommodation.isEmpty()) return null;

        accommodation.get().setImages(images);
        save(accommodation.get());

        AccommodationDTO accommodationDTO = new AccommodationDTO(accommodation.get());

        return accommodationDTO;
    }

    @Override
    public String getImages(Long accommodationId){
        Optional<Accommodation> accommodation = findById(accommodationId);

        if(accommodation.isEmpty()) return null;

        return accommodation.get().getImages();
    }
    @Override
    public Collection<Double> getMinMaxPrice(){
        Collection<Double> minMaxPrice= new ArrayList<>();

        Double minPrice = accommodationRepository.findMinPrice();
        Double maxPrice = accommodationRepository.findMaxPrice();
        minMaxPrice.add(minPrice);
        minMaxPrice.add(maxPrice);

        return minMaxPrice;
    }

    @Override
    public List<PriceList> findPriceList(Long id) {
        return accommodationRepository.findPriceList(id, AccommodationStatus.AVAILABLE);
    }

    @Override
    public List<AccommodationDTO> getGuestAccommodations(Long guestUserId){

        Query q = em.createQuery("select r.accommodation from Reservation r join r.accommodation a " +
                "where r.guest.user.id = :guestUserId and r.status = 'ACCEPTED' and r.endDate < current_timestamp()");
        q.setParameter("guestUserId" , guestUserId);

        List<AccommodationDTO> accommodationDTOS = new ArrayList<AccommodationDTO>();
        List<Accommodation> accommodations = q.getResultList();
        for(Accommodation accommodation : accommodations){
            accommodationDTOS.add(new AccommodationDTO(accommodation));
        }

        return accommodationDTOS;
    }

    @Override
    public List<AccommodationDTO> getGuestAccommodationsForComment(Long guestUserId) throws IOException {

        Query q = em.createQuery("SELECT r.accommodation FROM Reservation r " +
                "JOIN r.accommodation a " +
                "WHERE r.guest.user.id = :guestUserId " +
                "AND r.status = 'ACCEPTED' " +
                "AND current_timestamp() >= r.endDate " +
                "AND current_timestamp() - r.endDate <= :daysInterval");
        q.setParameter("daysInterval", Duration.ofDays(7));
        q.setParameter("guestUserId", guestUserId);


        List<AccommodationDTO> accommodationDTOS = new ArrayList<AccommodationDTO>();
        List<Accommodation> accommodations = q.getResultList();

        for(Accommodation accommodation : accommodations){
            AccommodationDTO accommodationDTO = new AccommodationDTO(accommodation);
            List<byte[]> images = new ArrayList<>();
            images.add(imageService.getCoverImage(accommodation.getImages().split(",")[0]));
            accommodationDTO.setImages(images);
            accommodationDTOS.add(accommodationDTO);
        }

        return accommodationDTOS;
    }

}


