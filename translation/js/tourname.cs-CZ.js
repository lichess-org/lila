"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.tourname)window.i18n.tourname={};let i=window.i18n.tourname;i['classicalShield']="Klasická Shield";i['classicalShieldArena']="Klasická Shield aréna";i['dailyClassical']="Denní klasika";i['dailyClassicalArena']="Denní klasická aréna";i['dailyRapid']="Denní Rapid";i['dailyRapidArena']="Denní Rapid aréna";i['dailyX']=s("Denní %s");i['dailyXArena']=s("Denní %s aréna");i['easternClassical']="Východní klasika";i['easternClassicalArena']="Východní klasická aréna";i['easternRapid']="Výhodní Rapid";i['easternRapidArena']="Východní Rapid aréna";i['easternX']=s("Východní %s");i['easternXArena']=s("Východní %s aréna");i['eliteX']=s("Elitní %s");i['eliteXArena']=s("Elitní %s aréna");i['hourlyRapid']="Hodinový Rapid";i['hourlyRapidArena']="Hodinová Rapid aréna";i['hourlyX']=s("Hodinová %s");i['hourlyXArena']=s("Hodinová %s aréna");i['monthlyClassical']="Měsíční klasické";i['monthlyClassicalArena']="Měsíční klasická aréna";i['monthlyRapid']="Měsíční Rapid";i['monthlyRapidArena']="Měsíční Rapid aréna";i['monthlyX']=s("Měsíční %s");i['monthlyXArena']=s("Měsíční aréna %s");i['rapidShield']="Rapid Shield";i['rapidShieldArena']="Rapid Shield aréna";i['weeklyClassical']="Týdenní klasika";i['weeklyClassicalArena']="Týdenní klasická aréna";i['weeklyRapid']="Týdenní Rapid";i['weeklyRapidArena']="Týdenní Rapid aréna";i['weeklyX']=s("Týdenní %s");i['weeklyXArena']=s("Týdenní aréna %s");i['xArena']=s("%s aréna");i['xShield']=s("%s Shield");i['xShieldArena']=s("%s Shield aréna");i['xTeamBattle']=s("Týmová bitva %s");i['yearlyClassical']="Roční klasická";i['yearlyClassicalArena']="Roční klasická aréna";i['yearlyRapid']="Roční Rapid";i['yearlyRapidArena']="Roční Rapid aréna";i['yearlyX']=s("Roční %s");i['yearlyXArena']=s("Roční aréna %s")})()