"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.onboarding)window.i18n.onboarding={};let i=window.i18n.onboarding;i['configureLichess']="Configurez Lichess à votre goût.";i['enabledKidModeSuggestion']=s("Un enfant utilisera-t-il ce compte ? Songez à activer le %s.");i['exploreTheSiteAndHaveFun']="Explorez le site et amusez-vous :-)";i['followYourFriendsOnLichess']="Suivez vos amis sur Lichess.";i['improveWithChessTacticsPuzzles']="Améliorez-vous en résolvant des problèmes de tactique.";i['learnChessRules']="Apprendre les règles des échecs";i['learnFromXAndY']=s("Apprenez avec les %1$s et les %2$s.");i['playInTournaments']="Participez à des tournois.";i['playOpponentsFromAroundTheWorld']="Jouez contre des adversaires du monde entier.";i['playTheArtificialIntelligence']="Affrontez l\\'intelligence artificielle.";i['thisIsYourProfilePage']="Voici votre profil.";i['welcome']="Bienvenue !";i['welcomeToLichess']="Bienvenue sur lichess.org !";i['whatNowSuggestions']="Et maintenant? Quelques suggestions :"})()