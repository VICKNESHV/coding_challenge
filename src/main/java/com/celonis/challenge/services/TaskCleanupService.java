package com.celonis.challenge.services;

import java.util.Calendar;
import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.celonis.challenge.model.TaskRepository;

@Service
public class TaskCleanupService {

    @Autowired
    private TaskRepository taskRepository;
    
    @Scheduled(cron = "0 0 0 * * 0") // Runs every Sunday at midnight
    public void cleanUpTasks() {
        // Get the date one week ago
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.WEEK_OF_YEAR, -1);
        Date oneWeekAgo = cal.getTime();
        taskRepository.deleteOldUnexecutedTasks(oneWeekAgo);
    }
}

