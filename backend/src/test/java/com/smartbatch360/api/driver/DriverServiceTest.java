package com.smartbatch360.api.driver;

import com.smartbatch360.api.common.ConflictException;
import com.smartbatch360.api.common.DuplicateResourceException;
import com.smartbatch360.api.driver.dto.DriverRequest;
import com.smartbatch360.api.vehicle.VehicleRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DriverServiceTest {

    @Mock
    private DriverRepository driverRepository;

    @Mock
    private VehicleRepository vehicleRepository;

    private DriverService service() {
        return new DriverService(driverRepository, vehicleRepository);
    }

    @Test
    void createRejectsDuplicateLicenseNo() {
        when(driverRepository.existsByLicenseNoIgnoreCase("MH12 2019 123456")).thenReturn(true);
        DriverRequest request = new DriverRequest("Ganesh More", "9822345678", "MH12 2019 123456", DriverStatus.ACTIVE);

        assertThatThrownBy(() -> service().create(request))
                .isInstanceOf(DuplicateResourceException.class);

        verify(driverRepository, never()).save(any());
    }

    @Test
    void deleteRejectedWhenAssignedToVehicle() {
        Driver driver = new Driver();
        driver.setName("Ganesh More");
        when(driverRepository.findById(1L)).thenReturn(Optional.of(driver));
        when(vehicleRepository.existsByDriverId(1L)).thenReturn(true);

        assertThatThrownBy(() -> service().delete(1L))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("assigned to a vehicle");

        verify(driverRepository, never()).delete(any());
    }

    @Test
    void deleteSucceedsWhenNotAssigned() {
        Driver driver = new Driver();
        driver.setName("Suresh Jadhav");
        when(driverRepository.findById(2L)).thenReturn(Optional.of(driver));
        when(vehicleRepository.existsByDriverId(2L)).thenReturn(false);

        service().delete(2L);

        verify(driverRepository).delete(driver);
    }
}
