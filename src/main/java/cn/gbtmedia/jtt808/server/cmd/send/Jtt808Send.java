package cn.gbtmedia.jtt808.server.cmd.send;

import cn.gbtmedia.jtt808.dto.CmdResult;
import cn.gbtmedia.jtt808.server.cmd.message.T0001;
import cn.gbtmedia.jtt808.server.cmd.message.T0104;
import cn.gbtmedia.jtt808.server.cmd.message.T0107;
import cn.gbtmedia.jtt808.server.cmd.message.T8104;
import cn.gbtmedia.jtt808.server.cmd.message.T8107;
import cn.gbtmedia.jtt808.server.cmd.message.T8300;
import cn.gbtmedia.jtt808.server.cmd.message.T9208;

/**
 * @author xqs
 */
public interface Jtt808Send {

    /**
     * 查询终端参数
     */
    CmdResult<T0104> sendT8104(T8104 t8104);

    /**
     * 查询终端属性
     */
    CmdResult<T0107> sendT8107(T8107 t8107);

    /**
     * 下发文本信息
     */
    CmdResult<T0001> sendT8300(T8300 t8300);

    /**
     * 上传报警附件
     */
    CmdResult<T0001> sendT9208(T9208 t9208);
}
