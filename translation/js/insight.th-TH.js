"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.insight)window.i18n.insight={};let i=window.i18n.insight;i['cantSeeInsights']=s("ขออภัย คุณไม่สามารถดูข้อมูลเชิงลึกหมากรุกของ %s ได้");i['crunchingData']="ตอนนี้กำลังจัดข้อมูลเพื่อคุณโดยเฉพาะ";i['generateInsights']=s("สร้างข้อมูลเชิงลึกหมากรุกของ %s");i['insightsAreProtected']=s("ข้อมูลเชิงลึกหมากรุกของ%sถูกปิดบัง");i['insightsSettings']="ตั้งค่าข้อมูลเชิงลึก";i['maybeAskThemToChangeTheir']=s("อาจขอให้พวกเขาเปลี่ยน %s ของพวกเขา");i['xChessInsights']=s("ข้อมูลหมากรุกเชิงลึกของ%s");i['xHasNoChessInsights']=s("%sยังไม่ข้อมูลเชิงลึกของหมากรุก")})()