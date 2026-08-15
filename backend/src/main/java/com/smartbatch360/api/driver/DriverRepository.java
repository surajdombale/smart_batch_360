package com.smartbatch360.api.driver;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DriverRepository extends JpaRepository<Driver, Long> {

    Optional<Driver> findByLicenseNoIgnoreCase(String licenseNo);

    boolean existsByLicenseNoIgnoreCase(String licenseNo);

    boolean existsByLicenseNoIgnoreCaseAndIdNot(String licenseNo, Long id);
}
