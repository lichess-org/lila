"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.tfa)window.i18n.tfa={};let i=window.i18n.tfa;i['enterPassword']="Ilagay ang iyong password at ang authentication code na ginawa ng app para makumpleto ang setup. Kakailanganin mo ng authentication code sa bawat oras na papasok ka.";i['ifYouCannotScanEnterX']=s("Kapag hindi mo ma-scan ang code. Ilagay ang secret key %s sa iyong app.");i['scanTheCode']="I-scan ang QR code gamit ang app."})()