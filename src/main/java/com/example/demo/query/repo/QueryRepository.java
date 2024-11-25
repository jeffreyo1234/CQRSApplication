package com.example.demo.query.repo;

import com.example.demo.query.model.UserProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface QueryRepository extends JpaRepository<UserProjection, Long> {}
