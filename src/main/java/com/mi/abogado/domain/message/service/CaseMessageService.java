package com.mi.abogado.domain.message.service;

import com.mi.abogado.domain.legalcase.entity.LegalCase;
import com.mi.abogado.domain.legalcase.repository.LegalCaseRepository;
import com.mi.abogado.domain.message.dto.MessageResponse;
import com.mi.abogado.domain.message.entity.CaseMessage;
import com.mi.abogado.domain.message.repository.CaseMessageRepository;
import com.mi.abogado.domain.user.entity.User;
import com.mi.abogado.domain.user.repository.UserRepository;
import com.mi.abogado.shared.error.BusinessException;
import com.mi.abogado.shared.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * Hilo de mensajes de un expediente. El mismo servicio atiende a la firma y al
 * portal: quien puede escribir en que caso lo decide el llamador
 * ({@code ClientPortalService} para el cliente, {@code @PreAuthorize} para la firma).
 */
@Service
@RequiredArgsConstructor
public class CaseMessageService {

    private final CaseMessageRepository messageRepository;
    private final LegalCaseRepository legalCaseRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public Page<MessageResponse> findThread(UUID caseId, Pageable pageable) {
        return messageRepository.findThread(caseId, pageable);
    }

    @Transactional
    public MessageResponse send(UUID caseId, String body) {
        LegalCase legalCase = legalCaseRepository.findById(caseId)
                .orElseThrow(() -> BusinessException.notFound("Expediente"));

        User sender = currentUser();
        CaseMessage message = messageRepository.save(new CaseMessage(legalCase, sender, body));

        return new MessageResponse(
                message.getId(), message.getBody(), sender.getId(), sender.getFullName(),
                sender.getRole(), null, message.getCreatedAt());
    }

    /** Marca leidos los mensajes que escribio el otro lado. */
    @Transactional
    public int markRead(UUID caseId) {
        return messageRepository.markThreadRead(caseId, CurrentUser.require().userId(), Instant.now());
    }

    @Transactional(readOnly = true)
    public long countUnread(UUID caseId, UUID readerId) {
        return messageRepository.countUnread(caseId, readerId);
    }

    private User currentUser() {
        return userRepository.findById(CurrentUser.require().userId())
                .orElseThrow(() -> BusinessException.notFound("Usuario"));
    }
}
