package com.smartbatch360.api.site;

import com.smartbatch360.api.client.Client;
import com.smartbatch360.api.client.ClientRepository;
import com.smartbatch360.api.client.ClientStatus;
import com.smartbatch360.api.common.NotFoundException;
import com.smartbatch360.api.site.dto.SiteRequest;
import com.smartbatch360.api.site.dto.SiteResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SiteServiceTest {

    @Mock
    private SiteRepository siteRepository;

    @Mock
    private ClientRepository clientRepository;

    private SiteService service() {
        return new SiteService(siteRepository, clientRepository);
    }

    private Client clientWithId(long id, String name) throws Exception {
        Client c = new Client();
        c.setName(name);
        c.setContactPerson("Contact");
        c.setPhone("9000000000");
        c.setStatus(ClientStatus.ACTIVE);
        Field idField = Client.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(c, id);
        return c;
    }

    @Test
    void createRejectsUnknownClient() {
        when(clientRepository.findById(5L)).thenReturn(Optional.empty());
        SiteRequest request = new SiteRequest("Kharadi", 5L, "Pune", SiteStatus.ACTIVE);

        assertThatThrownBy(() -> service().create(request))
                .isInstanceOf(NotFoundException.class);

        verify(siteRepository, never()).save(any());
    }

    @Test
    void createsSiteLinkedToClient() throws Exception {
        Client client = clientWithId(5L, "SmartBatch Solutions");
        when(clientRepository.findById(5L)).thenReturn(Optional.of(client));
        when(siteRepository.save(any(Site.class))).thenAnswer(inv -> inv.getArgument(0));

        SiteRequest request = new SiteRequest("Kharadi", 5L, "Pune", SiteStatus.ACTIVE);
        SiteResponse response = service().create(request);

        assertThat(response.name()).isEqualTo("Kharadi");
        assertThat(response.clientName()).isEqualTo("SmartBatch Solutions");
    }
}
