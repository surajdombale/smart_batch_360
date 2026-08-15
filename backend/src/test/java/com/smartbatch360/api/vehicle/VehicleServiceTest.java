package com.smartbatch360.api.vehicle;

import com.smartbatch360.api.common.DuplicateResourceException;
import com.smartbatch360.api.driver.Driver;
import com.smartbatch360.api.driver.DriverRepository;
import com.smartbatch360.api.vehicle.dto.VehicleRequest;
import com.smartbatch360.api.vehicle.dto.VehicleResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VehicleServiceTest {

    @Mock
    private VehicleRepository vehicleRepository;

    @Mock
    private DriverRepository driverRepository;

    private VehicleService service() {
        return new VehicleService(vehicleRepository, driverRepository);
    }

    @Test
    void createRejectsDuplicateVehicleNumber() {
        when(vehicleRepository.existsByVehicleNumberIgnoreCase("MH12PQ3457")).thenReturn(true);
        VehicleRequest request = new VehicleRequest("MH12PQ3457", null, new BigDecimal("6.00"), VehicleStatus.AVAILABLE);

        assertThatThrownBy(() -> service().create(request))
                .isInstanceOf(DuplicateResourceException.class);

        verify(vehicleRepository, never()).save(any());
    }

    @Test
    void createAllowsNoDriverAssigned() {
        when(vehicleRepository.existsByVehicleNumberIgnoreCase("MH12PQ7788")).thenReturn(false);
        when(vehicleRepository.save(any(Vehicle.class))).thenAnswer(inv -> inv.getArgument(0));

        VehicleRequest request = new VehicleRequest("MH12PQ7788", null, new BigDecimal("6.00"), VehicleStatus.AVAILABLE);
        VehicleResponse response = service().create(request);

        assertThat(response.driverId()).isNull();
        verify(driverRepository, never()).findById(any());
    }

    @Test
    void createRejectsUnknownDriver() {
        when(vehicleRepository.existsByVehicleNumberIgnoreCase("MH14DK5678")).thenReturn(false);
        when(driverRepository.findById(9L)).thenReturn(Optional.empty());

        VehicleRequest request = new VehicleRequest("MH14DK5678", 9L, new BigDecimal("7.00"), VehicleStatus.IN_USE);

        assertThatThrownBy(() -> service().create(request))
                .isInstanceOf(com.smartbatch360.api.common.NotFoundException.class);
    }

    @Test
    void createAssignsExistingDriver() {
        Driver driver = new Driver();
        driver.setName("Ganesh More");
        when(vehicleRepository.existsByVehicleNumberIgnoreCase("MH12PQ3457")).thenReturn(false);
        when(driverRepository.findById(1L)).thenReturn(Optional.of(driver));
        when(vehicleRepository.save(any(Vehicle.class))).thenAnswer(inv -> inv.getArgument(0));

        VehicleRequest request = new VehicleRequest("MH12PQ3457", 1L, new BigDecimal("6.00"), VehicleStatus.IN_USE);
        VehicleResponse response = service().create(request);

        assertThat(response.driverName()).isEqualTo("Ganesh More");
    }
}
