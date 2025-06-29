package cn.gbtmedia.gbt28181.repository;


import cn.gbtmedia.gbt28181.entity.PlatformChannel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @author xqs
 */
@Repository
public interface PlatformChannelRepository extends JpaRepository<PlatformChannel,Long>, JpaSpecificationExecutor<PlatformChannel> {

    PlatformChannel findByPlatformIdAndChannelId(String platformId,String channelId);

    List<PlatformChannel> findByPlatformId(String platformId);

    List<PlatformChannel> findByDeviceIdAndChannelId(String deviceId,String channelId);
}
