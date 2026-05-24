package com.collab.controller;

import com.collab.common.annotation.OperationLogAnnotation;
import com.collab.common.annotation.RequirePermission;
import com.collab.common.result.Result;
import com.collab.dto.TaskAssignDTO;
import com.collab.dto.TaskDTO;
import com.collab.dto.TaskStatusDTO;
import com.collab.service.TaskService;
import com.collab.vo.TaskVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/task")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    /**
     * 新增任务
     */
    @RequirePermission("task:add")
    @PostMapping("/add")
    public Result<Void> add(@RequestBody TaskDTO dto){

        taskService.addTask(dto);

        return Result.success();
    }

    /**
     * 任务列表
     */
    @GetMapping("/list")
    public Result<Object> list(){

        return Result.success(taskService.listTask());
    }

    /**
     * 任务分页
     */
    @GetMapping("/page")
    public Result<Object> page(
            @RequestParam Integer pageNum,
            @RequestParam Integer pageSize,
            @RequestParam(required = false) Long projectId,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Long executorId,
            @RequestParam(required = false) Integer priority,
            @RequestParam(required = false) String keyword
    ){

        return Result.success(
                taskService.pageTask(
                        pageNum,
                        pageSize,
                        projectId,
                        status,
                        executorId,
                        priority,
                        keyword
                )
        );
    }

    /**
     * 任务详情
     */
    @GetMapping("/detail/{id}")
    public Result<TaskVO> detail(@PathVariable Long id){

        return Result.success(taskService.detail(id));
    }

    /**
     * 修改任务
     */
    @PutMapping("/update")
    public Result<Void> update(@RequestBody TaskDTO dto){

        taskService.updateTask(dto);

        return Result.success();
    }

    /**
     * 删除任务
     */
    @OperationLogAnnotation("删除任务")//使用aop
    @DeleteMapping("/delete/{id}")
    public Result<Void> delete(@PathVariable Long id){
        log.info("进入删除接口");
        taskService.deleteTask(id);
        return Result.success();
    }

    /**
     * 修改任务状态
     */
    @PutMapping("/status")
    public Result<Void> status(@RequestBody TaskStatusDTO dto){

        taskService.updateStatus(dto);

        return Result.success();
    }

    /**
     * 指派任务
     */
    @PutMapping("/assign")
    public Result<Void> assign(@RequestBody TaskAssignDTO dto){

        taskService.assignTask(dto);

        return Result.success();
    }

    /**
     * 任务统计
     */
    @GetMapping("/statistics")
    public Result<Object> statistics(){

        return Result.success(taskService.statistics());
    }

    /**
     * 发送疑问
     */
    @PostMapping("/question")
    public Result<Void> sendQuestion(@RequestBody Map<String, Object> payload) {
        Long taskId = Long.valueOf(payload.get("taskId").toString());
        Long receiverId = Long.valueOf(payload.get("receiverId").toString());
        String content = (String) payload.get("content");
        taskService.sendQuestion(taskId, receiverId, content);
        return Result.success();
    }

    /**
     * 回复疑问
     */
    @PostMapping("/reply")
    public Result<Void> sendReply(@RequestBody Map<String, Object> payload) {
        Long taskId = Long.valueOf(payload.get("taskId").toString());
        Long receiverId = Long.valueOf(payload.get("receiverId").toString());
        String content = (String) payload.get("content");
        taskService.sendReply(taskId, receiverId, content);
        return Result.success();
    }

}