package cn.gbtmedia.gbt28181.repository;

import cn.gbtmedia.gbt28181.entity.DeviceChannel;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @author xqs
 */
@Repository
public interface DeviceChannelRepository extends JpaRepository<DeviceChannel,Long>, JpaSpecificationExecutor<DeviceChannel> {

    List<DeviceChannel> findByDeviceId(String deviceId);

    DeviceChannel findByDeviceIdAndChannelId(String deviceId,String channelId);

    @Transactional
    long deleteByDeviceId(String deviceId);
}
