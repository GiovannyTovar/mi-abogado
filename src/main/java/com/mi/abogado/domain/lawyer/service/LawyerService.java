package com.mi.abogado.domain.lawyer.service;

import com.mi.abogado.domain.lawyer.dto.CreateLawyerRequest;
import com.mi.abogado.domain.lawyer.dto.LawyerResponse;
import com.mi.abogado.domain.lawyer.dto.LawyerSummary;
import com.mi.abogado.domain.lawyer.dto.UpdateLawyerRequest;
import com.mi.abogado.domain.lawyer.entity.Lawyer;
import com.mi.abogado.domain.lawyer.entity.PracticeArea;
import com.mi.abogado.domain.lawyer.mapper.LawyerMapper;
import com.mi.abogado.domain.lawyer.repository.LawyerRepository;
import com.mi.abogado.domain.lawyer.repository.PracticeAreaRepository;
import com.mi.abogado.domain.subscription.service.SubscriptionService;
import com.mi.abogado.domain.tenant.entity.Tenant;
import com.mi.abogado.domain.tenant.repository.TenantRepository;
import com.mi.abogado.domain.user.entity.Role;
import com.mi.abogado.domain.user.entity.User;
import com.mi.abogado.domain.user.repository.UserRepository;
import com.mi.abogado.shared.error.BusinessException;
import com.mi.abogado.shared.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.UUID;

/**
 * Casos de uso del perfil de abogado. Sin interfaz aparte: hay una sola
 * implementacion y no hay motivo para inventar una segunda.
 */
@Service
@RequiredArgsConstructor
public class LawyerService {

    private final LawyerRepository lawyerRepository;
    private final PracticeAreaRepository practiceAreaRepository;
    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final SubscriptionService subscriptionService;
    private final LawyerMapper lawyerMapper;

    @Transactional(readOnly = true)
    public Page<LawyerSummary> search(String city, String search, Pageable pageable) {
        return lawyerRepository.search(city, search, pageable);
    }

    @Transactional(readOnly = true)
    public LawyerResponse findById(UUID id) {
        return lawyerRepository.findWithDetailsById(id)
                .map(lawyerMapper::toResponse)
                .orElseThrow(() -> BusinessException.notFound("Abogado"));
    }

    /**
     * Alta: invita al usuario (queda PENDING hasta su primer login con Google)
     * y crea su perfil profesional.
     */
    @Transactional
    public LawyerResponse create(CreateLawyerRequest request) {
        UUID tenantId = TenantContext.require();

        if (lawyerRepository.existsByLicenseNumber(request.licenseNumber())) {
            throw BusinessException.conflict("Ya existe un abogado con esa tarjeta profesional");
        }
        if (userRepository.existsByTenant_IdAndEmailIgnoreCase(tenantId, request.email())) {
            throw BusinessException.conflict("Ya hay un usuario con ese correo en la firma");
        }
        subscriptionService.ensureCanAddMember(tenantId);

        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> BusinessException.notFound("Firma"));

        User invited = new User(tenant, request.email(), request.fullName(), Role.LAWYER);
        invited.setPhone(request.phone());
        userRepository.save(invited);

        Lawyer lawyer = new Lawyer(invited, request.licenseNumber());
        lawyer.setBio(request.bio());
        lawyer.setCity(request.city());
        lawyer.setHourlyRate(request.hourlyRate());
        if (request.yearsOfExperience() != null) {
            lawyer.setYearsOfExperience(request.yearsOfExperience());
        }
        lawyer.replacePracticeAreas(resolvePracticeAreas(request.practiceAreaIds()));

        return lawyerMapper.toResponse(lawyerRepository.save(lawyer));
    }

    @Transactional
    public LawyerResponse update(UUID id, UpdateLawyerRequest request) {
        Lawyer lawyer = lawyerRepository.findWithDetailsById(id)
                .orElseThrow(() -> BusinessException.notFound("Abogado"));

        // Actualizacion parcial explicita: null significa "no tocar".
        if (request.licenseNumber() != null) {
            lawyer.setLicenseNumber(request.licenseNumber());
        }
        if (request.bio() != null) {
            lawyer.setBio(request.bio());
        }
        if (request.yearsOfExperience() != null) {
            lawyer.setYearsOfExperience(request.yearsOfExperience());
        }
        if (request.city() != null) {
            lawyer.setCity(request.city());
        }
        if (request.hourlyRate() != null) {
            lawyer.setHourlyRate(request.hourlyRate());
        }
        if (request.practiceAreaIds() != null) {
            lawyer.replacePracticeAreas(resolvePracticeAreas(request.practiceAreaIds()));
        }

        return lawyerMapper.toResponse(lawyer);
    }

    private Set<PracticeArea> resolvePracticeAreas(Set<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            return Set.of();
        }
        Set<PracticeArea> areas = practiceAreaRepository.findByIdIn(ids);
        if (areas.size() != ids.size()) {
            throw BusinessException.conflict("Alguna especialidad indicada no existe");
        }
        return areas;
    }
}
