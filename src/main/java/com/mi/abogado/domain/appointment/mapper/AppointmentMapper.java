package com.mi.abogado.domain.appointment.mapper;

import com.mi.abogado.domain.appointment.dto.AppointmentResponse;
import com.mi.abogado.domain.appointment.entity.Appointment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper
public interface AppointmentMapper {

    @Mapping(target = "clientId", source = "client.id")
    @Mapping(target = "clientName", source = "client.name")
    @Mapping(target = "caseId", source = "legalCase.id")
    @Mapping(target = "caseNumber", source = "legalCase.caseNumber")
    @Mapping(target = "lawyerId", source = "lawyer.id")
    @Mapping(target = "lawyerName", source = "lawyer.user.fullName")
    AppointmentResponse toResponse(Appointment appointment);

    List<AppointmentResponse> toResponses(List<Appointment> appointments);
}
