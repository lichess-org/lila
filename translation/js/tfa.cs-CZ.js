"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.tfa)window.i18n.tfa={};let i=window.i18n.tfa;i['authenticationCode']="Ověřovací kód";i['disableTwoFactor']="Zakázat dvoufázové ověření";i['enableTwoFactor']="Povolit dvoufázové ověřování";i['enterPassword']="Zadejte své heslo a ověřovací kód vygenerovaný aplikací pro dokončení nastavení. Při každém přihlášení budete potřebovat nový ověřovací kód.";i['ifYouCannotScanEnterX']=s("Pokud nemůžete naskenovat kód, zadejte %s do vaší aplikace.");i['ifYouLoseAccessTwoFactor']=s("Poznámka: Pokud ztratíte přístup ke kódům dvoufaktorového ověřování, můžete provést %s prostřednictvím e-mailu.");i['openTwoFactorApp']="Otevřete aplikaci pro dvoufázové ověřování na vašem zařízení a najděte svůj ověřovací kód.";i['scanTheCode']="Naskenujte QR kód pomocí aplikace.";i['setupReminder']="Pro zabezpečení vašeho účtu si zapněte dvoufázové ověření na https://lichess.org/account/twofactor. Tuto zprávu jste obdrželi protože zastaváte speciální funkce jako je vedoucí týmu,coach, učitel nebo streamer";i['twoFactorAppRecommend']="Pořiďte si aplikaci pro dvoufaktorové ověřování. Doporučujeme následující aplikace:";i['twoFactorAuth']="Dvoufázové ověření";i['twoFactorEnabled']="Dvoufázové ověřování zapnuto";i['twoFactorHelp']="Dvoufázové ověření vašeho účtu přidává další vrstvu zabezpečení.";i['twoFactorToDisable']="K vypnutí dvoufaktorového ověřování potřebujete heslo a ověřovací kód z ověřovací aplikace."})()