package com.smartroomfinder.smartroomfinder.repositories;

import com.smartroomfinder.smartroomfinder.entities.Addresses;
import com.smartroomfinder.smartroomfinder.entities.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AddressRepository extends JpaRepository<Addresses, Integer> {

    Optional<Addresses> findByUserAndIsPrimaryTrue(Users user);

    Optional<Addresses> findByAddressIdAndUser(Integer addressId, Users user);
}