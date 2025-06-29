package cn.gbtmedia.gbt28181.repository;


import cn.gbtmedia.gbt28181.entity.Platform;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @author xqs
 */
@Repository
public interface PlatformRepository extends JpaRepository<Platform,Long>, JpaSpecificationExecutor<Platform> {

    Platform findByPlatformId(String platformId);

    List<Platform> findByOnline(Integer online);

    List<Platform> findByEnableAndOnline(Integer enable, Integer online);
}
