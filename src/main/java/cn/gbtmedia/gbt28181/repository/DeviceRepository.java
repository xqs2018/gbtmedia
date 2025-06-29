package cn.gbtmedia.gbt28181.repository;

import cn.gbtmedia.gbt28181.entity.Device;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @author xqs
 */
@Repository
public interface DeviceRepository extends JpaRepository<Device,Long>, JpaSpecificationExecutor<Device> {

    Device findByDeviceId(String deviceId);

    List<Device> findByOnline(Integer online);

    Device findBySipIpAndSipPort(String sipIp, int sipPort);
}
