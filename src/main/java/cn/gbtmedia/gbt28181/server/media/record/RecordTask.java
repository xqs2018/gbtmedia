package cn.gbtmedia.gbt28181.server.media.record;

import lombok.Data;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author xqs
 */
@Data
public abstract class RecordTask{

    protected RecordParam recordParam;

    protected AtomicInteger recordFileSecond = new AtomicInteger(0);

    protected volatile boolean isStart;

    protected volatile boolean isStop;

    public void start(){
        if(isStart){
            return;
        }
        isStart = true;
        doStart();
    }

    public void stop() {
        isStop = true;
        doStop();
    }

    public abstract void doStart();

    public abstract void doStop();
}
