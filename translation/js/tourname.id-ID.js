"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.tourname)window.i18n.tourname={};let i=window.i18n.tourname;i['classicalShield']="Perisai Klasikal";i['classicalShieldArena']="Arena Perisai Klasikal";i['dailyClassical']="Klasikal Harian";i['dailyClassicalArena']="Arena Klasikal Harian";i['dailyRapid']="Rapid Harian";i['dailyRapidArena']="Arena Rapid Harian";i['dailyX']=s("%s Harian");i['dailyXArena']=s("Arena %s Harian");i['easternClassical']="Klasikal Ketimuran";i['easternClassicalArena']="Arena Klasikal Ketimuran";i['easternRapid']="Rapid Ketimuran";i['easternRapidArena']="Arena Rapid Ketimuran";i['easternX']=s("%s Ketimuran");i['easternXArena']=s("Arena %s Ketimuran");i['eliteX']=s("Elite %s");i['eliteXArena']=s("Arena Elite %s");i['hourlyRapid']="Rapid Sejam";i['hourlyRapidArena']="Sejam Arena Rapid";i['hourlyX']=s("%s Sejam");i['hourlyXArena']=s("Arena %s setiap jam");i['monthlyClassical']="Klasikal Bulanan";i['monthlyClassicalArena']="Arena Klasikal Bulanan";i['monthlyRapid']="Rapid Bulanan";i['monthlyRapidArena']="Arena Rapid Bulanan";i['monthlyX']=s("%s Bulanan");i['monthlyXArena']=s("Arena %s Bulanan");i['rapidShield']="Perisai Rapid";i['rapidShieldArena']="Arena Perisai Rapid";i['weeklyClassical']="Klasikal Mingguan";i['weeklyClassicalArena']="Arena Klasikal Mingguan";i['weeklyRapid']="Rapid Mingguan";i['weeklyRapidArena']="Arena Rapid Mingguan";i['weeklyX']=s("%s Mingguan");i['weeklyXArena']=s("Arena %s Mingguan");i['xArena']=s("Arena %s");i['xShield']=s("%s Perisai");i['xShieldArena']=s("Arena Perisai %s");i['xTeamBattle']=s("Pertempuran Tim %s");i['yearlyClassical']="Klasikal Tahunan";i['yearlyClassicalArena']="Arena Klasikal Tahunan";i['yearlyRapid']="Rapid Tahunan";i['yearlyRapidArena']="Arena Rapid Tahunan";i['yearlyX']=s("%s Tahunan");i['yearlyXArena']=s("Arena %s Tahunan")})()