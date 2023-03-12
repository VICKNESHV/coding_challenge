package com.celonis.challenge.services;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import com.celonis.challenge.exceptions.InternalException;
import com.celonis.challenge.model.Task;
import com.celonis.challenge.model.TaskRepository;
import com.celonis.challenge.model.TaskStatus;

@Component
public class FileService {
	
	private final Logger LOGGER = LoggerFactory.getLogger(getClass());

    @Autowired
    private  TaskService taskService;

    @Autowired
    private TaskRepository taskRepository;

    public ResponseEntity<FileSystemResource> getTaskResult(String taskId) {
		/*
		 * Optional<Task> task = taskRepository.findById(taskId); Task
		 * Task projectGenerationTask = task.orElseThrow(NotFoundException::new);
		 */
    	Task projectGenerationTask = taskService.getTask(taskId);
        File inputFile = new File(projectGenerationTask.getStorageLocation());
        LOGGER.info("Input file path is "+ inputFile.getAbsolutePath());
        if (!inputFile.exists()) {
            throw new InternalException("File not generated yet");
        }

        HttpHeaders respHeaders = new HttpHeaders();
        respHeaders.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        respHeaders.setContentDispositionFormData("attachment", "challenge.zip");

        return new ResponseEntity<>(new FileSystemResource(inputFile), respHeaders, HttpStatus.OK);
    }

    public void storeResult(String taskId, URL url) throws IOException {
    	/*
		 * Optional<Task> task = taskRepository.findById(taskId); Task
		 * Task projectGenerationTask = task.orElseThrow(NotFoundException::new);
		 */
    	Task task = taskService.getTask(taskId);
        File outputFile = File.createTempFile(taskId, ".zip");
        outputFile.deleteOnExit();
        task.setTaskStatus(TaskStatus.SUCCESS);
        task.setStorageLocation(outputFile.getAbsolutePath());
        taskRepository.save(task);
        try (InputStream is = url.openStream();
             OutputStream os = new FileOutputStream(outputFile)) {
            IOUtils.copy(is, os);
        }
        LOGGER.info("Output file path is "+ outputFile.getAbsolutePath());
    }
}
