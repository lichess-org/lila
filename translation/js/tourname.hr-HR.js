"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.tourname)window.i18n.tourname={};let i=window.i18n.tourname;i['classicalShield']="Klasični Štit";i['classicalShieldArena']="Klasična Štitna Arena";i['dailyClassical']="Dnevni Klasični Šah";i['dailyClassicalArena']="Dnevna Klasična Arena";i['dailyRapid']="Dnevni Rapid";i['dailyRapidArena']="Dnevna Rapidna Arena";i['dailyX']=s("Dnevni %s");i['dailyXArena']=s("Dnevna %s Arena");i['easternClassical']="Istočni Klasični Šah";i['easternClassicalArena']="Istočna Klasična Arena";i['easternRapid']="Istočni Rapid";i['easternRapidArena']="Istočni Rapidna Arena";i['easternX']=s("Istočni %s");i['easternXArena']=s("Istočna %s Arena");i['eliteX']=s("Elitni %s");i['eliteXArena']=s("Elitna %s Arena");i['hourlyRapid']="Satni Rapid";i['hourlyRapidArena']="Satna Rapidna Arena";i['hourlyX']=s("Satni %s");i['hourlyXArena']=s("Satna %s Arena");i['monthlyClassical']="Mjesečni Klasični Šah";i['monthlyClassicalArena']="Mjesečna Klasična Arena";i['monthlyRapid']="Mjesečni Rapid";i['monthlyRapidArena']="Mjesečna Rapidna Arena";i['monthlyX']=s("Mjesečni %s");i['monthlyXArena']=s("Mjesečna %s Arena");i['rapidShield']="Rapidni Štit";i['rapidShieldArena']="Rapidna Štitna Arena";i['weeklyClassical']="Tjedni Klasični Šah";i['weeklyClassicalArena']="Tjedna Klasična Arena";i['weeklyRapid']="Tjedni Rapid";i['weeklyRapidArena']="Tjedna Rapidna Arena";i['weeklyX']=s("Tjedni %s");i['weeklyXArena']=s("Tjedna %s Arena");i['xArena']=s("%s Arena");i['xShield']=s("%s Štit");i['xShieldArena']=s("%s Štitna Arena");i['xTeamBattle']=s("%s Timska Bitka");i['yearlyClassical']="Godišnji Klasični Šah";i['yearlyClassicalArena']="Godišnja Klasična Arena";i['yearlyRapid']="Godišnji Rapid";i['yearlyRapidArena']="Godišnja Rapidna Arena";i['yearlyX']=s("Godišnji %s");i['yearlyXArena']=s("Godišnja %s Arena")})()