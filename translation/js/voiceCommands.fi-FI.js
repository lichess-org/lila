"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.voiceCommands)window.i18n.voiceCommands={};let i=window.i18n.voiceCommands;i['cancelTimerOrDenyARequest']="Peruuta ajastin tai hylkää pyyntö";i['castle']="Tee linnoitus (kummalle tahansa puolelle)";i['instructions1']=s("Kytke äänentunnistus päälle ja pois %1$s-napista, avaa tämä apuikkunna %2$s-napista ja muokkaa puheasetuksia %3$s-valikosta.");i['instructions2']="Jos komento jää epäselväksi, laudalla näytetään usean siirron nuolet. Valitse haluamasi siirtonuoli lausumalla sen väri tai numero.";i['instructions3']=s("Jos nuoli näyttää tutkan pyyhkäisyn, siirto suoritetaan ympyrän tultua valmiiksi. Tänä aikana voit vain sanoa %1$s tehdäksesi siirron välittömästi, %2$s peruuttaaksesi sen, tai sanoa jonkin toisen nuolen värin tai numeron. Asetuksissa tätä ajastinta voi muokata tai sen voi poistaa kokonaan käytöstä.");i['instructions4']=s("Ota käyttöön %s meluisassa ympäristössä. Pidä Shift-näppäintä painettuna, kun sanot äänikomentoja tätä asetusta käyttäessäsi.");i['instructions5']="Käyttäessäsi foneettisia aakkosia shakkilaudan linjat ovat paremmin tunnistettavissa.";i['instructions6']=s("%s selitetään yksityiskohtaisesti ääniohjattujen siirtojen asetukset.");i['moveToE4OrSelectE4Piece']="Siirrä e4:ään tai valitse e4:ssä oleva nappula";i['phoneticAlphabetIsBest']="Foneettiset aakkoset toimivat parhaiten";i['playPreferredMoveOrConfirmSomething']="Tee ensisijainen siirto tai vahvista jotain";i['selectOrCaptureABishop']="Valitse tai lyö lähetti";i['showPuzzleSolution']="Näytä tehtävän ratkaisu";i['sleep']="Nuku (jos herätyssana on käytössä)";i['takeRookWithQueen']="Lyö torni daamilla";i['thisBlogPost']="Tässä blogikirjoituksessa";i['turnOffVoiceRecognition']="Poista äänen tunnistus käytöstä";i['voiceCommands']="Äänikomennot";i['watchTheVideoTutorial']="Katso opastusvideo"})()