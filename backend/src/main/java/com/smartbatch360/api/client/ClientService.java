package com.smartbatch360.api.client;

import com.smartbatch360.api.client.dto.ClientRequest;
import com.smartbatch360.api.client.dto.ClientResponse;
import com.smartbatch360.api.common.ConflictException;
import com.smartbatch360.api.common.NotFoundException;
import com.smartbatch360.api.site.SiteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class ClientService {

    private final ClientRepository clientRepository;
    private final SiteRepository siteRepository;

    public ClientService(ClientRepository clientRepository, SiteRepository siteRepository) {
        this.clientRepository = clientRepository;
        this.siteRepository = siteRepository;
    }

    @Transactional(readOnly = true)
    public List<ClientResponse> findAll() {
        return clientRepository.findAll().stream()
                .map(ClientResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public ClientResponse findById(Long id) {
        return ClientResponse.from(getOrThrow(id));
    }

    public ClientResponse create(ClientRequest request) {
        Client client = new Client();
        applyRequest(client, request);
        return ClientResponse.from(clientRepository.save(client));
    }

    public ClientResponse update(Long id, ClientRequest request) {
        Client client = getOrThrow(id);
        applyRequest(client, request);
        return ClientResponse.from(clientRepository.save(client));
    }

    public void delete(Long id) {
        Client client = getOrThrow(id);
        if (siteRepository.existsByClientId(id)) {
            throw new ConflictException("Client '" + client.getName()
                    + "' has one or more sites and cannot be deleted. Remove its sites first.");
        }
        clientRepository.delete(client);
    }

    Client getOrThrow(Long id) {
        return clientRepository.findById(id)
                .orElseThrow(() -> NotFoundException.forId("Client", id));
    }

    private void applyRequest(Client client, ClientRequest request) {
        client.setName(request.name().trim());
        client.setContactPerson(request.contactPerson().trim());
        client.setPhone(request.phone().trim());
        client.setAddress(blankToNull(request.address()));
        client.setStatus(request.status());
    }

    private String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
