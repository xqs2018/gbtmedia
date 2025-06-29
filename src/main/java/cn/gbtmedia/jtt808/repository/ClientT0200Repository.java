package cn.gbtmedia.jtt808.repository;

import cn.gbtmedia.common.aop.AsyncBatch;
import cn.gbtmedia.common.util.SpringUtil;
import cn.gbtmedia.jtt808.entity.ClientT0200;
import cn.hutool.core.date.DateTime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.util.List;

/**
 * @author xqs
 */
@Repository
public interface ClientT0200Repository extends JpaRepository<ClientT0200,Long>, JpaSpecificationExecutor<ClientT0200> {

    @AsyncBatch
    default void saveBatch(List<ClientT0200> list){
        JdbcTemplate jdbcTemplate = SpringUtil.getBean(JdbcTemplate.class);
        String sql = "insert into jtt808_client_t0200 (client_id,create_time,update_time,t0200_json) values(?, ?, ?,?)";
        String timeStr = new DateTime().toString();
        List<Object[]> batchArgs = list.stream()
                .map(v -> new Object[]{v.getClientId(), timeStr, timeStr, v.getT0200json()}).toList();
        jdbcTemplate.batchUpdate(sql, batchArgs);
    }
}
