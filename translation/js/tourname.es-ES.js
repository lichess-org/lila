"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.tourname)window.i18n.tourname={};let i=window.i18n.tourname;i['classicalShield']="Escudo clásico";i['classicalShieldArena']="Torneo de escudo clásico";i['dailyClassical']="Clásico diario";i['dailyClassicalArena']="Torneo clásico diario";i['dailyRapid']="Rápido diario";i['dailyRapidArena']="Torneo rápido diario";i['dailyX']=s("%s diario");i['dailyXArena']=s("Torneo %s diario");i['easternClassical']="Clásico oriental";i['easternClassicalArena']="Torneo clásico oriental";i['easternRapid']="Rápido oriental";i['easternRapidArena']="Torneo rápido oriental";i['easternX']=s("%s oriental");i['easternXArena']=s("Torneo %s oriental");i['eliteX']=s("%s de élite");i['eliteXArena']=s("Torneo %s de élite");i['hourlyRapid']="Rápido por hora";i['hourlyRapidArena']="Torneo rápido por hora";i['hourlyX']=s("%s por hora");i['hourlyXArena']=s("Torneo %s por hora");i['monthlyClassical']="Clásico mensual";i['monthlyClassicalArena']="Torneo clásico mensual";i['monthlyRapid']="Rápido mensual";i['monthlyRapidArena']="Torneo rápido mensual";i['monthlyX']=s("%s mensual");i['monthlyXArena']=s("Torneo %s mensual");i['rapidShield']="Escudo rápido";i['rapidShieldArena']="Torneo de escudo rápido";i['weeklyClassical']="Clásico semanal";i['weeklyClassicalArena']="Torneo clásico semanal";i['weeklyRapid']="Rápido semanal";i['weeklyRapidArena']="Torneo rápido semanal";i['weeklyX']=s("%s semanal");i['weeklyXArena']=s("Torneo %s semanal");i['xArena']=s("Torneo %s");i['xShield']=s("Escudo %s");i['xShieldArena']=s("Torneo de escudo %s");i['xTeamBattle']=s("Batalla por equipos %s");i['yearlyClassical']="Clásico anual";i['yearlyClassicalArena']="Torneo clásico anual";i['yearlyRapid']="Rápido anual";i['yearlyRapidArena']="Torneo rápido anual";i['yearlyX']=s("%s anual");i['yearlyXArena']=s("Torneo %s anual")})()