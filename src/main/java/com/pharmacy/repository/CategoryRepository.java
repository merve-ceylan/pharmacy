package com.pharmacy.repository;

import com.pharmacy.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    // Find by slug (for URL)
    Optional<Category> findBySlug(String slug);

    // Find all root categories (no parent)
    List<Category> findByParentIsNullAndActiveTrue();

    // Find child categories
    List<Category> findByParentIdAndActiveTrue(Long parentId);

    // Find all active categories
    List<Category> findByActiveTrue();

    // Find all active categories ordered
    List<Category> findByActiveTrueOrderByDisplayOrderAsc();

    // Find root categories ordered
    List<Category> findByParentIsNullAndActiveTrueOrderByDisplayOrderAsc();

    // Check if slug exists
    boolean existsBySlug(String slug);

    // Find by name
    Optional<Category> findByName(String name);
}

