package com.smartbatch360.api.driver;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Exercises DriverRepository against a real (in-memory H2) JPA layer, covering
 * behaviour the Mockito-based DriverServiceTest cannot: the actual unique
 * constraint on license_no and the custom finder query semantics.
 */
@DataJpaTest
class DriverRepositoryTest {

    @Autowired
    private DriverRepository driverRepository;

    private Driver driver(String name, String licenseNo) {
        Driver d = new Driver();
        d.setName(name);
        d.setPhone("9000000000");
        d.setLicenseNo(licenseNo);
        d.setStatus(DriverStatus.ACTIVE);
        return d;
    }

    @Test
    void existsByLicenseNoIgnoreCaseIsCaseInsensitive() {
        driverRepository.saveAndFlush(driver("Ganesh More", "MH12 2019 123456"));

        assertThat(driverRepository.existsByLicenseNoIgnoreCase("mh12 2019 123456")).isTrue();
        assertThat(driverRepository.existsByLicenseNoIgnoreCase("MH99 0000 000000")).isFalse();
    }

    @Test
    void existsByLicenseNoIgnoreCaseAndIdNotExcludesOwnRecord() {
        Driver saved = driverRepository.saveAndFlush(driver("Ramesh Patil", "MH14 2018 654321"));

        assertThat(driverRepository.existsByLicenseNoIgnoreCaseAndIdNot("MH14 2018 654321", saved.getId())).isFalse();
        assertThat(driverRepository.existsByLicenseNoIgnoreCaseAndIdNot("MH14 2018 654321", saved.getId() + 1)).isTrue();
    }

    @Test
    void duplicateLicenseNoViolatesUniqueConstraintAtDbLevel() {
        driverRepository.saveAndFlush(driver("Suresh Jadhav", "MH12 2017 111222"));

        assertThatThrownBy(() -> driverRepository.saveAndFlush(driver("Sanjay K.", "MH12 2017 111222")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
