package com.smartbatch360.api.site;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SiteRepository extends JpaRepository<Site, Long> {

    boolean existsByClientId(Long clientId);
}
