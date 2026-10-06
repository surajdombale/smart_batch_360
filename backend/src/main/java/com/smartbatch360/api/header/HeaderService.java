package com.smartbatch360.api.header;

import com.smartbatch360.api.common.NotFoundException;
import com.smartbatch360.api.header.dto.HeaderLogoRequest;
import com.smartbatch360.api.header.dto.HeaderLogoResponse;
import com.smartbatch360.api.common.InvalidRequestException;
import com.smartbatch360.api.header.dto.HeaderRequest;
import com.smartbatch360.api.header.dto.HeaderResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.List;

@Service
@Transactional
public class HeaderService {

    private final HeaderRepository headerRepository;

    public HeaderService(HeaderRepository headerRepository) {
        this.headerRepository = headerRepository;
    }

    @Transactional(readOnly = true)
    public List<HeaderResponse> findAll() {
        return headerRepository.findAll().stream()
                .map(HeaderResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public HeaderResponse findById(Long id) {
        return HeaderResponse.from(getOrThrow(id));
    }

    public HeaderResponse create(HeaderRequest request) {
        Header header = new Header();
        applyRequest(header, request);
        return HeaderResponse.from(headerRepository.save(header));
    }

    public HeaderResponse update(Long id, HeaderRequest request) {
        Header header = getOrThrow(id);
        applyRequest(header, request);
        return HeaderResponse.from(headerRepository.save(header));
    }

    public void delete(Long id) {
        Header header = getOrThrow(id);
        // No other Phase 1 table references Header - nothing to guard against yet.
        headerRepository.delete(header);
    }

    Header getOrThrow(Long id) {
        return headerRepository.findById(id)
                .orElseThrow(() -> NotFoundException.forId("Header", id));
    }

    /** The formats PDFBox can draw on a report, and nothing else. */
    private static final Set<String> SUPPORTED_LOGO_TYPES = Set.of("image/png", "image/jpeg");

    /** Comfortably more than a letterhead needs, and small enough to keep a form responsive. */
    private static final int MAX_LOGO_BYTES = 2 * 1024 * 1024;

    public void saveLogo(Long id, HeaderLogoRequest request) {
        Header header = getOrThrow(id);
        String contentType = request.contentType().trim().toLowerCase();

        if (!SUPPORTED_LOGO_TYPES.contains(contentType)) {
            throw new InvalidRequestException("A logo must be a PNG or JPEG image, but this is '"
                    + request.contentType().trim() + "'.");
        }
        if (request.data().length == 0) {
            throw new InvalidRequestException("The logo image is empty.");
        }
        if (request.data().length > MAX_LOGO_BYTES) {
            throw new InvalidRequestException("The logo is " + (request.data().length / 1024)
                    + " KB, which is over the " + (MAX_LOGO_BYTES / 1024) + " KB limit.");
        }

        header.setLogo(request.data());
        header.setLogoContentType(contentType);
        headerRepository.save(header);
    }

    @Transactional(readOnly = true)
    public HeaderLogoResponse findLogo(Long id) {
        Header header = getOrThrow(id);
        if (header.getLogo() == null || header.getLogo().length == 0) {
            throw new NotFoundException("Company #" + id + " has no logo.");
        }
        return new HeaderLogoResponse(header.getLogoContentType(), header.getLogo());
    }

    public void deleteLogo(Long id) {
        Header header = getOrThrow(id);
        header.setLogo(null);
        header.setLogoContentType(null);
        headerRepository.save(header);
    }

    private void applyRequest(Header header, HeaderRequest request) {
        header.setCompanyName(request.companyName().trim());
        header.setPlantName(request.plantName().trim());
        header.setAddress(blankToNull(request.address()));
        header.setCity(blankToNull(request.city()));
        header.setPinCode(blankToNull(request.pinCode()));
        header.setPhone(blankToNull(request.phone()));
        header.setEmail(blankToNull(request.email()));
        header.setGstin(blankToNull(request.gstin()));
        header.setSupervisorName(blankToNull(request.supervisorName()));
        header.setMixTimeSeconds(request.mixTimeSeconds());
        header.setDischargeTimeSeconds(request.dischargeTimeSeconds());
        header.setMixerCapacityM3(request.mixerCapacityM3());
        header.setStatus(request.status());
    }

    private String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
