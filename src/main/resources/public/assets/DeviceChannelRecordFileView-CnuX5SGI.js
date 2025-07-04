import{d as K,G as _,r as l,H as M,j as Q,C as W,k as X,b,e as n,c as i,g as $,f as y,w as h,y as Y,i as d,q as E,t as g,h as U,J as Z,K as ee,n as ae,o as k,_ as oe}from"./index-BYlyECpH.js";import{r as te,m as le}from"./gbt28181-C3wyvrue.js";import{l as ne}from"./zh_CN-CIeKJixd.js";import"./request-xFzgdMuq.js";const se={class:"record-view-container"},ie={class:"video-list"},re={class:"date-picker-container"},ce={class:"video-item-content"},de={class:"video-index"},ue={class:"video-time"},ve={class:"video-size"},fe={class:"video-player-container"},pe={key:0,class:"video-player"},me={class:"video-container"},_e={key:0,class:"video-loading"},ye=["src"],he={class:"video-actions"},ge={key:1,class:"no-video-selected"},we=K({__name:"DeviceChannelRecordFileView",setup(be){_.locale("zh-cn");const N=`${window.location.protocol}//${window.location.host}/`.replace(/\/$/,""),v=l(_()),C=l([]),D=l(!1),B=a=>a&&a>_().endOf("day"),I=a=>_(a).format("HH:mm:ss"),R=async()=>{D.value=!0;try{const a={pageNo:0,pageSize:1e3,deviceId:x.value,channelId:A.value},e=await te(a);e.data&&(C.value=e.data.map(s=>({...s,loading:!1})))}catch(a){console.error("获取数据失败:",a),d.error("获取数据失败")}finally{D.value=!1}},q=M(()=>{if(!v.value||!C.value.length)return[];const a=v.value.startOf("day"),e=v.value.endOf("day");return C.value.filter(s=>{const m=_(s.startTime);return m.isAfter(a)&&m.isBefore(e)})}),H=()=>{R()},f=l(""),c=l(""),p=l(null),w=l(-1),u=l(!1),o=l(null),r=l(null),L=async(a,e)=>{try{u.value=!0,r.value&&(r.value.abort(),r.value=null),o.value&&(o.value.pause(),o.value.removeAttribute("src"),o.value.load()),f.value="",c.value="",p.value=null,w.value=-1,await ae();const s=`${N}/backend/gbt28181/downloadRecordFile/${a.fileName}`;if(r.value=new AbortController,!(await fetch(s,{signal:r.value.signal,cache:"no-store"})).ok)throw new Error("视频加载失败");f.value=s,c.value=a.fileName,p.value=a,w.value=e}catch(s){if(s.name==="AbortError"){console.log("视频加载已取消");return}console.error("视频切换失败:",s),d.error("视频加载失败，请重试"),u.value=!1}},S=()=>{u.value=!1,o.value&&o.value.play().catch(a=>{console.error("自动播放失败:",a),d.error("自动播放失败，请手动点击播放")})},j=a=>{console.error("视频加载错误:",a),u.value=!1,d.error("视频加载失败，请重试"),o.value&&(o.value.pause(),o.value.removeAttribute("src"),o.value.load()),f.value="",c.value="",p.value=null,w.value=-1},P=async a=>{a.loading=!0;try{await le({fileName:a.fileName}),d.success("删除成功"),await R()}catch(e){console.error("删除失败:",e),d.error("删除失败")}finally{a.loading=!1}},F=W(),x=l(""),A=l("");Q(()=>{x.value=F.query.deviceId,A.value=F.query.channelId,R()}),X(()=>{o.value&&(o.value.pause(),o.value.removeAttribute("src")),r.value&&(r.value.abort(),r.value=null)});const G=()=>{if(!c.value){d.error("没有可下载的文件");return}const a=`${N}/backend/gbt28181/downloadRecordFile/${c.value}`,e=document.createElement("a");e.href=a,e.download=c.value,document.body.appendChild(e),e.click(),document.body.removeChild(e)};return(a,e)=>{var z;const s=y("a-date-picker"),m=y("a-list-item"),J=y("a-list"),O=y("a-spin"),T=y("a-button");return k(),b("div",se,[n("div",ie,[n("div",re,[i(s,{value:v.value,"onUpdate:value":e[0]||(e[0]=t=>v.value=t),disabledDate:B,onChange:H,style:{width:"100%"},locale:$(ne)},null,8,["value","locale"])]),i(O,{spinning:D.value},{default:h(()=>[i(J,{class:"video-list-content","data-source":q.value,bordered:!1},{renderItem:h(({item:t,index:V})=>[i(m,{class:E(["video-item",{"video-item-active":w.value===V}]),onClick:ke=>L(t,V)},{default:h(()=>[n("div",ce,[n("span",de,g(V+1),1),n("span",ue,g(I(t.startTime))+" - "+g(I(t.endTime)),1),n("span",{class:E(["video-type",t.type===1?"type-device":"type-cloud"])},g(t.type===1?"设备":t.type===2?"云端":t.type),3),n("span",ve,g(t.fileSize),1)])]),_:2},1032,["class","onClick"])]),_:1},8,["data-source"])]),_:1},8,["spinning"])]),n("div",fe,[f.value?(k(),b("div",pe,[n("div",me,[u.value?(k(),b("div",_e,[i(O,{size:"large"}),e[3]||(e[3]=n("span",{class:"loading-text"},"视频加载中...",-1))])):Y("",!0),n("video",{ref_key:"videoRef",ref:o,src:f.value,controls:"",muted:"",style:{width:"100%",height:"100%"},onLoadstart:e[1]||(e[1]=t=>u.value=!0),onCanplay:S,onError:j},null,40,ye)]),n("div",he,[i(T,{class:"action-button",onClick:G,disabled:!c.value},{default:h(()=>[i($(Z)),e[4]||(e[4]=U(" 下载 "))]),_:1},8,["disabled"]),i(T,{danger:"",onClick:e[2]||(e[2]=t=>P(p.value)),loading:(z=p.value)==null?void 0:z.loading},{default:h(()=>[i($(ee)),e[5]||(e[5]=U(" 删除 "))]),_:1},8,["loading"])])])):(k(),b("div",ge," 请从左侧选择要播放的视频 "))])])}}}),$e=oe(we,[["__scopeId","data-v-8e3f7fe7"]]);export{$e as default};
