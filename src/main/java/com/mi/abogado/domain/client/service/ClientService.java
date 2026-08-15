package com.mi.abogado.domain.client.service;

import com.mi.abogado.domain.client.dto.ClientResponse;
import com.mi.abogado.domain.client.dto.ClientSummary;
import com.mi.abogado.domain.client.dto.CreateClientRequest;
import com.mi.abogado.domain.client.dto.UpdateClientRequest;
import com.mi.abogado.domain.client.entity.Client;
import com.mi.abogado.domain.client.entity.ClientStatus;
import com.mi.abogado.domain.client.mapper.ClientMapper;
import com.mi.abogado.domain.client.repository.ClientRepository;
import com.mi.abogado.shared.error.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * CRM de clientes de la firma.
 */
@Service
@RequiredArgsConstructor
public class ClientService {

    private final ClientRepository clientRepository;
    private final ClientMapper clientMapper;

    @Transactional(readOnly = true)
    public Page<ClientSummary> search(ClientStatus status, String search, Pageable pageable) {
        return clientRepository.search(status, search, pageable);
    }

    @Transactional(readOnly = true)
    public ClientResponse findById(UUID id) {
        return clientMapper.toResponse(requireClient(id));
    }

    @Transactional
    public ClientResponse create(CreateClientRequest request) {
        return clientMapper.toResponse(createClient(request));
    }

    /**
     * Variante que devuelve la entidad, para quien necesita seguir trabajando con
     * ella (convertir un lead crea el cliente y acto seguido le abre un caso).
     */
    @Transactional
    public Client createClient(CreateClientRequest request) {
        if (clientRepository.existsByDocumentTypeAndDocumentNumber(request.documentType(), request.documentNumber())) {
            throw BusinessException.conflict("Ya existe un cliente con ese documento");
        }

        Client client = new Client(
                request.clientType(), request.documentType(), request.documentNumber(), request.name());
        client.setEmail(request.email());
        client.setPhone(request.phone());
        client.setAddress(request.address());
        client.setCity(request.city());
        client.setNotes(request.notes());

        return clientRepository.save(client);
    }

    @Transactional
    public ClientResponse update(UUID id, UpdateClientRequest request) {
        Client client = requireClient(id);

        if (request.name() != null) {
            client.setName(request.name());
        }
        if (request.email() != null) {
            client.setEmail(request.email());
        }
        if (request.phone() != null) {
            client.setPhone(request.phone());
        }
        if (request.address() != null) {
            client.setAddress(request.address());
        }
        if (request.city() != null) {
            client.setCity(request.city());
        }
        if (request.notes() != null) {
            client.setNotes(request.notes());
        }
        if (request.status() != null) {
            client.setStatus(request.status());
        }
        return clientMapper.toResponse(client);
    }

    /**
     * Uso interno de otros dominios (crear un caso, convertir un lead) que
     * necesitan la entidad, no el DTO.
     */
    @Transactional(readOnly = true)
    public Client requireClient(UUID id) {
        return clientRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("Cliente"));
    }
}
