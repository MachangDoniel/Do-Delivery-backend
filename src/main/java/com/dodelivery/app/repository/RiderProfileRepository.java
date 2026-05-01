package com.dodelivery.app.repository;

import com.dodelivery.app.entity.RiderProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface RiderProfileRepository extends JpaRepository<RiderProfile, UUID> {

    /**
     * Fetch rider profile by the associated user ID.
     * JOIN FETCH user avoids a second query when the caller needs user details.
     */
    @Query("SELECT rp FROM RiderProfile rp JOIN FETCH rp.user WHERE rp.user.id = :userId")
    Optional<RiderProfile> findByUserIdWithUser(@Param("userId") UUID userId);

    boolean existsByUserId(UUID userId);
}
