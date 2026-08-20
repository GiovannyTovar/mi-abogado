package com.miabogado.domain.document.mapper;

import com.miabogado.domain.document.dto.DocumentResponse;
import com.miabogado.domain.document.entity.Document;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper
public interface DocumentMapper {

    @Mapping(target = "caseId", source = "legalCase.id")
    @Mapping(target = "uploadedByName", source = "uploadedBy.fullName")
    DocumentResponse toResponse(Document document);

    List<DocumentResponse> toResponses(List<Document> documents);
}
