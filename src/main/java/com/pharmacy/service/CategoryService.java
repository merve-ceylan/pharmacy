package com.pharmacy.service;

import com.pharmacy.entity.Category;
import com.pharmacy.exception.ResourceNotFoundException;
import com.pharmacy.exception.DuplicateResourceException;
import com.pharmacy.repository.CategoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

@Service
@Transactional
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    public Category createCategory(Category category) {
        if (category.getSlug() == null || category.getSlug().isEmpty()) {
            category.setSlug(generateSlug(category.getName()));
        }

        if (categoryRepository.existsBySlug(category.getSlug())) {
            throw new DuplicateResourceException("Category", "slug", category.getSlug());
        }

        category.setActive(true);
        return categoryRepository.save(category);
    }

    public Category createSubcategory(Category category, Long parentId) {
        Category parent = getById(parentId);
        category.setParent(parent);
        return createCategory(category);
    }

    public Optional<Category> findById(Long id) {
        return categoryRepository.findById(id);
    }

    public Category getById(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", id));
    }

    public Optional<Category> findBySlug(String slug) {
        return categoryRepository.findBySlug(slug);
    }

    public Category getBySlug(String slug) {
        return categoryRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "slug", slug));
    }

    public List<Category> findRootCategories() {
        return categoryRepository.findByParentIsNullAndActiveTrueOrderByDisplayOrderAsc();
    }

    public List<Category> findSubcategories(Long parentId) {
        return categoryRepository.findByParentIdAndActiveTrue(parentId);
    }

    public List<Category> findAllActive() {
        return categoryRepository.findByActiveTrueOrderByDisplayOrderAsc();
    }

    public List<Category> findAll() {
        return categoryRepository.findAll();
    }

    public Category updateCategory(Category category) {
        return categoryRepository.save(category);
    }

    public Category updateDisplayOrder(Long categoryId, Integer newOrder) {
        Category category = getById(categoryId);
        category.setDisplayOrder(newOrder);
        return categoryRepository.save(category);
    }

    public Category deactivateCategory(Long categoryId) {
        Category category = getById(categoryId);
        category.setActive(false);

        if (category.hasChildren()) {
            for (Category child : category.getChildren()) {
                child.setActive(false);
                categoryRepository.save(child);
            }
        }

        return categoryRepository.save(category);
    }

    public Category activateCategory(Long categoryId) {
        Category category = getById(categoryId);
        category.setActive(true);
        return categoryRepository.save(category);
    }

    private String generateSlug(String name) {
        if (name == null) return "";

        String normalized = Normalizer.normalize(name, Normalizer.Form.NFD);
        Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        String slug = pattern.matcher(normalized).replaceAll("");

        slug = slug.replace("ı", "i").replace("İ", "i")
                .replace("ğ", "g").replace("Ğ", "g")
                .replace("ü", "u").replace("Ü", "u")
                .replace("ş", "s").replace("Ş", "s")
                .replace("ö", "o").replace("Ö", "o")
                .replace("ç", "c").replace("Ç", "c");

        slug = slug.toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-")
                .replaceAll("^-|-$", "");

        return slug;
    }

    public boolean isSlugAvailable(String slug) {
        return !categoryRepository.existsBySlug(slug);
    }
}
