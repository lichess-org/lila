"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.emails)window.i18n.emails={};let i=window.i18n.emails;i['common_contact']=s("Per contactar amb nosaltres, si us plau utilitza %s.");i['common_note']=s("Aquest és un correu de servei relacionat amb el seu ús de %s.");i['common_orPaste']="(No pot clicar? Provi a enganxar-ho al seu navegador!)";i['emailChange_click']="Per confirmar que vostè té accés a aquest correu electrònic, cliqui el següent enllaç:";i['emailChange_intro']="Ha sol·licitat canviar el correu electrònic.";i['emailChange_subject']=s("Confirma l\\'adreça de correu electrònic, %s");i['emailConfirm_click']="Clica l\\'enllaç per activar el teu compte de Lichess:";i['emailConfirm_ignore']="Si no t\\'has registrat a Lichess, pots ignorar tranquil·lament aquest missatge.";i['emailConfirm_subject']=s("Confirma el teu compte de lichess.org, %s");i['logInToLichess']=s("Inicia sessió a lichess.org, %s");i['passwordReset_clickOrIgnore']="Si has fet aquesta sol·licitud, clica l\\'enllaç de sota. Sinó, pots ignorar aquest correu.";i['passwordReset_intro']="Hem rebut una sol·licitud per restablir la contrasenya del teu compte.";i['passwordReset_subject']=s("Restableix la teva contrasenya de lichess.org, %s");i['welcome_subject']=s("Benvingut/da a lichess.org, %s");i['welcome_text']=s("Ha creat correctament el seu compte a https://lichess.org.\n\nAquí està la seva pàgina de perfil: %1$s. Pot personalitzar-la a %2$s.\n\nDiverteixi\\'s i esperem que les seves peces sempre trobin el camí cap al rei del seu adversari!")})()