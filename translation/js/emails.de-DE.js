"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.emails)window.i18n.emails={};let i=window.i18n.emails;i['common_contact']=s("Um uns zu kontaktieren, benutze bitte %s.");i['common_note']=s("Dies ist eine Service-E-Mail im Zusammenhang mit deiner Nutzung von %s.");i['common_orPaste']="(Klicken funktioniert nicht? Versuche den Link in deinen Browser einzufügen!)";i['emailChange_click']="Um zu bestätigen, dass Du Zugriff auf diese E-Mail hast, klicke bitte auf den untenstehenden Link:";i['emailChange_intro']="Du hast eine Änderung deiner E-Mail-Adresse angefordert.";i['emailChange_subject']=s("Bestätige die neue E-Mail-Adresse, %s");i['emailConfirm_click']="Klicke auf den Link, um dein Benutzerkonto bei Lichess zu aktivieren:";i['emailConfirm_ignore']="Wenn Du dich nicht bei Lichess registriert hast, kannst Du diese Nachricht ignorieren.";i['emailConfirm_subject']=s("Bestätige dein Benutzerkonto auf lichess.org, %s");i['logInToLichess']=s("Bei lichess.org anmelden, %s");i['passwordReset_clickOrIgnore']="Wenn Du diese Anfrage gemacht hast, klicke auf den untenstehenden Link. Falls nicht, ignoriere diese E-Mail.";i['passwordReset_intro']="Wir haben eine Anfrage zum Zurücksetzen deines Passworts erhalten.";i['passwordReset_subject']=s("Setze dein Passwort für lichess.org zurück, %s");i['welcome_subject']=s("Herzlich willkommen bei lichess.org, %s");i['welcome_text']=s("Du hast erfolgreich dein Benutzerkonto auf https://lichess.org erstellt.\n\nHier geht es zu deiner Profil-Seite: %1$s. Du kannst sie auf %2$s personalisieren.\n\nViel Spaß und mögen deine Figuren immer den Weg zum gegnerischen König finden!")})()