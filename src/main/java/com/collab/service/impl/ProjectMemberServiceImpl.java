package com.collab.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.collab.entity.ProjectMember;
import com.collab.mapper.ProjectMemberMapper;
import com.collab.service.ProjectMemberService;
import org.springframework.stereotype.Service;

@Service
public class ProjectMemberServiceImpl extends ServiceImpl<ProjectMemberMapper, ProjectMember> implements ProjectMemberService {
}
