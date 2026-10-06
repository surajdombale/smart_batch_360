package com.smartbatch360.api.header;

import com.smartbatch360.api.common.NotFoundException;
import com.smartbatch360.api.header.dto.HeaderLogoRequest;
import com.smartbatch360.api.common.InvalidRequestException;
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
                "Kharadi, Pune", "Pune", "411014", "9876543210", "info@smartbatch.example",
                "27ABCDE1234F1Z5", "R. Patil", 30, 20, new java.math.BigDecimal("1.50"), HeaderStatus.ACTIVE);
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
                "  ", "", null, "", null, "   ", "  ", null, null, null, HeaderStatus.ACTIVE);
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

    /**
     * The logo is drawn on every batch report by PDFBox, which can place a PNG
     * or a JPEG and nothing else - so a file it cannot draw is refused here
     * rather than failing when someone prints a report.
     */
    @Test
    void aLogoMustBeAnImageTheReportCanDraw() {
        Header header = new Header();
        when(headerRepository.findById(1L)).thenReturn(Optional.of(header));

        assertThatThrownBy(() -> service().saveLogo(1L,
                new HeaderLogoRequest("application/pdf", new byte[]{1, 2, 3})))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("must be a PNG or JPEG");
    }

    @Test
    void aPngLogoIsStoredWithItsType() {
        Header header = new Header();
        when(headerRepository.findById(1L)).thenReturn(Optional.of(header));
        when(headerRepository.save(any(Header.class))).thenAnswer(inv -> inv.getArgument(0));

        service().saveLogo(1L, new HeaderLogoRequest("IMAGE/PNG", new byte[]{1, 2, 3}));

        assertThat(header.getLogo()).containsExactly(1, 2, 3);
        assertThat(header.getLogoContentType()).isEqualTo("image/png");
    }

    @Test
    void anEmptyLogoIsRefused() {
        when(headerRepository.findById(1L)).thenReturn(Optional.of(new Header()));

        assertThatThrownBy(() -> service().saveLogo(1L, new HeaderLogoRequest("image/png", new byte[0])))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("empty");
    }

    @Test
    void anOversizedLogoIsRefusedWithItsSize() {
        when(headerRepository.findById(1L)).thenReturn(Optional.of(new Header()));

        assertThatThrownBy(() -> service().saveLogo(1L,
                new HeaderLogoRequest("image/png", new byte[3 * 1024 * 1024])))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("3072 KB")
                .hasMessageContaining("2048 KB limit");
    }

    /** A company without a logo is a normal state, not an error to hide. */
    @Test
    void askingForAMissingLogoSaysSo() {
        when(headerRepository.findById(1L)).thenReturn(Optional.of(new Header()));

        assertThatThrownBy(() -> service().findLogo(1L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("has no logo");
    }

    @Test
    void theNewReportFieldsAreStored() {
        HeaderRequest request = new HeaderRequest("SmartBatch Solutions", "Kharadi Plant",
                "Kharadi, Pune", "Pune", "411014", "9876543210", "info@smartbatch.example",
                "27ABCDE1234F1Z5", "R. Patil", 30, 20, new java.math.BigDecimal("1.50"), HeaderStatus.ACTIVE);
        when(headerRepository.save(any(Header.class))).thenAnswer(inv -> inv.getArgument(0));

        HeaderResponse response = service().create(request);

        assertThat(response.city()).isEqualTo("Pune");
        assertThat(response.pinCode()).isEqualTo("411014");
        assertThat(response.supervisorName()).isEqualTo("R. Patil");
        assertThat(response.mixTimeSeconds()).isEqualTo(30);
        assertThat(response.dischargeTimeSeconds()).isEqualTo(20);
        assertThat(response.hasLogo()).isFalse();
    }
}
