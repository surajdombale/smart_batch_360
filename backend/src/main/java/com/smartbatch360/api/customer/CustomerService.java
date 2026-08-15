package com.smartbatch360.api.customer;

import com.smartbatch360.api.common.ConflictException;
import com.smartbatch360.api.common.NotFoundException;
import com.smartbatch360.api.customer.dto.CustomerRequest;
import com.smartbatch360.api.customer.dto.CustomerResponse;
import com.smartbatch360.api.site.SiteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final SiteRepository siteRepository;

    public CustomerService(CustomerRepository customerRepository, SiteRepository siteRepository) {
        this.customerRepository = customerRepository;
        this.siteRepository = siteRepository;
    }

    @Transactional(readOnly = true)
    public List<CustomerResponse> findAll() {
        return customerRepository.findAll().stream()
                .map(CustomerResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public CustomerResponse findById(Long id) {
        return CustomerResponse.from(getOrThrow(id));
    }

    public CustomerResponse create(CustomerRequest request) {
        Customer customer = new Customer();
        applyRequest(customer, request);
        return CustomerResponse.from(customerRepository.save(customer));
    }

    public CustomerResponse update(Long id, CustomerRequest request) {
        Customer customer = getOrThrow(id);
        applyRequest(customer, request);
        return CustomerResponse.from(customerRepository.save(customer));
    }

    public void delete(Long id) {
        Customer customer = getOrThrow(id);
        if (siteRepository.existsByCustomerId(id)) {
            throw new ConflictException("Customer '" + customer.getName()
                    + "' has one or more sites and cannot be deleted. Remove its sites first.");
        }
        customerRepository.delete(customer);
    }

    Customer getOrThrow(Long id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> NotFoundException.forId("Customer", id));
    }

    private void applyRequest(Customer customer, CustomerRequest request) {
        customer.setName(request.name().trim());
        customer.setContactPerson(request.contactPerson().trim());
        customer.setPhone(request.phone().trim());
        customer.setStatus(request.status());
    }
}
