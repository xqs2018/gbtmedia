package cn.gbtmedia.system.controller;

import cn.gbtmedia.common.extra.SystemMonitor;
import cn.gbtmedia.common.vo.Result;
import cn.gbtmedia.system.dto.QueryParam;
import cn.gbtmedia.system.entity.SysLog;
import cn.gbtmedia.system.entity.SysUser;
import cn.gbtmedia.system.servcie.SysLogService;
import cn.gbtmedia.system.servcie.SysUserService;
import cn.hutool.json.JSONObject;
import jakarta.annotation.Resource;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author xqs
 */
@RequestMapping("/backend/system")
@RestController
public class SystemController {

    @Resource
    private SysUserService sysUserService;

    @Resource
    private SysLogService sysLogService;

    @PostMapping("/monitor")
    public Result<Object> info(@RequestBody JSONObject param){
        SystemMonitor.Info monitorInfo = SystemMonitor.getInstance().getMonitorInfo();
        return Result.success(monitorInfo);
    }

    @PostMapping("/user/page")
    public Result<Page<SysUser>> userPage(@RequestBody QueryParam param){
        Page<SysUser> page = sysUserService.page(param);
        return Result.success(page);
    }

    @PostMapping("/user/updatePassword")
    public Result<?> updatePassword(@RequestBody SysUser param){
        sysUserService.updatePassword(param);
        return Result.success();
    }

    @PostMapping("/log/page")
    public Result<Page<SysLog>> logPage(@RequestBody QueryParam param){
        Page<SysLog> page = sysLogService.page(param);
        return Result.success(page);
    }
}
