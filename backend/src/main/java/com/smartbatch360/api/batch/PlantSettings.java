package com.smartbatch360.api.batch;

import com.smartbatch360.api.header.Header;
import com.smartbatch360.api.header.HeaderRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Plant settings an operator can change, read fresh each time rather than fixed
 * at startup.
 *
 * Mixer capacity lives on Company Details, with the other plant figures the
 * batch report prints. It was configuration only until 06-Oct-2026, when the
 * user asked for it to stay editable - changing a mixer should not need a file
 * edit and a restart.
 *
 * The configured property is still honoured as a fallback, so an install that
 * sets it keeps working and nothing has to be entered twice.
 */
@Service
public class PlantSettings {

    private final HeaderRepository headerRepository;
    private final String configuredMixerCapacity;

    public PlantSettings(HeaderRepository headerRepository,
                         @Value("${smartbatch360.plant.mixer-capacity-m3:}") String configuredMixerCapacity) {
        this.headerRepository = headerRepository;
        this.configuredMixerCapacity = configuredMixerCapacity;
    }

    /**
     * What the company row says, or the configured property, or nothing - in
     * which case planning refuses with a message naming where to set it, rather
     * than guessing a capacity the plant does not have.
     */
    @Transactional(readOnly = true)
    public MixerCapacity mixerCapacity() {
        BigDecimal fromCompany = companyMixerCapacity();
        if (fromCompany != null) {
            return MixerCapacity.of(fromCompany.toPlainString());
        }
        return new MixerCapacity(configuredMixerCapacity);
    }

    private BigDecimal companyMixerCapacity() {
        List<Header> companies = headerRepository.findAll();
        for (Header company : companies) {
            if (company.getMixerCapacityM3() != null) {
                return company.getMixerCapacityM3();
            }
        }
        return null;
    }
}
