package com.smartbatch360.api.site;

import com.smartbatch360.api.common.NotFoundException;
import com.smartbatch360.api.customer.Customer;
import com.smartbatch360.api.customer.CustomerRepository;
import com.smartbatch360.api.site.dto.SiteRequest;
import com.smartbatch360.api.site.dto.SiteResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class SiteService {

    private final SiteRepository siteRepository;
    private final CustomerRepository customerRepository;

    public SiteService(SiteRepository siteRepository, CustomerRepository customerRepository) {
        this.siteRepository = siteRepository;
        this.customerRepository = customerRepository;
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
        siteRepository.delete(site);
    }

    Site getOrThrow(Long id) {
        return siteRepository.findById(id)
                .orElseThrow(() -> NotFoundException.forId("Site", id));
    }

    private void applyRequest(Site site, SiteRequest request) {
        Customer customer = customerRepository.findById(request.customerId())
                .orElseThrow(() -> NotFoundException.forId("Customer", request.customerId()));
        site.setName(request.name().trim());
        site.setCustomer(customer);
        site.setLocation(request.location().trim());
        site.setStatus(request.status());
    }
}
