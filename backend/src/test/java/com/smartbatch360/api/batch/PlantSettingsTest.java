package com.smartbatch360.api.batch;

import com.smartbatch360.api.plant.PlantSettingsRepository;
import com.smartbatch360.api.plant.PlantSettingsService;
import com.smartbatch360.api.plant.dto.PlantSettingsRequest;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Where the mixer capacity comes from. It is editable under Settings > Plant
 * Details, so it is read fresh rather than fixed at startup - a plant that
 * changes its mixer should not have to restart the app.
 */
@DataJpaTest
@Import(PlantSettingsService.class)
class PlantSettingsTest {

    @Autowired private PlantSettingsService plantSettingsService;
    @Autowired private PlantSettingsRepository plantSettingsRepository;
    @Autowired private EntityManager entityManager;

    private void plantDetailsSay(BigDecimal mixerCapacity) {
        plantSettingsService.save(new PlantSettingsRequest("R. Patil", mixerCapacity, null));
        entityManager.flush();
    }

    @Test
    void plantDetailsWins() {
        plantDetailsSay(new BigDecimal("1.50"));

        MixerCapacity capacity = new PlantSettings(plantSettingsService, "2").mixerCapacity();

        assertThat(capacity.capacityM3()).isEqualByComparingTo("1.50");
    }

    /** An install that set the property keeps working, and nothing is entered twice. */
    @Test
    void theConfiguredPropertyIsTheFallback() {
        plantDetailsSay(null);

        MixerCapacity capacity = new PlantSettings(plantSettingsService, "2").mixerCapacity();

        assertThat(capacity.capacityM3()).isEqualByComparingTo("2");
    }

    @Test
    void withNeitherSetNothingIsGuessed() {
        plantDetailsSay(null);

        MixerCapacity capacity = new PlantSettings(plantSettingsService, "").mixerCapacity();

        assertThat(capacity.isConfigured()).isFalse();
    }

    @Test
    void aChangeInPlantDetailsTakesEffectWithoutARestart() {
        plantDetailsSay(new BigDecimal("1.00"));
        PlantSettings settings = new PlantSettings(plantSettingsService, "");
        assertThat(settings.mixerCapacity().capacityM3()).isEqualByComparingTo("1.00");

        plantDetailsSay(new BigDecimal("2.50"));

        assertThat(settings.mixerCapacity().capacityM3()).isEqualByComparingTo("2.50");
    }

    /** There is one plant, so there is one row however often it is saved. */
    @Test
    void thereIsOnlyEverOneRowOfPlantSettings() {
        plantDetailsSay(new BigDecimal("1.00"));
        plantDetailsSay(new BigDecimal("2.00"));

        assertThat(plantSettingsRepository.count()).isEqualTo(1);
    }

    @Test
    void plantCapacityIsStoredInCubicMetresPerHour() {
        plantSettingsService.save(new PlantSettingsRequest("R. Patil", new BigDecimal("1.00"),
                new BigDecimal("30.00")));

        assertThat(plantSettingsService.find().plantCapacityM3PerHour()).isEqualByComparingTo("30.00");
    }
}
