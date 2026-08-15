package com.mi.abogado.domain.appointment.service;

import com.mi.abogado.domain.appointment.dto.AppointmentResponse;
import com.mi.abogado.domain.appointment.dto.CreateAppointmentRequest;
import com.mi.abogado.domain.appointment.dto.UpdateAppointmentRequest;
import com.mi.abogado.domain.appointment.entity.Appointment;
import com.mi.abogado.domain.appointment.mapper.AppointmentMapper;
import com.mi.abogado.domain.appointment.repository.AppointmentRepository;
import com.mi.abogado.domain.lawyer.entity.Lawyer;
import com.mi.abogado.domain.lawyer.repository.LawyerRepository;
import com.mi.abogado.domain.legalcase.entity.LegalCase;
import com.mi.abogado.domain.legalcase.repository.LegalCaseRepository;
import com.mi.abogado.domain.client.service.ClientService;
import com.mi.abogado.domain.user.entity.User;
import com.mi.abogado.domain.user.repository.UserRepository;
import com.mi.abogado.shared.error.BusinessException;
import com.mi.abogado.shared.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Agenda de citas de la firma.
 */
@Service
@RequiredArgsConstructor
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final LawyerRepository lawyerRepository;
    private final LegalCaseRepository legalCaseRepository;
    private final ClientService clientService;
    private final UserRepository userRepository;
    private final AppointmentMapper appointmentMapper;

    @Transactional(readOnly = true)
    public List<AppointmentResponse> agenda(Instant from, Instant to, UUID lawyerId, UUID clientId) {
        if (!to.isAfter(from)) {
            throw BusinessException.conflict("El rango de fechas esta invertido");
        }
        return appointmentMapper.toResponses(appointmentRepository.findAgenda(from, to, lawyerId, clientId));
    }

    @Transactional(readOnly = true)
    public AppointmentResponse findById(UUID id) {
        return appointmentMapper.toResponse(requireAppointment(id));
    }

    @Transactional
    public AppointmentResponse create(CreateAppointmentRequest request) {
        validateRange(request.startsAt(), request.endsAt());

        Appointment appointment = new Appointment(
                clientService.requireClient(request.clientId()),
                request.title(),
                request.startsAt(),
                request.endsAt());

        appointment.setDescription(request.description());
        appointment.setMode(request.mode());
        appointment.setLocation(request.location());
        appointment.setLegalCase(resolveCase(request.caseId()));
        appointment.setCreatedBy(currentUser());

        Lawyer lawyer = resolveLawyer(request.lawyerId());
        if (lawyer != null) {
            ensureNoOverlap(lawyer.getId(), request.startsAt(), request.endsAt(), null);
            appointment.setLawyer(lawyer);
        }

        return appointmentMapper.toResponse(appointmentRepository.save(appointment));
    }

    @Transactional
    public AppointmentResponse update(UUID id, UpdateAppointmentRequest request) {
        Appointment appointment = requireAppointment(id);
        if (!appointment.isOpen()) {
            throw BusinessException.conflict("La cita ya esta cerrada");
        }

        Instant startsAt = request.startsAt() != null ? request.startsAt() : appointment.getStartsAt();
        Instant endsAt = request.endsAt() != null ? request.endsAt() : appointment.getEndsAt();
        validateRange(startsAt, endsAt);

        if (request.title() != null) {
            appointment.setTitle(request.title());
        }
        if (request.description() != null) {
            appointment.setDescription(request.description());
        }
        if (request.mode() != null) {
            appointment.setMode(request.mode());
        }
        if (request.location() != null) {
            appointment.setLocation(request.location());
        }
        if (request.lawyerId() != null) {
            appointment.setLawyer(resolveLawyer(request.lawyerId()));
        }
        appointment.setStartsAt(startsAt);
        appointment.setEndsAt(endsAt);

        if (appointment.getLawyer() != null) {
            ensureNoOverlap(appointment.getLawyer().getId(), startsAt, endsAt, appointment.getId());
        }
        return appointmentMapper.toResponse(appointment);
    }

    /** La confirma el cliente desde su portal, o la firma por telefono. */
    @Transactional
    public AppointmentResponse confirm(UUID id) {
        Appointment appointment = requireAppointment(id);
        if (!appointment.isOpen()) {
            throw BusinessException.conflict("La cita ya esta cerrada");
        }
        appointment.confirm();
        return appointmentMapper.toResponse(appointment);
    }

    @Transactional
    public AppointmentResponse cancel(UUID id, String reason) {
        Appointment appointment = requireAppointment(id);
        if (!appointment.isOpen()) {
            throw BusinessException.conflict("La cita ya esta cerrada");
        }
        appointment.cancel(reason);
        return appointmentMapper.toResponse(appointment);
    }

    @Transactional
    public AppointmentResponse complete(UUID id) {
        Appointment appointment = requireAppointment(id);
        if (!appointment.isOpen()) {
            throw BusinessException.conflict("La cita ya esta cerrada");
        }
        appointment.complete();
        return appointmentMapper.toResponse(appointment);
    }

    /** Uso del portal, que ya valido que la cita sea de ese cliente. */
    @Transactional(readOnly = true)
    public Appointment requireAppointment(UUID id) {
        return appointmentRepository.findWithDetailsById(id)
                .orElseThrow(() -> BusinessException.notFound("Cita"));
    }

    @Transactional(readOnly = true)
    public List<AppointmentResponse> upcomingForClient(UUID clientId) {
        return appointmentMapper.toResponses(
                appointmentRepository.findUpcomingForClient(clientId, Instant.now()));
    }

    private void validateRange(Instant startsAt, Instant endsAt) {
        if (!endsAt.isAfter(startsAt)) {
            throw BusinessException.conflict("La cita no puede terminar antes de empezar");
        }
    }

    /**
     * Una agenda que permite dos citas a la misma hora no es una agenda.
     */
    private void ensureNoOverlap(UUID lawyerId, Instant startsAt, Instant endsAt, UUID excludeId) {
        if (appointmentRepository.countOverlapping(lawyerId, startsAt, endsAt, excludeId) > 0) {
            throw BusinessException.conflict("El abogado ya tiene una cita en ese horario");
        }
    }

    private Lawyer resolveLawyer(UUID lawyerId) {
        if (lawyerId == null) {
            return null;
        }
        return lawyerRepository.findWithDetailsById(lawyerId)
                .orElseThrow(() -> BusinessException.notFound("Abogado"));
    }

    private LegalCase resolveCase(UUID caseId) {
        if (caseId == null) {
            return null;
        }
        return legalCaseRepository.findById(caseId)
                .orElseThrow(() -> BusinessException.notFound("Expediente"));
    }

    private User currentUser() {
        return CurrentUser.find()
                .flatMap(principal -> userRepository.findById(principal.userId()))
                .orElse(null);
    }
}
