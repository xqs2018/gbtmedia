import request from './request';
import axios from "axios";

export function monitor(param:any) {
  return request.post('/backend/system/monitor', param);
}

export function userPage(param:any) {
  return request.post('/backend/system/user/page', param);
}

export function updatePassword(param:any) {
  return request.post('/backend/system/user/updatePassword', param);
}

export function logPage(param:any) {
  return request.post('/backend/system/log/page', param);
}
