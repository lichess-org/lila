"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.emails)window.i18n.emails={};let i=window.i18n.emails;i['common_contact']=s("Da bi nas kontaktirali, molimo da koristite %s.");i['common_note']=s("Ovo je servisni e-mail povezan s vašim korištenjem stranice %s.");i['common_orPaste']="(Klikanje na link ne radi? Probajte ga zalijepiti u vaš preglednik!)";i['emailChange_click']="Potvrdite da imate pristup ovom email-u klikom na poveznicu ispod:";i['emailChange_intro']="Zatražili ste promjenu e-mail adrese.";i['emailChange_subject']=s("Potvrdite svoju novu e-mail adresu, %s");i['emailConfirm_click']="Kliknite na link za aktivaciju vašeg Lichess računa:";i['emailConfirm_ignore']="Ako se niste registrirali na Lichess-u možete slobodno ignorirati ovu poruku.";i['emailConfirm_subject']=s("Potvrdite svoj račun na lichess.org, %s");i['logInToLichess']=s("Prijavite se na lichess.org, %s");i['passwordReset_clickOrIgnore']="Ako ste vi podnijeli ovaj zahtjev, kliknite na link ispod. Ako niste, možete zanemariti ovaj e-mail.";i['passwordReset_intro']="Zaprimili smo zahtjev za resetiranje lozinke vašeg računa.";i['passwordReset_subject']=s("Resetirajte svoju lichess lozinku, %s");i['welcome_subject']=s("Dobro došli na lichess.org, %s");i['welcome_text']=s("Vaš račun na https://lichess.org je uspješno stvoren.\n\nOvo je vaš profil: %1$s. Možete unijeti promjene na poveznici %2$s.\n\nUživajte, i neka vaše figure uvijek pronađu put do protivničkog kralja!")})()