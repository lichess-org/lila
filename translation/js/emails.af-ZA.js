"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.emails)window.i18n.emails={};let i=window.i18n.emails;i['common_contact']=s("Om ons te kontak, gebruik asseblief %s.");i['common_note']=s("Dit is \\'n diens e-pos in verband met jou gebruik van %s.");i['common_orPaste']="(Werk dit nie om te klik nie? Probeer dit na die webblaaier oorkopieer!)";i['emailChange_click']="Om te bevestig dat jy toegang tot hierdie e-pos het, klik asseblief die onderstaande skakel:";i['emailChange_intro']="Jy het versoek om jou e-pos adres te verander.";i['emailChange_subject']=s("Bevestig nuwe e-pos adres, %s");i['emailConfirm_click']="Klik die skakel om jou Lichess rekening te aktiveer:";i['emailConfirm_ignore']="Indien jy nie met Lichess geregistreer het nie, kan jy hierdie boodskap met \\'n geruste hart ignoreer.";i['emailConfirm_subject']=s("Bevestig jou lichess.org rekening, %s");i['logInToLichess']=s("Teken in op lichess.org, %s");i['passwordReset_clickOrIgnore']="Indien jy hierdie versoek gemaak het, klik die onderstaande skakel. Indien nie, kan jy hierdie e-pos ignoreer.";i['passwordReset_intro']="Ons het \\'n versoek ontvang om die wagwoord vir jou rekening te herstel.";i['passwordReset_subject']=s("Herstel jou lichess.org wagwoord, %s");i['welcome_subject']=s("Welkom by lichess.org, %s");i['welcome_text']=s("Jy het suksesvol jou rekening op https://lichess.org geskep.\n\nHier is jou profielblad: %1$s. Jy kan dit verpersoonlik op %2$s.\n\nGeniet dit, mag jou stukke altyd hulle weg na die opponent se koning vind!")})()