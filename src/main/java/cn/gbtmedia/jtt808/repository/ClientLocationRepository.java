package cn.gbtmedia.jtt808.repository;

import cn.gbtmedia.common.aop.AsyncBatch;
import cn.gbtmedia.common.util.SpringUtil;
import cn.gbtmedia.jtt808.entity.ClientLocation;
import cn.hutool.core.date.DateTime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @author xqs
 */
@Repository
public interface ClientLocationRepository extends JpaRepository<ClientLocation,Long>, JpaSpecificationExecutor<ClientLocation> {
    @AsyncBatch
    default void saveBatch(List<ClientLocation> list){
        JdbcTemplate jdbcTemplate = SpringUtil.getBean(JdbcTemplate.class);
        String sql = "INSERT INTO jtt808_client_location (" +
                "client_id, plate_no, warn_bit, warn_bit_name, status_bit, status_bit_name, " +
                "latitude, longitude, altitude, speed, direction, total_mileage, oil, device_time, " +
                "platform_alarm_id, alarm_name, alarm_file_name, create_time, update_time" +
                ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        String timeStr = new DateTime().toString();
        List<Object[]> batchArgs = list.stream()
                .map(v -> new Object[]{
                        v.getClientId(),
                        v.getPlateNo(),
                        v.getWarnBit(),
                        v.getWarnBitName(),
                        v.getStatusBit(),
                        v.getStatusBitName(),
                        v.getLatitude(),
                        v.getLongitude(),
                        v.getAltitude(),
                        v.getSpeed(),
                        v.getDirection(),
                        v.getTotalMileage(),
                        v.getOil(),
                        v.getDeviceTime(),
                        v.getPlatformAlarmId(),
                        v.getAlarmName(),
                        v.getAlarmFileName(),
                        timeStr,
                        timeStr
                }).toList();
        jdbcTemplate.batchUpdate(sql, batchArgs);
    }

    @Query("SELECT cl FROM ClientLocation cl WHERE cl.id IN (SELECT MAX(cl2.id) FROM ClientLocation cl2 GROUP BY cl2.clientId)")
    List<ClientLocation> findLastClientLocation();

    ClientLocation findByPlatformAlarmId(String platformAlarmId);
}
