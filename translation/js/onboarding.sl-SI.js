"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.onboarding)window.i18n.onboarding={};let i=window.i18n.onboarding;i['configureLichess']="Konfigurirajte Lichess po svojih željah.";i['enabledKidModeSuggestion']=s("Ali bo otrok uporabljal ta račun? Morda boste želeli omogočiti %s.");i['exploreTheSiteAndHaveFun']="Raziščite stran in se zabavajte :)";i['followYourFriendsOnLichess']="Spremljajte svoje prijatelje na Lichess.";i['improveWithChessTacticsPuzzles']="Izboljšajte se z ugankami šahovske taktike.";i['learnChessRules']="Naučite se šahovskih pravil";i['learnFromXAndY']=s("Učite se od %1$s in %2$s.");i['playInTournaments']="Igraj na turnirjih.";i['playOpponentsFromAroundTheWorld']="Igrajte nasprotnike z vsega sveta.";i['playTheArtificialIntelligence']="Igrajte z umetno inteligenco.";i['thisIsYourProfilePage']="To je stran vašega profila.";i['welcome']="Dobrodošli!";i['welcomeToLichess']="Dobrodošli na lichess.org!";i['whatNowSuggestions']="Kaj zdaj? Tukaj je nekaj predlogov:"})()