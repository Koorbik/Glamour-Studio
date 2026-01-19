package com.hszadkowski.iwa_backend.controllers;

import com.hszadkowski.iwa_backend.dto.portfolio.CreatePortfolioItemDto;
import com.hszadkowski.iwa_backend.dto.portfolio.PortfolioItemResponseDto;
import com.hszadkowski.iwa_backend.services.interfaces.PortfolioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/portfolio")
@RequiredArgsConstructor
public class PortfolioController {

    private final PortfolioService portfolioService;

    @GetMapping
    public ResponseEntity<List<PortfolioItemResponseDto>> getAllItems() {
        return ResponseEntity.ok(portfolioService.getAllPortfolioItems());
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PortfolioItemResponseDto> createItem(
            @RequestPart("data") @Valid CreatePortfolioItemDto dto,
            @RequestPart(value = "files") List<MultipartFile> files) {
        return ResponseEntity.ok(portfolioService.createPortfolioItem(dto, files));
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PortfolioItemResponseDto> updateItem(
            @PathVariable Integer id,
            @RequestPart("data") @Valid CreatePortfolioItemDto dto,
            @RequestPart(value = "files", required = false) List<MultipartFile> files) {
        return ResponseEntity.ok(portfolioService.updatePortfolioItem(id, dto, files));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteItem(@PathVariable Integer id) {
        portfolioService.deletePortfolioItem(id);
        return ResponseEntity.noContent().build();
    }
}