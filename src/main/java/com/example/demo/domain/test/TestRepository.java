package com.example.demo.domain.test;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TestRepository extends JpaRepository<TestEntityMySql, Long> {

}
