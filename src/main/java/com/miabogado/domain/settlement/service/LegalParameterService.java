package com.miabogado.domain.settlement.service;

import com.miabogado.domain.settlement.dto.LegalParameterResponse;
import com.miabogado.domain.settlement.dto.UpsertLegalParameterRequest;
import com.miabogado.domain.settlement.entity.LegalParameter;
import com.miabogado.domain.settlement.mapper.LegalParameterMapper;
import com.miabogado.domain.settlement.repository.LegalParameterRepository;
import com.miabogado.shared.error.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Las cifras que el Gobierno fija cada ano. Catalogo de plataforma: lo lee
 * cualquiera (la calculadora publica lo necesita sin sesion) y lo escribe solo
 * el super-admin.
 */
@Service
@RequiredArgsConstructor
public class LegalParameterService {

    private final LegalParameterRepository legalParameterRepository;
    private final LegalParameterMapper legalParameterMapper;

    @Transactional(readOnly = true)
    public List<LegalParameterResponse> findAll() {
        return legalParameterRepository.findAllByOrderByYearDesc().stream()
                .map(legalParameterMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public LegalParameterResponse findByYear(int year) {
        return legalParameterMapper.toResponse(requireExactYear(year));
    }

    /**
     * Parametros con los que liquidar una terminacion de ese ano. Si el ano no
     * esta cargado cae al ultimo anterior disponible: el decreto del salario
     * minimo sale a finales de diciembre y la calculadora no puede quedarse
     * muerta mientras tanto. Quien llama expone el ano usado en la respuesta.
     */
    @Transactional(readOnly = true)
    public LegalParameter resolveForYear(int year) {
        return legalParameterRepository.findFirstByYearLessThanEqualOrderByYearDesc(year)
                .orElseThrow(() -> BusinessException.conflict(
                        "No hay parametros legales cargados para " + year + " ni para ningun ano anterior"));
    }

    /**
     * Upsert por ano. En diciembre lo unico que importa es que las cifras del ano
     * entrante queden cargadas, exista o no ya la fila.
     */
    @Transactional
    public LegalParameterResponse upsert(UpsertLegalParameterRequest request) {
        LegalParameter parameter = legalParameterRepository.findByYear(request.year())
                .orElseGet(() -> new LegalParameter(request.year(), request.minimumWage(),
                        request.transportAllowance(), request.uvt()));

        parameter.setMinimumWage(request.minimumWage());
        parameter.setTransportAllowance(request.transportAllowance());
        parameter.setUvt(request.uvt());
        // Null deja el valor vigente: estos tres cambian de decada en decada.
        if (request.severanceInterestRate() != null) {
            parameter.setSeveranceInterestRate(request.severanceInterestRate());
        }
        if (request.transportAllowanceWageCap() != null) {
            parameter.setTransportAllowanceWageCap(request.transportAllowanceWageCap());
        }
        if (request.highSalaryThreshold() != null) {
            parameter.setHighSalaryThreshold(request.highSalaryThreshold());
        }

        return legalParameterMapper.toResponse(legalParameterRepository.save(parameter));
    }

    private LegalParameter requireExactYear(int year) {
        return legalParameterRepository.findByYear(year)
                .orElseThrow(() -> BusinessException.notFound("Parametros legales de " + year));
    }
}
