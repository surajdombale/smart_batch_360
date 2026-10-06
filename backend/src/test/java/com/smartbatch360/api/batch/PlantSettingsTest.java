package com.smartbatch360.api.batch;

import com.smartbatch360.api.header.Header;
import com.smartbatch360.api.header.HeaderRepository;
import com.smartbatch360.api.header.HeaderStatus;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Where the mixer capacity comes from. It is editable on Company Details, so it
 * is read fresh rather than fixed at startup - a plant that changes its mixer
 * should not have to restart the app.
 */
@DataJpaTest
class PlantSettingsTest {

    @Autowired private HeaderRepository headerRepository;
    @Autowired private EntityManager entityManager;

    private void company(BigDecimal mixerCapacity) {
        Header header = new Header();
        header.setCompanyName("SmartBatch Solutions");
        header.setPlantName("Kharadi Plant");
        header.setStatus(HeaderStatus.ACTIVE);
        header.setMixerCapacityM3(mixerCapacity);
        entityManager.persist(header);
        entityManager.flush();
    }

    @Test
    void theCompanyRowWins() {
        company(new BigDecimal("1.50"));

        MixerCapacity capacity = new PlantSettings(headerRepository, "2").mixerCapacity();

        assertThat(capacity.capacityM3()).isEqualByComparingTo("1.50");
    }

    /** An install that set the property keeps working, and nothing is entered twice. */
    @Test
    void theConfiguredPropertyIsTheFallback() {
        company(null);

        MixerCapacity capacity = new PlantSettings(headerRepository, "2").mixerCapacity();

        assertThat(capacity.capacityM3()).isEqualByComparingTo("2");
    }

    @Test
    void withNeitherSetNothingIsGuessed() {
        company(null);

        MixerCapacity capacity = new PlantSettings(headerRepository, "").mixerCapacity();

        assertThat(capacity.isConfigured()).isFalse();
    }

    @Test
    void aChangeOnCompanyDetailsTakesEffectWithoutARestart() {
        company(new BigDecimal("1.00"));
        PlantSettings settings = new PlantSettings(headerRepository, "");
        assertThat(settings.mixerCapacity().capacityM3()).isEqualByComparingTo("1.00");

        headerRepository.findAll().get(0).setMixerCapacityM3(new BigDecimal("2.50"));
        entityManager.flush();

        assertThat(settings.mixerCapacity().capacityM3()).isEqualByComparingTo("2.50");
    }
}
