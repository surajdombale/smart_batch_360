package com.smartbatch360.api.site;

import com.smartbatch360.api.client.Client;
import com.smartbatch360.api.client.ClientStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exercises SiteRepository against a real (in-memory H2) JPA layer, covering the
 * existsByClientId lookup used by ClientService to block deleting a client
 * that still has sites.
 */
@DataJpaTest
class SiteRepositoryTest {

    @Autowired
    private SiteRepository siteRepository;

    @Autowired
    private jakarta.persistence.EntityManager entityManager;

    @Test
    void existsByClientIdReflectsLinkedSites() {
        Client client = new Client();
        client.setName("SmartBatch Solutions");
        client.setContactPerson("Rahul Deshmukh");
        client.setPhone("9876543210");
        client.setStatus(ClientStatus.ACTIVE);
        entityManager.persist(client);

        Client clientWithoutSites = new Client();
        clientWithoutSites.setName("Afcons Infrastructure");
        clientWithoutSites.setContactPerson("Pramod Patil");
        clientWithoutSites.setPhone("9899909998");
        clientWithoutSites.setStatus(ClientStatus.ACTIVE);
        entityManager.persist(clientWithoutSites);

        Site site = new Site();
        site.setName("Kharadi");
        site.setClient(client);
        site.setLocation("Pune");
        site.setStatus(SiteStatus.ACTIVE);
        siteRepository.saveAndFlush(site);

        assertThat(siteRepository.existsByClientId(client.getId())).isTrue();
        assertThat(siteRepository.existsByClientId(clientWithoutSites.getId())).isFalse();
    }
}
