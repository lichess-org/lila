"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.tourname)window.i18n.tourname={};let i=window.i18n.tourname;i['classicalShield']="Classical Shield";i['classicalShieldArena']="Classical Shield Arena";i['dailyClassical']="Codzienny turniej Classical";i['dailyClassicalArena']="Codzienny turniej Classical Arena";i['dailyRapid']="Codzienny turniej Rapid";i['dailyRapidArena']="Codzienny turniej Rapid Arena";i['dailyX']=s("Codzienny turniej %s");i['dailyXArena']=s("Codzienny turniej %s Arena");i['easternClassical']="Afro-eurazjatycki turniej Classical";i['easternClassicalArena']="Afro-eurazjatycki turniej Classical Arena";i['easternRapid']="Afro-eurazjatycki turniej Rapid";i['easternRapidArena']="Afro-eurazjatycki turniej Rapid Arena";i['easternX']=s("Afro-eurazjatycki turniej %s");i['easternXArena']=s("Afro-eurazjatycki turniej %s Arena");i['eliteX']=s("Elite %s");i['eliteXArena']=s("Elite %s Arena");i['hourlyRapid']="Cogodzinny turniej Rapid";i['hourlyRapidArena']="Cogodzinny turniej Rapid Arena";i['hourlyX']=s("Cogodzinny turniej %s");i['hourlyXArena']=s("Cogodzinny turniej %s Arena");i['monthlyClassical']="Comiesięczny turniej Classical";i['monthlyClassicalArena']="Comiesięczny turniej Classical Arena";i['monthlyRapid']="Comiesięczny turniej Rapid";i['monthlyRapidArena']="Comiesięczny turniej Rapid Arena";i['monthlyX']=s("Comiesięczny turniej %s");i['monthlyXArena']=s("Comiesięczny turniej %s Arena");i['rapidShield']="Rapid Shield";i['rapidShieldArena']="Rapid Shield Arena";i['weeklyClassical']="Cotygodniowy turniej Classical";i['weeklyClassicalArena']="Cotygodniowy turniej Classical Arena";i['weeklyRapid']="Cotygodniowy turniej Rapid";i['weeklyRapidArena']="Cotygodniowy turniej Rapid Arena";i['weeklyX']=s("Cotygodniowy turniej %s");i['weeklyXArena']=s("Cotygodniowy turniej %s Arena");i['xArena']=s("%s Arena");i['xShield']=s("%s Shield");i['xShieldArena']=s("%s Shield Arena");i['xTeamBattle']=s("%s Pojedynek Klubów");i['yearlyClassical']="Doroczny turniej Classical";i['yearlyClassicalArena']="Doroczny turniej Classical Arena";i['yearlyRapid']="Doroczny turniej Rapid";i['yearlyRapidArena']="Doroczny turniej Rapid Arena";i['yearlyX']=s("Doroczny turniej %s");i['yearlyXArena']=s("Doroczny turniej %s Arena")})()