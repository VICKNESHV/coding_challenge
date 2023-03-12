package com.celonis.challenge.controllers;

import com.celonis.challenge.model.Task;
import com.celonis.challenge.services.FileService;
import com.celonis.challenge.services.TaskService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    @Autowired
    private TaskService taskService;

    @Autowired
    private FileService fileService;

    @GetMapping("/")
    public List<Task> listTasks() {
        return taskService.listTasks();
    }

    @PostMapping("/")
    public Task createTask(@RequestBody @Valid Task task) {
        return taskService.createTask(task);
    }

    @GetMapping("/{taskId}")
    public Task getTask(@PathVariable String taskId) {
        return taskService.getTask(taskId);
    }

    @PutMapping("/{taskId}")
    public Task updateTask(@PathVariable String taskId,
                                            @RequestBody @Valid Task task) {
        return taskService.update(taskId, task);
    }

    @DeleteMapping("/{taskId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTask(@PathVariable String taskId) {
        taskService.delete(taskId);
    }

    @PostMapping("/{taskId}/execute")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void executeTask(@PathVariable String taskId) {
        taskService.executeTask(taskId);
    }

    @GetMapping("/{taskId}/result")
    public ResponseEntity<FileSystemResource> getResult(@PathVariable String taskId) {
        return fileService.getTaskResult(taskId);
    }

    @GetMapping("/{taskId}/progress")
    public String getProgress(@PathVariable String taskId) {
        Task task = taskService.getTask(taskId);
        return "TaskId:"+ taskId + " is "+ task.getTaskStatus() +" and the progress is "+ Optional.ofNullable(task.getProgress()).orElse(0.0).doubleValue()+"%";
    }
    
    @DeleteMapping("/{taskId}/cancel")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancelTask(@PathVariable String taskId) {
        taskService.cancelTask(taskId);
    }

}
