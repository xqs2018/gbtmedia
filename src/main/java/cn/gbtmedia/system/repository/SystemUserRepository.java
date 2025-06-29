package cn.gbtmedia.system.repository;

import cn.gbtmedia.system.entity.SysUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

/**
 * @author xqs
 */
@Repository
public interface SystemUserRepository extends JpaRepository<SysUser,Long>, JpaSpecificationExecutor<SysUser> {

    SysUser findByUsername(String username);
}
