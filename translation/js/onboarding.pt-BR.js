"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.onboarding)window.i18n.onboarding={};let i=window.i18n.onboarding;i['configureLichess']="Configure o Lichess do seu modo.";i['enabledKidModeSuggestion']=s("Alguma criança usará esta conta? Você pode querer habilitar %s.");i['exploreTheSiteAndHaveFun']="Explore o site e divirta-se :)";i['followYourFriendsOnLichess']="Seguir seus amigos no Lichess.";i['improveWithChessTacticsPuzzles']="Melhorar com problemas táticos de xadrez.";i['learnChessRules']="Aprenda regras do xadrez";i['learnFromXAndY']=s("Aprenda com %1$s e %2$s.");i['playInTournaments']="Jogar torneios.";i['playOpponentsFromAroundTheWorld']="Jogar contra adversários de todo o mundo.";i['playTheArtificialIntelligence']="Jogar contra a Inteligência Artificial.";i['thisIsYourProfilePage']="Esta é sua página de perfil.";i['welcome']="Bem vindo(a)!";i['welcomeToLichess']="Bem-vindo(a) ao lichess.org!";i['whatNowSuggestions']="E agora? Aqui estão algumas sugestões:"})()