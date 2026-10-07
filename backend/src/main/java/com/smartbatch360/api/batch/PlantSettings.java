package com.smartbatch360.api.batch;

import com.smartbatch360.api.plant.PlantSettingsService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Plant settings an operator can change, read fresh each time rather than fixed
 * at startup.
 *
 * Mixer capacity lives under Settings > Plant Details, moved there from Company
 * Details on 07-Oct-2026 - the company's details are one thing, the plant's
 * figures another. It was configuration only until 06-Oct, when the user asked
 * for it to stay editable; changing a mixer should not need a file edit and a
 * restart.
 *
 * The configured property is still honoured as a fallback, so an install that
 * sets it keeps working and nothing has to be entered twice.
 */
@Service
public class PlantSettings {

    private final PlantSettingsService plantSettingsService;
    private final String configuredMixerCapacity;

    public PlantSettings(PlantSettingsService plantSettingsService,
                         @Value("${smartbatch360.plant.mixer-capacity-m3:}") String configuredMixerCapacity) {
        this.plantSettingsService = plantSettingsService;
        this.configuredMixerCapacity = configuredMixerCapacity;
    }

    /**
     * What Plant Details says, or the configured property, or nothing - in which
     * case planning refuses with a message naming where to set it, rather than
     * guessing a capacity the plant does not have.
     */
    @Transactional(readOnly = true)
    public MixerCapacity mixerCapacity() {
        BigDecimal fromSettings = plantSettingsService.mixerCapacityM3();
        if (fromSettings != null) {
            return MixerCapacity.of(fromSettings.toPlainString());
        }
        return new MixerCapacity(configuredMixerCapacity);
    }
}
