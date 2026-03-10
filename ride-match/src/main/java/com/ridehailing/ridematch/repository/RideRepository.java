package com.ridehailing.ridematch.repository;

import com.ridehailing.ridematch.entity.Ride;
import com.ridehailing.ridematch.enums.RideStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RideRepository extends JpaRepository<Ride, Long> {
    List<Ride> findByStatus(RideStatus status);
}