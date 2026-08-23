package com.smartbatch360.api.dashboard;

import com.smartbatch360.api.client.ClientRepository;
import com.smartbatch360.api.driver.DriverRepository;
import com.smartbatch360.api.header.HeaderRepository;
import com.smartbatch360.api.site.SiteRepository;
import com.smartbatch360.api.vehicle.VehicleRepository;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthComponent;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.health.Status;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DashboardService {

    private final ClientRepository clientRepository;
    private final SiteRepository siteRepository;
    private final VehicleRepository vehicleRepository;
    private final DriverRepository driverRepository;
    private final HeaderRepository headerRepository;
    private final HealthEndpoint healthEndpoint;

    public DashboardService(ClientRepository clientRepository,
                             SiteRepository siteRepository,
                             VehicleRepository vehicleRepository,
                             DriverRepository driverRepository,
                             HeaderRepository headerRepository,
                             HealthEndpoint healthEndpoint) {
        this.clientRepository = clientRepository;
        this.siteRepository = siteRepository;
        this.vehicleRepository = vehicleRepository;
        this.driverRepository = driverRepository;
        this.headerRepository = headerRepository;
        this.healthEndpoint = healthEndpoint;
    }

    @Transactional(readOnly = true)
    public DashboardSummaryResponse getSummary() {
        HealthComponent overall = healthEndpoint.health();
        String backendStatus = overall.getStatus().equals(Status.UP) ? "UP" : "DOWN";
        String databaseStatus = resolveDatabaseStatus(overall);

        return new DashboardSummaryResponse(
                clientRepository.count(),
                siteRepository.count(),
                vehicleRepository.count(),
                driverRepository.count(),
                headerRepository.count(),
                backendStatus,
                databaseStatus,
                "UP"
        );
    }

    /** Spring Boot auto-registers a "db" health indicator when a DataSource is present. */
    private String resolveDatabaseStatus(HealthComponent overall) {
        if (overall instanceof Health health) {
            return health.getStatus().equals(Status.UP) ? "UP" : "DOWN";
        }
        if (overall instanceof org.springframework.boot.actuate.health.CompositeHealth composite) {
            HealthComponent db = composite.getComponents().get("db");
            if (db != null) {
                return db.getStatus().equals(Status.UP) ? "UP" : "DOWN";
            }
        }
        return overall.getStatus().equals(Status.UP) ? "UP" : "DOWN";
    }
}
