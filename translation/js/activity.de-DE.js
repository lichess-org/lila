"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.activity)window.i18n.activity={};let i=window.i18n.activity;i['activity']="Verlauf";i['competedInNbSwissTournaments']=p({"one":"Hat an %s Turnier nach Schweizer System teilgenommen","other":"Hat an %s Turnieren nach Schweizer System teilgenommen"});i['competedInNbTournaments']=p({"one":"Hat an %s Turnier teilgenommen","other":"Hat an %s Turnieren teilgenommen"});i['completedNbGames']=p({"one":"Hat %s Fernschachpartie gespielt","other":"Hat %s Fernschachpartien gespielt"});i['completedNbVariantGames']=p({"one":"Hat %1$s %2$s-Fernschachpartie gespielt","other":"Hat %1$s %2$s-Fernschachpartien gespielt"});i['createdNbStudies']=p({"one":"Hat %s neue Studie erstellt","other":"Hat %s neue Studien erstellt"});i['followedNbPlayers']=p({"one":"Folgt %s Spieler","other":"Folgt %s Spielern"});i['gainedNbFollowers']=p({"one":"Hat %s neuen Follower","other":"Hat %s neue Follower"});i['hostedALiveStream']="Hat live gestreamt";i['hostedNbSimuls']=p({"one":"Hat %s Simultanvorstellung gegeben","other":"Hat %s Simultanvorstellungen gegeben"});i['inNbCorrespondenceGames']=p({"one":"in %1$s Fernschachpartie","other":"in %1$s Fernschachpartien"});i['joinedNbSimuls']=p({"one":"Hat an %s Simultanvorstellung teilgenommen","other":"Hat an %s Simultanvorstellungen teilgenommen"});i['joinedNbTeams']=p({"one":"Ist %s Team beigetreten","other":"Ist %s Teams beigetreten"});i['playedNbGames']=p({"one":"Hat %1$s Partie %2$s gespielt","other":"Hat %1$s Partien %2$s gespielt"});i['playedNbMoves']=p({"one":"Spielte %1$s Zug","other":"Spielte %1$s Züge"});i['postedNbMessages']=p({"one":"Hat %1$s Nachricht in %2$s geschrieben","other":"Hat %1$s Nachrichten in %2$s geschrieben"});i['practicedNbPositions']=p({"one":"Hat %1$s Stellung bei %2$s geübt","other":"Hat %1$s Stellungen bei %2$s geübt"});i['rankedInSwissTournament']=s("Hat Platz #%1$s im Turnier %2$s belegt");i['rankedInTournament']=p({"one":"Hat Platz #%1$s (obere %2$s%%) mit %3$s Spiel in %4$s erreicht","other":"Hat Platz #%1$s (obere %2$s%%) mit %3$s Spielen in %4$s erreicht"});i['signedUp']="Hat sich bei Lichess angemeldet";i['solvedNbPuzzles']=p({"one":"Hat %s Taktikaufgabe gelöst","other":"Hat %s Taktikaufgaben gelöst"});i['supportedNbMonths']=p({"one":"Unterstützt lichess.org seit %1$s Monat als %2$s","other":"Unterstützt lichess.org seit %1$s Monaten als %2$s"})})()