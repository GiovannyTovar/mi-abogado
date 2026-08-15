package com.mi.abogado.domain.client.service;

import com.mi.abogado.domain.client.dto.ClientResponse;
import com.mi.abogado.domain.client.dto.ClientSummary;
import com.mi.abogado.domain.client.dto.CreateClientRequest;
import com.mi.abogado.domain.client.dto.UpdateClientRequest;
import com.mi.abogado.domain.client.entity.Client;
import com.mi.abogado.domain.client.entity.ClientStatus;
import com.mi.abogado.domain.auth.service.RefreshTokenService;
import com.mi.abogado.domain.client.mapper.ClientMapper;
import com.mi.abogado.domain.client.repository.ClientRepository;
import com.mi.abogado.domain.tenant.entity.Tenant;
import com.mi.abogado.domain.tenant.repository.TenantRepository;
import com.mi.abogado.domain.user.dto.UserResponse;
import com.mi.abogado.domain.user.entity.Role;
import com.mi.abogado.domain.user.entity.User;
import com.mi.abogado.domain.user.mapper.UserMapper;
import com.mi.abogado.domain.user.repository.UserRepository;
import com.mi.abogado.shared.error.BusinessException;
import com.mi.abogado.shared.tenant.TenantContext;
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
    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final RefreshTokenService refreshTokenService;
    private final ClientMapper clientMapper;
    private final UserMapper userMapper;

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
     * Da acceso al portal: invita al cliente como usuario con rol CLIENT y lo
     * enlaza con su ficha. Queda PENDING hasta que entre con Google.
     * <p>
     * Los clientes no cuentan para el limite de miembros del plan: son los clientes
     * de la firma, no su plantilla.
     */
    @Transactional
    public UserResponse grantPortalAccess(UUID clientId) {
        Client client = requireClient(clientId);

        if (client.getUser() != null) {
            throw BusinessException.conflict("El cliente ya tiene acceso al portal");
        }
        if (client.getEmail() == null || client.getEmail().isBlank()) {
            throw BusinessException.conflict("El cliente necesita un correo para acceder al portal");
        }

        UUID tenantId = TenantContext.require();
        if (userRepository.existsByTenant_IdAndEmailIgnoreCase(tenantId, client.getEmail())) {
            throw BusinessException.conflict("Ya hay un usuario con ese correo en la firma");
        }

        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> BusinessException.notFound("Firma"));

        User portalUser = new User(tenant, client.getEmail(), client.getName(), Role.CLIENT);
        portalUser.setPhone(client.getPhone());
        userRepository.save(portalUser);
        client.setUser(portalUser);

        return userMapper.toResponse(portalUser);
    }

    /** Revoca el acceso sin borrar la ficha ni el historial del cliente. */
    @Transactional
    public void revokePortalAccess(UUID clientId) {
        Client client = requireClient(clientId);
        User portalUser = client.getUser();

        if (portalUser == null) {
            throw BusinessException.conflict("El cliente no tiene acceso al portal");
        }

        portalUser.disable();
        refreshTokenService.revokeAllSessions(portalUser);
        client.setUser(null);
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
