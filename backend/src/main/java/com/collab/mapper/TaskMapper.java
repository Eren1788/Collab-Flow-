package com.collab.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.collab.entity.Task;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TaskMapper extends BaseMapper<Task> {
}
