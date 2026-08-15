package com.mi.abogado.domain.message.controller;

import com.mi.abogado.domain.message.dto.MessageResponse;
import com.mi.abogado.domain.message.dto.SendMessageRequest;
import com.mi.abogado.domain.message.service.CaseMessageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** Lado firma del hilo. El lado cliente vive en {@code /api/v1/portal}. */
@RestController
@RequestMapping("/api/v1/cases/{caseId}/messages")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('FIRM_OWNER', 'LAWYER', 'ASSISTANT')")
public class CaseMessageController {

    private final CaseMessageService caseMessageService;

    @GetMapping
    public Page<MessageResponse> thread(@PathVariable UUID caseId,
                                        @PageableDefault(size = 30) Pageable pageable) {
        return caseMessageService.findThread(caseId, pageable);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MessageResponse send(@PathVariable UUID caseId, @Valid @RequestBody SendMessageRequest request) {
        return caseMessageService.send(caseId, request.body());
    }

    @PostMapping("/read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markRead(@PathVariable UUID caseId) {
        caseMessageService.markRead(caseId);
    }
}
