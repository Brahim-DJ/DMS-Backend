package com.dms.document.repository;

import com.dms.document.model.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {
    List<Document> findByDepartmentId(Long departmentId);
    
    List<Document> findByDepartmentIdIn(Set<Long> departmentIds);
    
    List<Document> findByCategoryId(Long categoryId);
    
    List<Document> findByCreatedBy(String username);
    
    @Query("SELECT d FROM Document d WHERE d.departmentId IN :departmentIds AND " +
           "LOWER(d.title) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<Document> searchByTitleInDepartments(String keyword, Set<Long> departmentIds);
}