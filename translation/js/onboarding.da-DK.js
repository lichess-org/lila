"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.onboarding)window.i18n.onboarding={};let i=window.i18n.onboarding;i['configureLichess']="Indstil Lichess, som du vil have det.";i['enabledKidModeSuggestion']=s("Skal et barn bruge denne konto? Det kan være en god idé at aktivere %s.");i['exploreTheSiteAndHaveFun']="Udforsk webstedet og have det sjovt :)";i['followYourFriendsOnLichess']="Følg dine venner på Lichess.";i['improveWithChessTacticsPuzzles']="Bliv bedre med taktiske skakopgaver.";i['learnChessRules']="Lær reglerne for skak";i['learnFromXAndY']=s("Lær af %1$s og %2$s.");i['playInTournaments']="Spil i turneringer.";i['playOpponentsFromAroundTheWorld']="Spil mod modstandere fra hele verden.";i['playTheArtificialIntelligence']="Spil mod den kunstige intelligens.";i['thisIsYourProfilePage']="Dette er din profilside.";i['welcome']="Velkommen!";i['welcomeToLichess']="Velkommen til lichess.org!";i['whatNowSuggestions']="Hvad nu? Her er et par forslag:"})()