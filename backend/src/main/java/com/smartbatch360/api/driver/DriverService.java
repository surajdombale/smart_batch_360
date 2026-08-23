package com.smartbatch360.api.driver;

import com.smartbatch360.api.batch.BatchRepository;
import com.smartbatch360.api.common.ConflictException;
import com.smartbatch360.api.common.DuplicateResourceException;
import com.smartbatch360.api.common.NotFoundException;
import com.smartbatch360.api.driver.dto.DriverRequest;
import com.smartbatch360.api.driver.dto.DriverResponse;
import com.smartbatch360.api.vehicle.VehicleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class DriverService {

    private final DriverRepository driverRepository;
    private final VehicleRepository vehicleRepository;
    private final BatchRepository batchRepository;

    public DriverService(DriverRepository driverRepository, VehicleRepository vehicleRepository,
                          BatchRepository batchRepository) {
        this.driverRepository = driverRepository;
        this.vehicleRepository = vehicleRepository;
        this.batchRepository = batchRepository;
    }

    @Transactional(readOnly = true)
    public List<DriverResponse> findAll() {
        return driverRepository.findAll().stream()
                .map(DriverResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public DriverResponse findById(Long id) {
        return DriverResponse.from(getOrThrow(id));
    }

    public DriverResponse create(DriverRequest request) {
        String licenseNo = request.licenseNo().trim();
        if (driverRepository.existsByLicenseNoIgnoreCase(licenseNo)) {
            throw new DuplicateResourceException("A driver with license number '" + licenseNo + "' already exists.");
        }
        Driver driver = new Driver();
        applyRequest(driver, request, licenseNo);
        return DriverResponse.from(driverRepository.save(driver));
    }

    public DriverResponse update(Long id, DriverRequest request) {
        Driver driver = getOrThrow(id);
        String licenseNo = request.licenseNo().trim();
        if (driverRepository.existsByLicenseNoIgnoreCaseAndIdNot(licenseNo, id)) {
            throw new DuplicateResourceException("A driver with license number '" + licenseNo + "' already exists.");
        }
        applyRequest(driver, request, licenseNo);
        return DriverResponse.from(driverRepository.save(driver));
    }

    public void delete(Long id) {
        Driver driver = getOrThrow(id);
        if (vehicleRepository.existsByDriverId(id)) {
            throw new ConflictException("Driver '" + driver.getName()
                    + "' is assigned to a vehicle and cannot be deleted. Unassign the vehicle first.");
        }
        if (batchRepository.existsByDriverId(id)) {
            throw new ConflictException("Driver '" + driver.getName()
                    + "' has one or more production batches and cannot be deleted.");
        }
        driverRepository.delete(driver);
    }

    Driver getOrThrow(Long id) {
        return driverRepository.findById(id)
                .orElseThrow(() -> NotFoundException.forId("Driver", id));
    }

    private void applyRequest(Driver driver, DriverRequest request, String licenseNo) {
        driver.setName(request.name().trim());
        driver.setPhone(request.phone().trim());
        driver.setLicenseNo(licenseNo);
        driver.setStatus(request.status());
    }
}
