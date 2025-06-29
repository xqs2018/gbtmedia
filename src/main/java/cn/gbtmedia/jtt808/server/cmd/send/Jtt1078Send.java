package cn.gbtmedia.jtt808.server.cmd.send;

import cn.gbtmedia.jtt808.dto.CmdResult;
import cn.gbtmedia.jtt808.server.cmd.message.T0001;
import cn.gbtmedia.jtt808.server.cmd.message.T1003;
import cn.gbtmedia.jtt808.server.cmd.message.T1205;
import cn.gbtmedia.jtt808.server.cmd.message.T9003;
import cn.gbtmedia.jtt808.server.cmd.message.T9101;
import cn.gbtmedia.jtt808.server.cmd.message.T9102;
import cn.gbtmedia.jtt808.server.cmd.message.T9201;
import cn.gbtmedia.jtt808.server.cmd.message.T9202;
import cn.gbtmedia.jtt808.server.cmd.message.T9205;
import cn.gbtmedia.jtt808.server.cmd.message.T9206;
import cn.gbtmedia.jtt808.server.cmd.message.T9207;

/**
 * @author xqs
 */
public interface Jtt1078Send {

     /**
      * 实时音视频播放请求
      */
     CmdResult<T0001> sendT9101(T9101 t9101);

     /**
      * 音视频实时播放控制
      */
     CmdResult<T0001> sendT9102(T9102 T9102);

     /**
      * 查询终端音视频属性
      */
     CmdResult<T1003> sendT9003(T9003 t9003);

     /**
      * 查询终端录像
      */
     CmdResult<T1205> sendT9205(T9205 t9205);

     /**
      * 录像音视频播放请求
      */
     CmdResult<T1205> sendT9201(T9201 t9201);

     /**
      * 音视频录像播放控制
      */
     CmdResult<T0001> sendT9202(T9202 t9202);

     /**
      * 音视频录像上传
      */
     CmdResult<T0001> sendT9206(T9206 t9206);

     /**
      * 音视频录像上传控制
      */
     CmdResult<T0001> sendT9207(T9207 t9207);
}
