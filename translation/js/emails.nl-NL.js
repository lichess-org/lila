"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.emails)window.i18n.emails={};let i=window.i18n.emails;i['common_contact']=s("Contact opnemen kan via: %s.");i['common_note']=s("Dit is een service e-mail gekoppeld aan uw gebruik van %s.");i['common_orPaste']="(Werkt klikken niet? Probeer de link in uw browser te plakken!)";i['emailChange_click']="Om te bevestigen dat u toegang heeft tot deze e-mail, klikt u op onderstaande link:";i['emailChange_intro']="U heeft een aanvraag gedaan om uw e-mailadres te wijzigen.";i['emailChange_subject']=s("Bevestig nieuw e-mailadres, %s");i['emailConfirm_click']="Klik op de link om uw Lichess-account te bevestigen:";i['emailConfirm_ignore']="Als u zich niet heeft aangemeld bij Lichess kunt u deze e-mail veilig negeren.";i['emailConfirm_subject']=s("Bevestig uw lichess.org account, %s");i['logInToLichess']=s("Login op lichess.org, %s");i['passwordReset_clickOrIgnore']="Als u deze aanvraag heeft gedaan, klikt u op onderstaande link. U kunt deze e-mail anders negeren.";i['passwordReset_intro']="We hebben een aanvraag ontvangen om het wachtwoord voor uw account te herstellen.";i['passwordReset_subject']=s("Herstel uw lichess.org wachtwoord, %s");i['welcome_subject']=s("Welkom op lichess.org, %s");i['welcome_text']=s("Je hebt succesvol een account aangemaakt op https://lichess.org.\n\nHier is je profielpagina: %1$s. Je kunt het personaliseren op %2$s.\n\nVeel plezier, en mogen je stukken altijd hun weg vinden naar de koning van je tegenstander!")})()