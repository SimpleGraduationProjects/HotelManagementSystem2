package com.hotel.system.service.impl;

import com.hotel.system.util.StringUtils;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hotel.system.entity.Permission;
import com.hotel.system.entity.Role;
import com.hotel.system.entity.RolePermissionRef;
import com.hotel.system.mapper.RoleMapper;
import com.hotel.system.service.RolePermissionRefService;
import com.hotel.system.service.RoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 角色业务逻辑实现类
 * @author liuyanzhao
 * @date: 2022/04/30
 */
@Service
public class RoleServiceImpl implements RoleService {

    @Autowired
    private RoleMapper roleMapper;

    @Autowired
    private RolePermissionRefService rolePermissionRefService;

    @Override
    public BaseMapper<Role> getRepository() {
        return roleMapper;
    }

    @Override
    public QueryWrapper<Role> getQueryWrapper(Role role) {
        //对指定字段查询
        QueryWrapper<Role> queryWrapper = new QueryWrapper<>();
        if (role != null) {
            if (StringUtils.isNotEmpty(role.getRole())) {
                queryWrapper.eq("role", role.getRole());
            }
            if (StringUtils.isNotEmpty(role.getDescription())) {
                queryWrapper.eq("description", role.getDescription());
            }
        }
        return queryWrapper;
    }

    @Override
    public void deleteByUserId(Long userId) {
        roleMapper.deleteByUserId(userId);
    }

    @Override
    public Role findByRoleId(Long roleId) {
        return roleMapper.selectById(roleId);
    }

    @Override
    public Role findByRoleName(String roleName) {
        QueryWrapper queryWrapper = new QueryWrapper();
        queryWrapper.eq("role", roleName);
        return roleMapper.selectOne(queryWrapper);
    }

    @Override
    public Role findByUserId(Long userId) {
        return roleMapper.findByUserId(userId);
    }

    @Override
    public Integer countUserByRoleId(Long roleId) {
        return roleMapper.countUserByRoleId(roleId);
    }

    @Override
    public Integer findMaxLevelByUserId(Long userId) {
        return roleMapper.findMaxLevelByUserId(userId);
    }

    @Override
    public List<Role> findByLessThanLevel(Integer level) {
        return roleMapper.findByLessThanLevel(level);
    }

    @Override
    public Role findDefaultRole() {
        return roleMapper.findDefaultRole();
    }

    @Override
    public Role getMaxRoleByUserId(Long userId) {
        return null;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Role insert(Role role) {
        roleMapper.insert(role);
        if (role.getPermissions() != null && role.getPermissions().size() > 0) {
            List<RolePermissionRef> rolePermissionRefs = new ArrayList<>(role.getPermissions().size());
            for (Permission permission : role.getPermissions()) {
                rolePermissionRefs.add(new RolePermissionRef(role.getId(), permission.getId()));
            }
            rolePermissionRefService.batchSaveByRolePermissionRef(rolePermissionRefs);
        }
        return role;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Role update(Role role) {
        roleMapper.updateById(role);
        if (role.getPermissions() != null && role.getPermissions().size() > 0) {
            // 删除关联
            rolePermissionRefService.deleteRefByRoleId(role.getId());
            // 添加关联
            List<RolePermissionRef> rolePermissionRefs = new ArrayList<>(role.getPermissions().size());
            for (Permission permission : role.getPermissions()) {
                rolePermissionRefs.add(new RolePermissionRef(role.getId(), permission.getId()));
            }
            rolePermissionRefService.batchSaveByRolePermissionRef(rolePermissionRefs);
        }
        return role;
    }

    @Override
    public Role insertOrUpdate(Role entity) {
        if (entity.getId() == null) {
            insert(entity);
        } else {
            update(entity);
        }
        return entity;
    }

}
