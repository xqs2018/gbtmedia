package cn.gbtmedia.test;

import cn.hutool.core.util.IdUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.IntStream;

/**
 * @author xqs
 */
public class SubDirTest {
    public static void main(String[] args) {
        Map<String,Integer> map = new HashMap<>();
        IntStream.range(0,10000000).forEach(v->{
            String key = IdUtil.fastSimpleUUID().substring(0, 2);
            Integer count = map.get(key);
            if(count == null){
                count = 0;
            }
            count ++ ;
            map.put(key,count);
        });
        map.forEach((k,v)->{
            System.out.println(k+" "+ v);
        });
    }
}
