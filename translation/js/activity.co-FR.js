"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.activity)window.i18n.activity={};let i=window.i18n.activity;i['activity']="Attività";i['competedInNbSwissTournaments']=p({"one":"Hà participatu à %s ghjustra Svizzera","other":"Hà participatu à %s ghjustre Svizzere"});i['competedInNbTournaments']=p({"one":"Hà participatu à %s ghjustra Arena","other":"Hà participatu à %s ghjustre Arena"});i['completedNbGames']=p({"one":"Hà finitu %s partita per via di currispundenza","other":"Hà finitu %s partite per via di a currispundenza"});i['createdNbStudies']=p({"one":"Hà creatu %s studiu novu","other":"Hà creatu %s studii novi"});i['followedNbPlayers']=p({"one":"Seguiteghja %s ghjucadore","other":"Seguiteghja %s ghjucadori"});i['gainedNbFollowers']=p({"one":"Guadagna %s seguitore novu","other":"Guadagna %s seguitori novi"});i['hostedALiveStream']="Erate l\\' Ostu d\\' un Direttu";i['hostedNbSimuls']=p({"one":"Hà urganizatu %s mostra di partite simultanee","other":"Hà urganizatu %s mostre di partite simultanee"});i['inNbCorrespondenceGames']=p({"one":"in %1$s partita di scacchi pè via di currispundenza","other":"in %1$s partite di scacchi pè via di currispundenza"});i['joinedNbSimuls']=p({"one":"Hà participatu à %s mostra di partite simultanee","other":"Hà participatu à %s mostre di partite simultanee"});i['joinedNbTeams']=p({"one":"Hè ghjuntu in %s squadra","other":"Hè ghjuntu in %s squadre"});i['playedNbGames']=p({"one":"Hà ghjucatu %1$s partita di %2$s","other":"Hà ghjucatu %1$s partite di %2$s"});i['playedNbMoves']=p({"one":"Hà ghjucatu %1$s colpu","other":"Hà ghjucatu %1$s colpi"});i['postedNbMessages']=p({"one":"Hà mandatu %1$s messaghju in %2$s","other":"Hà mandatu %1$s messaghji in %2$s"});i['practicedNbPositions']=p({"one":"Hà risoltu %1$s pusizione in %2$s","other":"Hà risoltu %1$s pusizioni in %2$s"});i['rankedInSwissTournament']=s("Hè classificatu #%1$s in %2$s");i['rankedInTournament']=p({"one":"Hè classificatu %1$s (top%2$s%%) cù %3$s partita in %4$s","other":"Hè classificatu %1$s (top%2$s%%) cù %3$s partite in %4$s"});i['signedUp']="S\\' hè cunnettatu à lichess.org";i['solvedNbPuzzles']=p({"one":"Hà risoltu %s  di tattica","other":"Hà risoltu %s prublemi di tattica"});i['supportedNbMonths']=p({"one":"Aiuta lichess.org dapoi %1$s mese tale un %2$s","other":"Aiuta lichess.org dapoi %1$s mesi tale un %2$s"})})()