package cn.gbtmedia.gbt28181.repository;

import cn.gbtmedia.gbt28181.entity.InviteInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

/**
 * @author xqs
 */
@Repository
public interface InviteInfoRepository extends JpaRepository<InviteInfo,Long>, JpaSpecificationExecutor<InviteInfo> {

}
