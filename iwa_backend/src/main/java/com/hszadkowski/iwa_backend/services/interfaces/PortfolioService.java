package com.hszadkowski.iwa_backend.services.interfaces;

import com.hszadkowski.iwa_backend.dto.portfolio.CreatePortfolioItemDto;
import com.hszadkowski.iwa_backend.dto.portfolio.PortfolioItemResponseDto;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface PortfolioService {
    PortfolioItemResponseDto createPortfolioItem(CreatePortfolioItemDto dto, List<MultipartFile> files);
    List<PortfolioItemResponseDto> getAllPortfolioItems();
    PortfolioItemResponseDto updatePortfolioItem(Integer id, CreatePortfolioItemDto dto, List<MultipartFile> files);
    void deletePortfolioItem(Integer id);
}