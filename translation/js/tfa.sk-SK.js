"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.tfa)window.i18n.tfa={};let i=window.i18n.tfa;i['authenticationCode']="Overovací kód";i['disableTwoFactor']="Vypnúť dvojfaktorové overenie";i['enableTwoFactor']="Zapnúť dvojfaktorové overovanie";i['enterPassword']="Zadajte vaše heslo a overovací kód vygenerovaný aplikáciou, na dokončenie nastavenia. Overovací kód budete potrebovať pre každé prihlásenie.";i['ifYouCannotScanEnterX']=s("Ak nemôžete naskenovať kód, zadajte tajný %s do Vašej aplikácie.");i['ifYouLoseAccessTwoFactor']=s("Poznámka: Ak stratíte prístup k svojim dvojfaktorovým overovacím kódom, môžete vykonať %s prostredníctvom e-mailu.");i['openTwoFactorApp']="Otvorte aplikáciu na dvojstupňové overenie na Vašom zariadení, a zistite Váš overovací kód na overenie Vašej identity.";i['scanTheCode']="Oskenujte QR kód s aplikáciou.";i['setupReminder']="Na zabezpečenie svojho konta zapnite prosím dvojfázové overovanie! Urobiť tak môžete tu https://lichess.org/account/twofactor.\nTúto správu ste dostali, pretože vaše konto má špeciálne povinnosti, ako napríklad vedúci tímu, tréner, učiteľ alebo streamer";i['twoFactorAppRecommend']="Získajte aplikáciu na dvojfaktorové overovanie. Odporúčame tieto aplikácie:";i['twoFactorAuth']="Dvojstupňové overenie";i['twoFactorEnabled']="Dvojfaktorové overovanie je zapnuté";i['twoFactorHelp']="Dvojstupňové overenie pridá ďalšiu vrstvu bezpečnosti k vášmu účtu.";i['twoFactorToDisable']="Na vypnutie dvojfaktorového overovania potrebujete svoje heslo a overovací kód z aplikácie autentifikátora."})()