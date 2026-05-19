package com.collab.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.collab.entity.ProjectMember;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ProjectMemberMapper extends BaseMapper<ProjectMember> {
}
