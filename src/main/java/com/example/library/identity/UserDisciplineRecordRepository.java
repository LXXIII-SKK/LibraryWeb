package com.example.library.identity;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UserDisciplineRecordRepository extends JpaRepository<UserDisciplineRecord, Long> {

    List<UserDisciplineRecord> findAllByTargetUser_IdOrderByCreatedAtDesc(Long targetUserId);
}
