"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.appeal)window.i18n.appeal={};let i=window.i18n.appeal;i['arenaBanned']="Via konto estas forbarita de aliĝi al arenoj.";i['boosterMarked']="Via konto estas markita por ranga minpulado.";i['cleanAllGood']="Via konto ne estas markita aŭ limigita. Vi bonas!";i['engineMarked']="Via konton estas markita por ekstera asisto en ludoj.";i['engineMarkedInfo']=s("Ni difini tion kiel uzado de io ekstera helpo por plifortigi vian scion kaj/aŭ kalkuladajn kapablojn por gajni maljustan avantaĝon je via kontraŭulo. Vidu la %s paĝon por plu detaloj.");i['prizeBanned']="Via konto estas forbarita de turniroj kun realaj premioj."})()