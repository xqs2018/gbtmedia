package cn.gbtmedia.jtt808.repository;

import cn.gbtmedia.jtt808.entity.Client;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import java.util.List;

/**
 * @author xqs
 */
@Repository
public interface ClientRepository extends JpaRepository<Client,Long>, JpaSpecificationExecutor<Client> {

    Client findByClientId(String clientId);

    List<Client> findByOnline(Integer online);
}
