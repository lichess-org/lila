"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.tfa)window.i18n.tfa={};let i=window.i18n.tfa;i['authenticationCode']="Kimlik təsdiqləmə kodu";i['disableTwoFactor']="2 mərhələli təsdiqləməni deaktiv et";i['enableTwoFactor']="2 mərhələli təsdiqləməni fəallaşdır";i['enterPassword']="Quraşdırmanı tamamlamaq üçün şifrəni və tətbiq tərəfindən yaradılmış kimlik təsdiqləmə kodunu daxil edin. Hər giriş edəndə təsdiqləmə kodunu daxil etməli olacaqsınız.";i['ifYouCannotScanEnterX']=s("Kodu skan edə bilmirsinizsə, tətbiqinizə gizli %s kodunu daxil edin.");i['openTwoFactorApp']="Kimlik təsdiqləmə koduna baxmaq və kimliyinizi təsdiqləmək üçün cihazınızdakı iki mərhələli kimlik təsdiqləmə tətbiqini açın.";i['scanTheCode']="Tətbiq ilə QR kodu skan edin.";i['twoFactorAuth']="2 mərhələli təsdiqləmə";i['twoFactorEnabled']="2 mərhələli təsdiqləmə aktivdir";i['twoFactorHelp']="2 mərhələli təsdiqləmə, hesabınıza daha bir təhlükəsizlik qatı əlavə edir."})()