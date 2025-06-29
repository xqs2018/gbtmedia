package cn.gbtmedia.gbt28181.server.sip.receive.request;

import cn.gbtmedia.common.config.ServerConfig;
import cn.gbtmedia.common.util.SpringUtil;
import cn.gbtmedia.gbt28181.server.media.MediaTransport;
import cn.gbtmedia.gbt28181.dto.SipResult;
import cn.gbtmedia.gbt28181.server.sip.event.DeviceEvent;
import cn.gbtmedia.gbt28181.server.sip.receive.IRequestHandler;
import cn.gbtmedia.gbt28181.repository.DeviceRepository;
import cn.gbtmedia.gbt28181.server.sip.util.DigestServerAuthenticationHelper;
import cn.gbtmedia.gbt28181.server.sip.util.SipUtil;
import cn.gbtmedia.gbt28181.server.sip.SipServer;
import cn.gbtmedia.gbt28181.server.sip.send.SipDeviceSend;
import cn.gbtmedia.gbt28181.entity.Device;
import cn.hutool.core.util.ObjectUtil;
import gov.nist.javax.sip.RequestEventExt;
import gov.nist.javax.sip.header.Expires;
import jakarta.annotation.Resource;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import javax.sip.RequestEvent;
import javax.sip.header.AuthorizationHeader;
import javax.sip.header.ExpiresHeader;
import javax.sip.header.ViaHeader;
import javax.sip.message.Request;
import java.util.Date;

/**
 * @author xqs
 */
@Slf4j
@Component
public class RegisterRequestHandler implements IRequestHandler {

    @Resource
    private SipServer sipServer;
    @Resource
    private DeviceRepository deviceRepository;
    @Resource
    private SipDeviceSend sipDeviceSend;

    @Override
    public boolean support(RequestEvent requestEvent) {
        return requestEvent.getRequest().getMethod().equals(Request.REGISTER);
    }

    @Override
    @SneakyThrows
    public void handle(RequestEvent requestEvent) {
        RequestEventExt evtExt = (RequestEventExt) requestEvent;
        // 开始进行注册流程处理
        String requestAddress = evtExt.getRemoteIpAddress() + ":" + evtExt.getRemotePort();
        Request request = evtExt.getRequest();
        // 获取注册信息
        String deviceId = SipUtil.getUserIdFromFromHeader(request);
        // 获取到设备
        Device device = deviceRepository.findByDeviceId(deviceId);
        // 过期头
        ExpiresHeader expiresHeader = (ExpiresHeader) request.getHeader(Expires.NAME);
        if (expiresHeader == null) {
            log.error("register no expiresHeader {}",requestAddress);
            sipServer.response400(requestEvent);
            return;
        }
        if (expiresHeader.getExpires() == 0 && device != null) {
            // 注销成功
            log.info("register offline {} {}", device.getDeviceId(), requestAddress);
            device.setOnline(0);
            deviceRepository.save(device);
            SpringUtil.publishEvent(new DeviceEvent(this,device.getDeviceId(),0));
            sipServer.responseRegister200(requestEvent);
            return;
        }
        // 检查认证请求头是否存在
        AuthorizationHeader authHead = (AuthorizationHeader) request.getHeader(AuthorizationHeader.NAME);
        String password = ServerConfig.getInstance().getGbt28181().getSipPassword();
        if (authHead == null && ObjectUtil.isNotEmpty(password)) {
            log.error("register no authorization {}", requestAddress);
            sipServer.response401(requestEvent);
            return;
        }
        // 校验密码是否正确
        if (ObjectUtil.isNotEmpty(password) && !new DigestServerAuthenticationHelper()
                .doAuthenticatePlainTextPassword(request, password)) {
            log.error("register wrong password {}", requestAddress);
            sipServer.response403(requestEvent);
            return;
        }
        // 回复注册成功
        sipServer.responseRegister200(requestEvent);
        // 设备不存在，新设备入库
        if(device == null){
            device = new Device();
            device.setCharset("GB2312");
            device.setMediaTransport(MediaTransport.tcpPassive.name());
            device.setMaxPlayStream(32);
            device.setMaxDownloadStream(32);
            device.setMaxPlaybackStream(32);
        }
        device.setDeviceId(deviceId);
        // 获取到通信地址等信息
        ViaHeader viaHeader = (ViaHeader) request.getHeader(ViaHeader.NAME);
        String received = viaHeader.getReceived();
        int rPort = viaHeader.getRPort();
        // 解析本地地址替代
        if (ObjectUtil.isEmpty(received) || rPort == -1) {
            received = viaHeader.getHost();
            rPort = viaHeader.getPort();
        }
        device.setSipIp(received);
        device.setSipPort(rPort);
        // 注册成功
        log.info("register online {} {}", device.getDeviceId(), requestAddress);
        device.setExpires(expiresHeader.getExpires());
        ViaHeader reqViaHeader = (ViaHeader) request.getHeader(ViaHeader.NAME);
        String transport = reqViaHeader.getTransport();
        Integer lastOnLien = device.getOnline();
        device.setOnline(1);
        device.setSipTransport("TCP".equals(transport) ? "TCP" : "UDP");
        device.setKeepaliveTime(new Date());
        device.setRegistTime(new Date());
        deviceRepository.save(device);
        SpringUtil.publishEvent(new DeviceEvent(this,device.getDeviceId(),1));
        // 查询设备信息，上次也在线不查询
        if(lastOnLien !=null && lastOnLien == 1){
            return;
        }
        Thread.sleep(10);
        SipResult<?> result = sipDeviceSend.queryDeviceInfo(deviceId);
        Thread.sleep(10);
        SipResult<?> result2 = sipDeviceSend.queryCatalog(deviceId);
        Thread.sleep(10);
        SipResult<?> result3 = sipDeviceSend.subscribeCatalog(deviceId);
    }
}
