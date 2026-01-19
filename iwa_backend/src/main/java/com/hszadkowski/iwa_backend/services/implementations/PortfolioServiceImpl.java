package com.hszadkowski.iwa_backend.services.implementations;

import com.hszadkowski.iwa_backend.dto.portfolio.CreatePortfolioItemDto;
import com.hszadkowski.iwa_backend.dto.portfolio.PortfolioItemResponseDto;
import com.hszadkowski.iwa_backend.models.PortfolioItem;
import com.hszadkowski.iwa_backend.repos.PortfolioRepository;
import com.hszadkowski.iwa_backend.services.FileStorageService;
import com.hszadkowski.iwa_backend.services.interfaces.PortfolioService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PortfolioServiceImpl implements PortfolioService {

    private final PortfolioRepository portfolioItemRepository;
    private final FileStorageService fileStorageService;

    @Override
    @Transactional
    public PortfolioItemResponseDto createPortfolioItem(CreatePortfolioItemDto dto, List<MultipartFile> files) {
        PortfolioItem item = PortfolioItem.builder()
                .description(dto.getDescription())
                .category(dto.getCategory())
                .imageUrls(new ArrayList<>())
                .build();

        // Process files
        if (files != null && !files.isEmpty()) {
            for (MultipartFile file : files) {
                if (!file.isEmpty()) {
                    String fileUrl = saveFileAndGetUrl(file);
                    item.getImageUrls().add(fileUrl);
                }
            }
        } else {
            throw new RuntimeException("At least one image is required.");
        }

        PortfolioItem savedItem = portfolioItemRepository.save(item);
        return mapToDto(savedItem);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PortfolioItemResponseDto> getAllPortfolioItems() {
        return portfolioItemRepository.findAll()
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public PortfolioItemResponseDto updatePortfolioItem(Integer id, CreatePortfolioItemDto dto, List<MultipartFile> files) {
        PortfolioItem item = portfolioItemRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Portfolio item not found"));

        item.setDescription(dto.getDescription());
        item.setCategory(dto.getCategory());

        // Append new files if provided
        if (files != null && !files.isEmpty()) {
            for (MultipartFile file : files) {
                if (!file.isEmpty()) {
                    String fileUrl = saveFileAndGetUrl(file);
                    item.getImageUrls().add(fileUrl);
                }
            }
        }

        PortfolioItem updatedItem = portfolioItemRepository.save(item);
        return mapToDto(updatedItem);
    }

    @Override
    @Transactional
    public void deletePortfolioItem(Integer id) {
        PortfolioItem item = portfolioItemRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Portfolio item not found"));

        // Delete all associated files from disk
        if (item.getImageUrls() != null) {
            for (String url : item.getImageUrls()) {
                fileStorageService.deleteFile(url);
            }
        }

        portfolioItemRepository.delete(item);
    }

    private String saveFileAndGetUrl(MultipartFile file) {
        if (file.getContentType() == null || !file.getContentType().startsWith("image/")) {
            throw new RuntimeException("Only image files are allowed.");
        }
        String fileName = fileStorageService.storeFile(file);
        return ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/uploads/")
                .path(fileName)
                .toUriString();
    }

    private PortfolioItemResponseDto mapToDto(PortfolioItem item) {
        return PortfolioItemResponseDto.builder()
                .id(item.getId())
                .imageUrls(item.getImageUrls())
                .description(item.getDescription())
                .category(item.getCategory())
                .createdAt(item.getCreatedAt())
                .build();
    }
}