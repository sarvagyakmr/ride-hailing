package com.ridehailing.ridematch.repository;

import com.ridehailing.ridematch.entity.Vehicle;
import com.ridehailing.ridematch.enums.VehicleStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VehicleRepository extends JpaRepository<Vehicle, Long> {
    List<Vehicle> findByStatus(VehicleStatus status);
}