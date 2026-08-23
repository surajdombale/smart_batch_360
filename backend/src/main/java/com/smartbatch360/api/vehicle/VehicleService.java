package com.smartbatch360.api.vehicle;

import com.smartbatch360.api.batch.BatchRepository;
import com.smartbatch360.api.common.ConflictException;
import com.smartbatch360.api.common.DuplicateResourceException;
import com.smartbatch360.api.common.NotFoundException;
import com.smartbatch360.api.driver.Driver;
import com.smartbatch360.api.driver.DriverRepository;
import com.smartbatch360.api.vehicle.dto.VehicleRequest;
import com.smartbatch360.api.vehicle.dto.VehicleResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class VehicleService {

    private final VehicleRepository vehicleRepository;
    private final DriverRepository driverRepository;
    private final BatchRepository batchRepository;

    public VehicleService(VehicleRepository vehicleRepository, DriverRepository driverRepository,
                           BatchRepository batchRepository) {
        this.vehicleRepository = vehicleRepository;
        this.driverRepository = driverRepository;
        this.batchRepository = batchRepository;
    }

    @Transactional(readOnly = true)
    public List<VehicleResponse> findAll() {
        return vehicleRepository.findAll().stream()
                .map(VehicleResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public VehicleResponse findById(Long id) {
        return VehicleResponse.from(getOrThrow(id));
    }

    public VehicleResponse create(VehicleRequest request) {
        String vehicleNumber = request.vehicleNumber().trim();
        if (vehicleRepository.existsByVehicleNumberIgnoreCase(vehicleNumber)) {
            throw new DuplicateResourceException("A vehicle with number '" + vehicleNumber + "' already exists.");
        }
        Vehicle vehicle = new Vehicle();
        applyRequest(vehicle, request, vehicleNumber);
        return VehicleResponse.from(vehicleRepository.save(vehicle));
    }

    public VehicleResponse update(Long id, VehicleRequest request) {
        Vehicle vehicle = getOrThrow(id);
        String vehicleNumber = request.vehicleNumber().trim();
        if (vehicleRepository.existsByVehicleNumberIgnoreCaseAndIdNot(vehicleNumber, id)) {
            throw new DuplicateResourceException("A vehicle with number '" + vehicleNumber + "' already exists.");
        }
        applyRequest(vehicle, request, vehicleNumber);
        return VehicleResponse.from(vehicleRepository.save(vehicle));
    }

    public void delete(Long id) {
        Vehicle vehicle = getOrThrow(id);
        if (batchRepository.existsByVehicleId(id)) {
            throw new ConflictException("Vehicle '" + vehicle.getVehicleNumber()
                    + "' has one or more production batches and cannot be deleted.");
        }
        vehicleRepository.delete(vehicle);
    }

    Vehicle getOrThrow(Long id) {
        return vehicleRepository.findById(id)
                .orElseThrow(() -> NotFoundException.forId("Vehicle", id));
    }

    private void applyRequest(Vehicle vehicle, VehicleRequest request, String vehicleNumber) {
        vehicle.setVehicleNumber(vehicleNumber);
        vehicle.setCapacityCubicMeters(request.capacityCubicMeters());
        vehicle.setStatus(request.status());

        if (request.driverId() == null) {
            vehicle.setDriver(null);
        } else {
            Driver driver = driverRepository.findById(request.driverId())
                    .orElseThrow(() -> NotFoundException.forId("Driver", request.driverId()));
            vehicle.setDriver(driver);
        }
    }
}
