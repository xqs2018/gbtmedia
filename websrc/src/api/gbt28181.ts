import request from './request';
import axios from "axios";

export function info(param:any) {
  return request.post('/backend/gbt28181/info', param);
}

export function pageDevice(param:any) {
  return request.post('/backend/gbt28181/pageDevice', param);
}

export function updateDevice(param:any) {
  return request.post('/backend/gbt28181/updateDevice', param);
}

export function deleteDevice(param:any) {
    return request.post('/backend/gbt28181/deleteDevice', param);
}

export function pageDeviceChannel(param:any) {
  return request.post('/backend/gbt28181/pageDeviceChannel', param);
}

export function updateDeviceChannel(param:any) {
    return request.post('/backend/gbt28181/updateDeviceChannel', param);
}

export function deleteDeviceChannel(param:any) {
    return request.post('/backend/gbt28181/deleteDeviceChannel', param);
}

export function channelTree(param:any) {
    return request.post('/backend/gbt28181/channelTree', param);
}
export function queryCatalog(param:any) {
  return request.post('/backend/gbt28181/queryCatalog', param);
}

export function queryDeviceInfo(param:any) {
  return request.post('/backend/gbt28181/queryDeviceInfo', param);
}
export function listRecordFile(param:any) {
  return request.post('/backend/gbt28181/listRecordFile', param);
}

export function deleteRecordFile(param:any) {
  return request.post('/backend/gbt28181/deleteRecordFile', param);
}

export function queryRecordInfo(param:any) {
  return request.post('/backend/gbt28181/queryRecordInfo', param);
}

export function play(param:any) {
  return request.post('/backend/gbt28181/play', param);
}

export function talk(param:any) {
  return request.post('/backend/gbt28181/talk', param);
}

export function broadcast(param:any) {
  return request.post('/backend/gbt28181/broadcast', param);
}

// leftRight; 0:停止 1:左移 2:右移
// upDown; 0:停止 1:上移 2:下移
// inOut; 镜头放大缩小 0:停止 1:缩小 2:放大
// 镜头移动速度 moveSpeed = 255;
// 镜头缩放速度 zoomSpeed = 1;
export function controlPtzCmd(param:any) {
  return request.post('/backend/gbt28181/controlPtzCmd', param);
}

export function playback(param:any) {
  return request.post('/backend/gbt28181/playback', param);
}

export function playbackSpeed(param:any) {
  return request.post('/backend/gbt28181/playbackSpeed', param);
}

export function download(param:any) {
  return request.post('/backend/gbt28181/download', param);
}

export function listInvite(param:any) {
  return request.post('/backend/gbt28181/listInvite', param);
}
export function stopInvite(param:any) {
  return request.post('/backend/gbt28181/stopInvite', param);
}

export function pagePlatform(param:any) {
  return request.post('/backend/gbt28181/pagePlatform', param);
}

export function savePlatform(param:any) {
  return request.post('/backend/gbt28181/savePlatform', param);
}

export function deletePlatform(param:any) {
  return request.post('/backend/gbt28181/deletePlatform', param);
}

export function enablePlatform(param:any) {
  return request.post('/backend/gbt28181/enablePlatform', param);
}

export function savePlatformChannelList(param:any) {
  return request.post('/backend/gbt28181/savePlatformChannelList', param);
}

export function listPlatformChannel(param:any) {
  return request.post('/backend/gbt28181/listPlatformChannel', param);
}
