package com.mi.abogado.domain.user.controller;

import com.mi.abogado.domain.user.dto.InviteMemberRequest;
import com.mi.abogado.domain.user.dto.MemberSummary;
import com.mi.abogado.domain.user.dto.UserResponse;
import com.mi.abogado.domain.user.entity.Role;
import com.mi.abogado.domain.user.entity.UserStatus;
import com.mi.abogado.domain.user.service.MemberService;
import com.mi.abogado.shared.tenant.TenantContext;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/members")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    @GetMapping
    @PreAuthorize("hasAnyRole('FIRM_OWNER', 'LAWYER', 'ASSISTANT')")
    public Page<MemberSummary> findMembers(@RequestParam(required = false) Role role,
                                           @PageableDefault(size = 20, sort = "fullName", direction = Sort.Direction.ASC)
                                           Pageable pageable) {
        return memberService.findMembers(TenantContext.require(), role, pageable);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('FIRM_OWNER')")
    public UserResponse invite(@Valid @RequestBody InviteMemberRequest request) {
        return memberService.invite(TenantContext.require(), request);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('FIRM_OWNER')")
    public UserResponse changeStatus(@PathVariable UUID id, @RequestParam UserStatus status) {
        return memberService.changeStatus(TenantContext.require(), id, status);
    }
}
