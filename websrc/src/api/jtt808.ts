import request from './request';
import axios from "axios";

export function info(param:any) {
  return request.post('/backend/jtt808/info', param);
}

export function pageClient(param:any) {
  return request.post('/backend/jtt808/pageClient', param);
}
export function updateClient(param:any) {
  return request.post('/backend/jtt808/updateClient', param);
}

export function deleteClient(param:any) {
  return request.post('/backend/jtt808/deleteClient', param);
}

export function pageClientLocation(param:any) {
  return request.post('/backend/jtt808/pageClientLocation', param);
}

export function lastClientLocation(param:any) {
  return request.post('/backend/jtt808/lastClientLocation', {});
}

export function play(param:any) {
  return request.post('/backend/jtt808/play', param);
}

export function queryRecordInfo(param:any) {
  return request.post('/backend/jtt808/queryRecordInfo', param);
}

export function playback(param:any) {
  return request.post('/backend/jtt808/playback', param);
}

export function stopPlayback(param:any) {
  return request.post('/backend/jtt808/stopPlayback', param);
}

export function downloadRecord(param:any) {
  return request.post('/backend/jtt808/downloadRecord', param);
}

export function downloadRecordControl(param:any) {
  return request.post('/backend/jtt808/downloadRecordControl', param);
}

export function deleteRecordFile(param:any) {
  return request.post('/backend/jtt808/deleteRecordFile', param);
}

export function listRecordFile(param:any) {
  return request.post('/backend/jtt808/listRecordFile', param);
}

export function downloadRecordFile(param:any) {
  return request.post('/backend/jtt808/downloadRecordFile', param);
}

export function stopClientMedia(param:any) {
  return request.post('/backend/jtt808/stopClientMedia', param);
}

export function listClientMedia(param:any) {
  return request.post('/backend/jtt808/listClientMedia', param);
}


