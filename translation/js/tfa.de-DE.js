"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.tfa)window.i18n.tfa={};let i=window.i18n.tfa;i['authenticationCode']="Authentifizierungscode";i['disableTwoFactor']="Deaktiviere die Zwei-Faktor-Authentifizierung";i['enableTwoFactor']="Aktiviere die Zwei-Faktor-Authentifizierung";i['enterPassword']="Gib dein Passwort und den Authentifizierungscode ein, der von der App generiert wurde, um die Einrichtung abzuschließen. Du benötigst für jede weitere Anmeldung einen Authentifizierungscode.";i['ifYouCannotScanEnterX']=s("Falls du den Code nicht einscannen kannst, gib den geheimen Code %s in deine App ein.");i['ifYouLoseAccessTwoFactor']=s("Hinweis: Falls du den Zugang zu deinen Zwei-Faktor-Authentifizierungscodes verlierst, kannst du %s per Email durchführen.");i['openTwoFactorApp']="Öffne auf deinem Gerät die App zur zweistufigen Authentifizierung, um deinen Authentifizierungscode anzuzeigen und deine Identität zu bestätigen.";i['scanTheCode']="Scanne den QR Code mit der App.";i['setupReminder']="Bitte aktiviere die Zwei-Faktor-Authentifizierung auf https://lichess.org/account/twofactor, um dein Konto zu sichern.\nDu hast diese Nachricht erhalten, da dein Konto besondere Verantwortung erfordert, wie zum Beispiel das eines Teamführers, Trainers, Lehrers oder Streamers";i['twoFactorAppRecommend']="Hole dir eine App zur Zwei-Faktor-Authentifizierung. Wir empfehlen folgende Apps:";i['twoFactorAuth']="Zwei-Faktor-Authentifizierung";i['twoFactorEnabled']="Zwei-Faktor Authentifizierung aktiviert";i['twoFactorHelp']="Zwei-Faktor Authentifizierung fügt deinem Konto ein zusätzliches Sicherheitsmerkmal hinzu.";i['twoFactorToDisable']="Du benötigst dein Passwort und einen Authentifizierungscode von deiner Authentifizierungs-App, um die Zwei-Faktor-Authentifizierung zu deaktivieren."})()