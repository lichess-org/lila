"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.emails)window.i18n.emails={};let i=window.i18n.emails;i['common_contact']=s("Pro nos contatare, pro piaghere impita %s.");i['common_note']=s("Custa est una mail de servitziu, relativa a s\\'impitu tou de%s.");i['common_orPaste']="(No resessis a clicare? Proa a incollare in su browser tou!)";i['emailChange_click']="Pro confirmare chi as atzessu a custa email, pro piaghere, clica in su link a suta:";i['emailChange_intro']="As pedidu de cambiare s\\'indiritzu email tou.";i['emailChange_subject']=s("Cunfirma s\\'indiritzu email nou, %s");i['emailConfirm_click']="Clica su lin pro abilitare su profilu Lichess tou:";i['emailConfirm_ignore']="Si no ti ses registradu in Lichess, podes ignorare custu messagiu chena problemas.";i['emailConfirm_subject']=s("Cunfirma su profilu de Lichess.org tou, %s");i['logInToLichess']=s("Bintra in lichess.org, %s");i['passwordReset_clickOrIgnore']="Si lu as pedidu tue, clica su lin a suta. Si nono, podes ignorare custa mail.";i['passwordReset_intro']="Nos est bistadu pedidu de resetare sa password de profilu tou.";i['passwordReset_subject']=s("Cambia sa password de lichess.org tua, %s");i['welcome_subject']=s("Ben\\'ennidu in lichess.org, %s");i['welcome_text']=s("As criadu su profilu tou de https://lichess.org cun sutzessu.\n\nCusta est sa pagina de su profilu tou: %1$s. La podes acontzare in %2$s.\n\nA ti divertire, e potan sos petzos tuos agatare sempre s\\'istrada cuntra de su re inimigu!")})()