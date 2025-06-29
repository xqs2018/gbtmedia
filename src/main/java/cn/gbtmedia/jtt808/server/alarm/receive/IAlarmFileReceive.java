package cn.gbtmedia.jtt808.server.alarm.receive;

import cn.gbtmedia.jtt808.server.alarm.session.AlarmFileSession;

/**
 * @author xqs
 */
public interface IAlarmFileReceive {

    boolean support(Object request);

    void handle(AlarmFileSession session, Object request);
}
