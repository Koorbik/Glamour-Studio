package com.hszadkowski.iwa_backend.repos;

import com.hszadkowski.iwa_backend.models.PortfolioItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PortfolioRepository extends JpaRepository<PortfolioItem, Integer> {
    List<PortfolioItem> findAllByCategory(String category);
}
