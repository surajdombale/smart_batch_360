package com.smartbatch360.api.header;

import com.smartbatch360.api.common.NotFoundException;
import com.smartbatch360.api.header.dto.HeaderRequest;
import com.smartbatch360.api.header.dto.HeaderResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HeaderServiceTest {

    @Mock
    private HeaderRepository headerRepository;

    private HeaderService service() {
        return new HeaderService(headerRepository);
    }

    @Test
    void createsHeaderFromRequest() {
        HeaderRequest request = new HeaderRequest("SmartBatch Solutions", "Kharadi Plant",
                "Kharadi, Pune", "9876543210", "info@smartbatch.example", "27ABCDE1234F1Z5", HeaderStatus.ACTIVE);
        when(headerRepository.save(any(Header.class))).thenAnswer(inv -> inv.getArgument(0));

        HeaderResponse response = service().create(request);

        assertThat(response.companyName()).isEqualTo("SmartBatch Solutions");
        assertThat(response.plantName()).isEqualTo("Kharadi Plant");
        assertThat(response.status()).isEqualTo(HeaderStatus.ACTIVE);
        verify(headerRepository).save(any(Header.class));
    }

    @Test
    void blankOptionalFieldsAreStoredAsNull() {
        HeaderRequest request = new HeaderRequest("SmartBatch Solutions", "Kharadi Plant",
                "  ", "", null, "   ", HeaderStatus.ACTIVE);
        when(headerRepository.save(any(Header.class))).thenAnswer(inv -> inv.getArgument(0));

        HeaderResponse response = service().create(request);

        assertThat(response.address()).isNull();
        assertThat(response.phone()).isNull();
        assertThat(response.email()).isNull();
        assertThat(response.gstin()).isNull();
    }

    @Test
    void findByIdThrowsNotFoundWhenMissing() {
        when(headerRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().findById(99L))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void deleteSucceeds() {
        Header header = new Header();
        header.setCompanyName("SmartBatch Solutions");
        when(headerRepository.findById(1L)).thenReturn(Optional.of(header));

        service().delete(1L);

        verify(headerRepository).delete(header);
    }
}
