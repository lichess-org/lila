"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.activity)window.i18n.activity={};let i=window.i18n.activity;i['activity']="lo ra\\'arfau";i['competedInNbTournaments']=p({"other":"jivna fo %s lo grinunjvi"});i['createdNbStudies']=p({"other":"finti %s lo seltadni"});i['followedNbPlayers']=p({"other":"co\\'a catlu %s da"});i['gainedNbFollowers']=p({"other":"co\\'a se catlu %s da"});i['hostedALiveStream']="strimgau";i['joinedNbSimuls']=p({"other":"jivna fo %s cabysi\\'u nuntigni"});i['joinedNbTeams']=p({"other":"co\\'a cmima %s bende"});i['playedNbGames']=p({"other":"jivna fo %1$s da ci\\'e la\\'o zoi. %2$s .zoi"});i['postedNbMessages']=p({"other":"lo nu selsku fi la\\'o gy. %2$s .gy poi snustu cu zilkancu li %1$s"});i['practicedNbPositions']=p({"other":"crezenzu\\'e se pi\\'o %1$s lo nabmi be se ra\\'a %2$s"});i['signedUp']="co\\'a se jaspu fi la\\'o .urli lichess.org .urli";i['solvedNbPuzzles']=p({"other":"dafyfa\\'i %s lo nabmi be se ra\\'a kavytadji"});i['supportedNbMonths']=p({"other":"sarji la .litces. ze\\'a nu\\'i la\\'u lo masti be li %1$s ta\\'i lo nu me %2$s"})})()