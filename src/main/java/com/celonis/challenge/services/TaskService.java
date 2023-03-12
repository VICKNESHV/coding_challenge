package com.celonis.challenge.services;

import java.net.URL;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import com.celonis.challenge.exceptions.InternalException;
import com.celonis.challenge.exceptions.NotFoundException;
import com.celonis.challenge.model.Task;
import com.celonis.challenge.model.TaskRepository;
import com.celonis.challenge.model.TaskStatus;
import com.celonis.challenge.model.TaskType;

@Service
@Lazy
public class TaskService {

    private final Logger LOGGER = LoggerFactory.getLogger(getClass());

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private FileService fileService;

    private final ExecutorService executorService = Executors.newFixedThreadPool(10);;

    private final Map<String, Runnable> runningTasks = new ConcurrentHashMap<>();
    
    @PreDestroy
    public void shutdownExecutorService() {
        executorService.shutdown();
    }

    public List<Task> listTasks() {
        return taskRepository.findAll();
    }

    public Task createTask(Task task) {
        task.setId(null);
        task.setCreationDate(new Date());
        task.setTaskStatus(TaskStatus.QUEUED);
        return taskRepository.save(task);
    }

    public Task getTask(String taskId) {
        return get(taskId);
    }

    public Task update(String taskId, Task task) {
        Task existingTask = get(taskId);
        existingTask.setCreationDate(task.getCreationDate());
        existingTask.setName(task.getName());
        return taskRepository.save(existingTask);
    }

    public void delete(String taskId) {
        taskRepository.deleteById(taskId);
    }

    public void executeTask(String taskId) {
        Task task = get(taskId);
        if(task.getTaskStatus()==TaskStatus.QUEUED){
            if (task.getTaskType() == TaskType.COUNTER) {
                executeCounterTask(task);
            } else if (task.getTaskType() == TaskType.PROJECT_GENERATION) {
                executeDefaultTask(task);
            }else {
                throw new InternalException("Unsupported task type");
            }
        }else{
            throw new InternalException("Unsupported task status");
        }
    }

    public void cancelTask(String taskId) {
        Task task = get(taskId);
        if (task.getTaskType() == TaskType.COUNTER && task.getTaskStatus() == TaskStatus.RUNNING) {
            Thread thread = (Thread) runningTasks.get(taskId);
            if (thread != null) {
                thread.interrupt();
                runningTasks.remove(taskId);
            }
            task.setTaskStatus(TaskStatus.CANCELLED);
            taskRepository.save(task);
        }
    }

   /*  public void cancelTask(String taskId) {
    	ProjectGenerationTask task = get(taskId);
        if (task.getTaskType() == TaskType.COUNTER && task.getTaskStatus() == TaskStatus.RUNNING) {
            task.setTaskStatus(TaskStatus.CANCELLED);
            projectGenerationTaskRepository.save(task);
        }
    } */

    

    private void executeCounterTask(Task counterTask) {
        executorService.submit(() -> {
            try {
                // keep track of the current thread
                Thread currentThread = Thread.currentThread();
                runningTasks.put(counterTask.getId(), currentThread);
                Integer x = counterTask.getX();
                Integer y = counterTask.getY();
                if (x != null && y != null){
                int currentCount = x;
                while (currentCount <= y) {
                    Thread.sleep(1000);
                    if (currentThread.isInterrupted()) {
                        LOGGER.error("Taskid: "+counterTask.getId()+" is interrupted");
                        throw new InterruptedException();
                    }
                    double percentage =  ((double) (currentCount - x) / (double) (y - x) * 100);
                    counterTask.setTaskStatus(TaskStatus.RUNNING);
                    counterTask.setProgress(percentage);
                    taskRepository.save(counterTask);
                    currentCount++;
                }}
                counterTask.setProgress(100.00);
                counterTask.setTaskStatus(TaskStatus.SUCCESS);
                taskRepository.save(counterTask);
            } catch (InterruptedException e) {
                counterTask.setTaskStatus(TaskStatus.CANCELLED);
                taskRepository.save(counterTask);
                LOGGER.error("Taskid: "+counterTask.getId()+" is cancelled");
            } catch (Exception e) {
                counterTask.setTaskStatus(TaskStatus.FAILED);
                taskRepository.save(counterTask);
                throw new InternalException(e);
            }finally {
                runningTasks.remove(counterTask.getId());
            }
        });
    }

    private void executeDefaultTask(Task defaultTask) {
        URL url = Thread.currentThread().getContextClassLoader().getResource("challenge.zip");
        LOGGER.info("URL is" + url);
        if (url == null) {
            defaultTask.setTaskStatus(TaskStatus.FAILED);
            taskRepository.save(defaultTask);
            throw new InternalException("Zip file not found");
        }
        try {
            fileService.storeResult(defaultTask.getId(), url);
            defaultTask.setProgress(100.00);
            defaultTask.setTaskStatus(TaskStatus.SUCCESS);
            taskRepository.save(defaultTask);
        } catch (Exception e) {
            defaultTask.setTaskStatus(TaskStatus.FAILED);
            taskRepository.save(defaultTask);
            throw new InternalException(e);
        }
    }

    private Task get(String taskId) {
        Optional<Task> task = taskRepository.findById(taskId);
        return task.orElseThrow(NotFoundException::new);
    }

}
