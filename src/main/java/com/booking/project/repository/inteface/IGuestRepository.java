package com.booking.project.repository.inteface;

import com.booking.project.model.Accommodation;
import com.booking.project.model.Guest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Optional;

public interface IGuestRepository extends JpaRepository<Guest, Long>{
    @Query(
            "SELECT g.favourites " +
                    "FROM Guest g " +
                    "where g.user.id = :guestUserId "
    )
    public Collection<Accommodation> findByFavourites(
            @Param("guestUserId") Long guestUserid);
    Optional<Guest> findByUserId(Long id);
}
