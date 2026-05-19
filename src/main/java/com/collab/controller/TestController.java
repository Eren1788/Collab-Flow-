package com.collab.controller;

import com.collab.common.result.Result;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 测试前后端联调
 */
@RestController
@RequestMapping("/test")
@Tag(name = "测试页面",description = "测试相关的注解")
public class TestController {

    @GetMapping
    public Result<String> test(){
        return Result.success("后端已成功启动");
    }
}
