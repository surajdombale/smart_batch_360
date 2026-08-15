package com.smartbatch360.api.vehicle;

import com.smartbatch360.api.driver.Driver;
import com.smartbatch360.api.driver.DriverStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.jdbc.Sql;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Exercises VehicleRepository against a real (in-memory H2) JPA layer, covering
 * the unique constraint on vehicle_number and the driver-assignment lookup used
 * by DriverService to block deleting an assigned driver.
 */
@DataJpaTest
class VehicleRepositoryTest {

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private jakarta.persistence.EntityManager entityManager;

    private Vehicle vehicle(String number, Driver driver) {
        Vehicle v = new Vehicle();
        v.setVehicleNumber(number);
        v.setDriver(driver);
        v.setCapacityCubicMeters(new BigDecimal("6.00"));
        v.setStatus(driver != null ? VehicleStatus.IN_USE : VehicleStatus.AVAILABLE);
        return v;
    }

    private Driver persistedDriver(String licenseNo) {
        Driver d = new Driver();
        d.setName("Ganesh More");
        d.setPhone("9000000000");
        d.setLicenseNo(licenseNo);
        d.setStatus(DriverStatus.ACTIVE);
        entityManager.persist(d);
        return d;
    }

    @Test
    void existsByVehicleNumberIgnoreCaseIsCaseInsensitive() {
        vehicleRepository.saveAndFlush(vehicle("MH12PQ3457", null));

        assertThat(vehicleRepository.existsByVehicleNumberIgnoreCase("mh12pq3457")).isTrue();
        assertThat(vehicleRepository.existsByVehicleNumberIgnoreCase("MH00XX0000")).isFalse();
    }

    @Test
    void duplicateVehicleNumberViolatesUniqueConstraintAtDbLevel() {
        vehicleRepository.saveAndFlush(vehicle("MH12PQ7788", null));

        assertThatThrownBy(() -> vehicleRepository.saveAndFlush(vehicle("MH12PQ7788", null)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void existsByDriverIdReflectsAssignment() {
        Driver driver = persistedDriver("MH12 2019 123456");
        vehicleRepository.saveAndFlush(vehicle("MH12PQ3457", driver));

        assertThat(vehicleRepository.existsByDriverId(driver.getId())).isTrue();
        assertThat(vehicleRepository.existsByDriverId(driver.getId() + 1)).isFalse();
    }
}
