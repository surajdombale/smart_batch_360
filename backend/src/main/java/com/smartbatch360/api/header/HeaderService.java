package com.smartbatch360.api.header;

import com.smartbatch360.api.common.NotFoundException;
import com.smartbatch360.api.header.dto.HeaderRequest;
import com.smartbatch360.api.header.dto.HeaderResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    private void applyRequest(Header header, HeaderRequest request) {
        header.setCompanyName(request.companyName().trim());
        header.setPlantName(request.plantName().trim());
        header.setAddress(blankToNull(request.address()));
        header.setPhone(blankToNull(request.phone()));
        header.setEmail(blankToNull(request.email()));
        header.setGstin(blankToNull(request.gstin()));
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
