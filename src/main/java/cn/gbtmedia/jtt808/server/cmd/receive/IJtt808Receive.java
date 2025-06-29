package cn.gbtmedia.jtt808.server.cmd.receive;

import cn.gbtmedia.jtt808.server.cmd.codec.Jtt808Message;
import cn.gbtmedia.jtt808.server.cmd.session.ClientSession;

/**
 * @author xqs
 */
public interface IJtt808Receive {

    boolean support(Jtt808Message request);

    void handle(ClientSession session, Jtt808Message request);
}
