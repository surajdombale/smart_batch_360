package com.smartbatch360.api.client;

import com.smartbatch360.api.batch.BatchRepository;
import com.smartbatch360.api.client.dto.ClientRequest;
import com.smartbatch360.api.client.dto.ClientResponse;
import com.smartbatch360.api.common.ConflictException;
import com.smartbatch360.api.common.NotFoundException;
import com.smartbatch360.api.site.SiteRepository;
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
class ClientServiceTest {

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private SiteRepository siteRepository;

    @Mock
    private BatchRepository batchRepository;

    private ClientService service() {
        return new ClientService(clientRepository, siteRepository, batchRepository);
    }

    @Test
    void createsClientFromRequest() {
        ClientRequest request = new ClientRequest("Larsen & Toubro", "Amit Sharma", "9822334455", "Hinjewadi, Pune", ClientStatus.ACTIVE);
        when(clientRepository.save(any(Client.class))).thenAnswer(inv -> inv.getArgument(0));

        ClientResponse response = service().create(request);

        assertThat(response.name()).isEqualTo("Larsen & Toubro");
        assertThat(response.address()).isEqualTo("Hinjewadi, Pune");
        assertThat(response.status()).isEqualTo(ClientStatus.ACTIVE);
        verify(clientRepository).save(any(Client.class));
    }

    @Test
    void blankAddressIsStoredAsNull() {
        ClientRequest request = new ClientRequest("Larsen & Toubro", "Amit Sharma", "9822334455", "   ", ClientStatus.ACTIVE);
        when(clientRepository.save(any(Client.class))).thenAnswer(inv -> inv.getArgument(0));

        ClientResponse response = service().create(request);

        assertThat(response.address()).isNull();
    }

    @Test
    void findByIdThrowsNotFoundWhenMissing() {
        when(clientRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().findById(99L))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void deleteRejectedWhenClientHasSites() {
        Client client = new Client();
        client.setName("Tata Projects");
        when(clientRepository.findById(1L)).thenReturn(Optional.of(client));
        when(siteRepository.existsByClientId(1L)).thenReturn(true);

        assertThatThrownBy(() -> service().delete(1L))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("sites");

        verify(clientRepository, never()).delete(any());
    }

    @Test
    void deleteRejectedWhenClientHasBatches() {
        Client client = new Client();
        client.setName("Tata Projects");
        when(clientRepository.findById(3L)).thenReturn(Optional.of(client));
        when(siteRepository.existsByClientId(3L)).thenReturn(false);
        when(batchRepository.existsByClientId(3L)).thenReturn(true);

        assertThatThrownBy(() -> service().delete(3L))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("batches");

        verify(clientRepository, never()).delete(any());
    }

    @Test
    void deleteSucceedsWhenNoSites() {
        Client client = new Client();
        client.setName("Afcons Infrastructure");
        when(clientRepository.findById(2L)).thenReturn(Optional.of(client));
        when(siteRepository.existsByClientId(2L)).thenReturn(false);

        service().delete(2L);

        verify(clientRepository).delete(client);
    }
}
