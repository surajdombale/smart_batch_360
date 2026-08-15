package com.smartbatch360.api.site;

import com.smartbatch360.api.customer.Customer;
import com.smartbatch360.api.customer.CustomerStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exercises SiteRepository against a real (in-memory H2) JPA layer, covering the
 * existsByCustomerId lookup used by CustomerService to block deleting a customer
 * that still has sites.
 */
@DataJpaTest
class SiteRepositoryTest {

    @Autowired
    private SiteRepository siteRepository;

    @Autowired
    private jakarta.persistence.EntityManager entityManager;

    @Test
    void existsByCustomerIdReflectsLinkedSites() {
        Customer customer = new Customer();
        customer.setName("SmartBatch Solutions");
        customer.setContactPerson("Rahul Deshmukh");
        customer.setPhone("9876543210");
        customer.setStatus(CustomerStatus.ACTIVE);
        entityManager.persist(customer);

        Customer customerWithoutSites = new Customer();
        customerWithoutSites.setName("Afcons Infrastructure");
        customerWithoutSites.setContactPerson("Pramod Patil");
        customerWithoutSites.setPhone("9899909998");
        customerWithoutSites.setStatus(CustomerStatus.ACTIVE);
        entityManager.persist(customerWithoutSites);

        Site site = new Site();
        site.setName("Kharadi");
        site.setCustomer(customer);
        site.setLocation("Pune");
        site.setStatus(SiteStatus.ACTIVE);
        siteRepository.saveAndFlush(site);

        assertThat(siteRepository.existsByCustomerId(customer.getId())).isTrue();
        assertThat(siteRepository.existsByCustomerId(customerWithoutSites.getId())).isFalse();
    }
}
