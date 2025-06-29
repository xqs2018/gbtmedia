import axios, { type AxiosResponse, AxiosError } from 'axios';
import { message } from 'ant-design-vue';

const request = axios.create({
  baseURL: import.meta.env.VITE_APP_BASE_API,
  timeout: 30000
});

request.interceptors.response.use(
  (response: AxiosResponse) => {
    const { data, status } = response;
    if (status !== 200 || data.code !== 200) {
      message.error(data.message || '请求失败');
      return Promise.reject(data.message || '请求失败');
    }
    return data;
  },
  (error: AxiosError) => {
    if (error.response){
      // @ts-ignore
      message.error(error.response.data.message || "网络异常");
    }else {
      message.error("网络异常");
    }
    return Promise.reject(error);
  }
);

export default request;
