package com.smartbatch360.api.site;

import com.smartbatch360.api.batch.BatchRepository;
import com.smartbatch360.api.client.Client;
import com.smartbatch360.api.client.ClientRepository;
import com.smartbatch360.api.common.ConflictException;
import com.smartbatch360.api.common.NotFoundException;
import com.smartbatch360.api.site.dto.SiteRequest;
import com.smartbatch360.api.site.dto.SiteResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class SiteService {

    private final SiteRepository siteRepository;
    private final ClientRepository clientRepository;
    private final BatchRepository batchRepository;

    public SiteService(SiteRepository siteRepository, ClientRepository clientRepository,
                        BatchRepository batchRepository) {
        this.siteRepository = siteRepository;
        this.clientRepository = clientRepository;
        this.batchRepository = batchRepository;
    }

    @Transactional(readOnly = true)
    public List<SiteResponse> findAll() {
        return siteRepository.findAll().stream()
                .map(SiteResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public SiteResponse findById(Long id) {
        return SiteResponse.from(getOrThrow(id));
    }

    public SiteResponse create(SiteRequest request) {
        Site site = new Site();
        applyRequest(site, request);
        return SiteResponse.from(siteRepository.save(site));
    }

    public SiteResponse update(Long id, SiteRequest request) {
        Site site = getOrThrow(id);
        applyRequest(site, request);
        return SiteResponse.from(siteRepository.save(site));
    }

    public void delete(Long id) {
        Site site = getOrThrow(id);
        if (batchRepository.existsBySiteId(id)) {
            throw new ConflictException("Site '" + site.getName()
                    + "' has one or more production batches and cannot be deleted.");
        }
        siteRepository.delete(site);
    }

    Site getOrThrow(Long id) {
        return siteRepository.findById(id)
                .orElseThrow(() -> NotFoundException.forId("Site", id));
    }

    private void applyRequest(Site site, SiteRequest request) {
        Client client = clientRepository.findById(request.clientId())
                .orElseThrow(() -> NotFoundException.forId("Client", request.clientId()));
        site.setName(request.name().trim());
        site.setClient(client);
        site.setLocation(request.location().trim());
        site.setStatus(request.status());
    }
}
