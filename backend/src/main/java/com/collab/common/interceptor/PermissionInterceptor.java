package com.collab.common.interceptor;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.collab.common.annotation.RequirePermission;
import com.collab.common.exception.BusinessException;
import com.collab.common.utils.LoginUserContext;
import com.collab.entity.*;
import com.collab.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.lang.reflect.Method;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 权限拦截器
 */
@RequiredArgsConstructor
public class PermissionInterceptor implements HandlerInterceptor {

    private final UserRoleMapper userRoleMapper;

    private final RolePermissionMapper rolePermissionMapper;

    private final PermissionMapper permissionMapper;

    @Override
    public boolean preHandle(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler
    ) {

        if (!(handler instanceof HandlerMethod methodHandler)) {
            return true;
        }
        Method method = methodHandler.getMethod();

        RequirePermission permission = method.getAnnotation(RequirePermission.class);

        // 没加权限注解直接放行
        if (permission == null) {
            return true;
        }

        String permissionCode = permission.value();

        Long userId = LoginUserContext.getUserId();

        // 1.查询用户角色
        LambdaQueryWrapper<UserRole> userRoleWrapper = new LambdaQueryWrapper<>();

        userRoleWrapper.eq(
                UserRole::getUserId,
                userId
        );

        List<UserRole> userRoles = userRoleMapper.selectList(userRoleWrapper);

        if (userRoles.isEmpty()) {
            throw new BusinessException("暂无权限");
        }

        List<Long> roleIds = userRoles.stream()
                .map(UserRole::getRoleId)
                .collect(Collectors.toList());

        // 2.查询角色权限
        LambdaQueryWrapper<RolePermission> rolePermissionWrapper = new LambdaQueryWrapper<>();

        rolePermissionWrapper.in(
                RolePermission::getRoleId,
                roleIds
        );

        List<RolePermission> rolePermissions = rolePermissionMapper
                .selectList(rolePermissionWrapper);

        List<Long> permissionIds = rolePermissions
                .stream()
                .map(RolePermission::getPermissionId)
                .collect(Collectors.toList());

        if (permissionIds.isEmpty()) {
            throw new BusinessException("暂无权限");
        }

        // 3.查询权限
        LambdaQueryWrapper<Permission> permissionWrapper = new LambdaQueryWrapper<>();

        permissionWrapper.in(
                Permission::getId,
                permissionIds
        );

        List<Permission> permissions = permissionMapper.selectList(permissionWrapper);

        List<String> permissionCodes = permissions
                .stream()
                .map(Permission::getPermissionCode)
                .toList();

        // 判断权限
        if (!permissionCodes.contains(permissionCode)) {
            throw new BusinessException("权限不足");
        }

        return true;
    }
}