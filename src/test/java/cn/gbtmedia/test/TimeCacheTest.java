package cn.gbtmedia.test;

import cn.hutool.cache.impl.TimedCache;
import lombok.extern.slf4j.Slf4j;

/**
 * @author xqs
 */
@Slf4j
public class TimeCacheTest {

    public static void main(String[] args) {
        TimedCache cache = new TimedCache<>(100);
    }
}
