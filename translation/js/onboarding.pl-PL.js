"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.onboarding)window.i18n.onboarding={};let i=window.i18n.onboarding;i['configureLichess']="Skonfiguruj Lichess tak jak chcesz.";i['enabledKidModeSuggestion']=s("Czy tego konta będzie używało dziecko? Możesz włączyć %s.");i['exploreTheSiteAndHaveFun']="Odkrywaj nasz portal i baw się dobrze :)";i['followYourFriendsOnLichess']="Obserwuj swoich znajomych na Lichess.";i['improveWithChessTacticsPuzzles']="Rozwijaj się rozwiązując zadania szachowe.";i['learnChessRules']="Naucz się zasad gry";i['learnFromXAndY']=s("Ucz się z %1$s i %2$s.");i['playInTournaments']="Graj w turniejach.";i['playOpponentsFromAroundTheWorld']="Graj z ludźmi z całego świata.";i['playTheArtificialIntelligence']="Zagraj przeciwko sztucznej inteligencji.";i['thisIsYourProfilePage']="To jest Twoja strona profilowa.";i['welcome']="Witaj!";i['welcomeToLichess']="Witaj na lichess.org!";i['whatNowSuggestions']="Co teraz? Oto kilka sugestii:"})()