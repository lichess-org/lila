"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.tfa)window.i18n.tfa={};let i=window.i18n.tfa;i['authenticationCode']="Codigo d\\'autentificación";i['disableTwoFactor']="Desactivar l\\'autentificación en dos pasos";i['enableTwoFactor']="Habilitar l\\'autenticación en dos pasos";i['enterPassword']="Escribe la tuya clau y lo codigo d\\'autentificación chenerau per l\\'aplicación pa completar la configuración. Te caldrá un codigo d\\'autentificación cada vegada que inicies sesión.";i['ifYouCannotScanEnterX']=s("Si no puetz escaniar lo codigo, escribe la clau secreta %s en a tuya app.");i['openTwoFactorApp']="Ubre l\\'autentificación en dos pasos en o tuyos dispositivo pa veyer lo tuyo codigo d\\'autentificación y verifica la tuya identidat.";i['scanTheCode']="Escaneya lo codigo QR con l\\'aplicación.";i['twoFactorAuth']="Autentificación en dos pasos";i['twoFactorEnabled']="Autenticación en dos pasos activada";i['twoFactorHelp']="L\\'autentificación en dos pasos anyade una capa de seguridat adicional a la tuya cuenta."})()