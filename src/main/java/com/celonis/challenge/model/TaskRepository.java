package com.celonis.challenge.model;

import java.util.Date;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TaskRepository extends JpaRepository<Task, String> {

    @Modifying
    @Query("DELETE FROM Task t WHERE t.taskStatus = 'QUEUED' AND t.creationDate <= :cutoffDate")
    void deleteOldUnexecutedTasks(@Param("cutoffDate") Date cutoffDate);
}
