"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.insight)window.i18n.insight={};let i=window.i18n.insight;i['cantSeeInsights']=s("Maalesef %s adlı oyuncunun satranç verilerini göremezsiniz.");i['crunchingData']="Verileri tam şimdi senin için toparlıyoruz!";i['generateInsights']=s("%s adlı oyuncunun satranç verilerini oluştur.");i['insightsAreProtected']=s("%s adlı oyuncunun satranç verileri gizli");i['insightsSettings']="veri ayarlarını";i['maybeAskThemToChangeTheir']=s("Bir ihtimal %s değiştirmesini talep edebilirsiniz.");i['xChessInsights']=s("%s adlı oyuncunun satranç verileri");i['xHasNoChessInsights']=s("%s adlı oyuncunun satranç verileri henüz analiz edilmemiş!")})()