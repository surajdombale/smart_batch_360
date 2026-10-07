package com.smartbatch360.api.plant;

import com.smartbatch360.api.plant.dto.PlantSettingsRequest;
import com.smartbatch360.api.plant.dto.PlantSettingsResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Settings > Plant Details, which is one row.
 *
 * Reading creates it if the migration has not, so the screen never has to deal
 * with "no settings exist yet" - an empty form is the same thing.
 */
@Service
@Transactional
public class PlantSettingsService {

    private final PlantSettingsRepository repository;

    public PlantSettingsService(PlantSettingsRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public PlantSettingsResponse find() {
        return PlantSettingsResponse.from(current());
    }

    public PlantSettingsResponse save(PlantSettingsRequest request) {
        PlantSettingsEntity settings = current();
        settings.setSupervisorName(blankToNull(request.supervisorName()));
        settings.setMixerCapacityM3(request.mixerCapacityM3());
        settings.setPlantCapacityM3PerHour(request.plantCapacityM3PerHour());
        return PlantSettingsResponse.from(repository.save(settings));
    }

    /** The mixer capacity, for production planning. Null when it has not been set. */
    @Transactional(readOnly = true)
    public BigDecimal mixerCapacityM3() {
        return current().getMixerCapacityM3();
    }

    private PlantSettingsEntity current() {
        return repository.findById(PlantSettingsEntity.SINGLETON_ID)
                .orElseGet(() -> repository.save(new PlantSettingsEntity()));
    }

    private String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
