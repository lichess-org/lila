"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.tfa)window.i18n.tfa={};let i=window.i18n.tfa;i['authenticationCode']="Код за автентикација";i['disableTwoFactor']="Исклучи ја двофакторната автентикација";i['enableTwoFactor']="Вклучи ја двофакторната автентикација";i['enterPassword']="Внесете ја вашата лозинка и кодот за автентикација создаден од апликацијата, за да го завршите поставувањето. При секое најавување ќе ви треба код за автентикација.";i['ifYouCannotScanEnterX']=s("Ако не можете да го скенирате кодот, внесете го тајниот %s во вашата апликација.");i['openTwoFactorApp']="Отворете ја апликацијата со двофакторна автентикација на вашиот уред, за да го видите вашиот код за автентикација и да го потврдите вашиот идентитет.";i['scanTheCode']="Скенирајте го QR кодот со апликацијата.";i['twoFactorAuth']="Двофакторна автентикација";i['twoFactorEnabled']="Двофакторната автентикација е вклучена";i['twoFactorHelp']="Двофакторната автентикација додава уште еден слој на сигурност на вашата сметка."})()