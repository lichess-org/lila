"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.tourname)window.i18n.tourname={};let i=window.i18n.tourname;i['classicalShield']="クラシカル シールド戦";i['classicalShieldArena']="クラシカル シールド戦アリーナ";i['dailyClassical']="毎日のクラシカル";i['dailyClassicalArena']="毎日のクラシカル アリーナ";i['dailyRapid']="毎日のラピッド";i['dailyRapidArena']="毎日のラピッド アリーナ";i['dailyX']=s("毎日の %s");i['dailyXArena']=s("毎日の %s アリーナ");i['easternClassical']="東半球クラシカル";i['easternClassicalArena']="東半球クラシカル アリーナ";i['easternRapid']="東半球ラピッド";i['easternRapidArena']="東半球ラピッド アリーナ";i['easternX']=s("東半球 %s");i['easternXArena']=s("東半球 %s アリーナ");i['eliteX']=s("エリート %s");i['eliteXArena']=s("エリート %s アリーナ");i['hourlyRapid']="毎時のラピッド";i['hourlyRapidArena']="毎時のラピッド アリーナ";i['hourlyX']=s("毎時の %s");i['hourlyXArena']=s("毎時の %s アリーナ");i['monthlyClassical']="毎月のクラシカル";i['monthlyClassicalArena']="毎月のクラシカル アリーナ";i['monthlyRapid']="毎月のラピッド";i['monthlyRapidArena']="毎月のラピッド アリーナ";i['monthlyX']=s("毎月の %s");i['monthlyXArena']=s("毎月の %s アリーナ");i['rapidShield']="ラピッド シールド戦";i['rapidShieldArena']="ラピッド シールド戦アリーナ";i['weeklyClassical']="毎週のクラシカル";i['weeklyClassicalArena']="毎週のクラシカル アリーナ";i['weeklyRapid']="毎週のラピッド";i['weeklyRapidArena']="毎週のラピッド アリーナ";i['weeklyX']=s("毎週の %s");i['weeklyXArena']=s("毎週の %s アリーナ");i['xArena']=s("%s アリーナ");i['xShield']=s("%s シールド戦");i['xShieldArena']=s("%s シールド戦アリーナ");i['xTeamBattle']=s("%s チームバトル");i['yearlyClassical']="毎年のクラシカル";i['yearlyClassicalArena']="毎年のクラシカル アリーナ";i['yearlyRapid']="毎年のラピッド";i['yearlyRapidArena']="毎年のラピッド アリーナ";i['yearlyX']=s("毎年の %s");i['yearlyXArena']=s("毎年の %s アリーナ")})()