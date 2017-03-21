package lee.study.bean;

import io.netty.channel.ChannelHandlerContext;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by yijia on 2017/3/17.
 */
public class Room {
    public static Map<ChannelHandlerContext,Hero> room = new ConcurrentHashMap<ChannelHandlerContext, Hero>();

    private static int userId = 0;

    public synchronized static int buildUserId(){
        return ++userId;
    }
}
