"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.activity)window.i18n.activity={};let i=window.i18n.activity;i['activity']="Aktivitet";i['competedInNbSwissTournaments']=p({"one":"Deltog i %s schweizerturnering","other":"Deltog i %s schweizerturneringer"});i['competedInNbTournaments']=p({"one":"Deltog i %s turnering","other":"Deltog i %s turneringer"});i['completedNbGames']=p({"one":"Afsluttede %s korrespondanceparti","other":"Afsluttede %s korrespondancepartier"});i['completedNbVariantGames']=p({"one":"Afsluttede %1$s %2$s korrespondanceparti","other":"Afsluttede %1$s %2$s korrespondancepartier"});i['createdNbStudies']=p({"one":"Oprettede %s nyt studie","other":"Oprettede %s nye studier"});i['followedNbPlayers']=p({"one":"Begyndte at følge %s spiller","other":"Begyndte at følge %s spillere"});i['gainedNbFollowers']=p({"one":"Fik %s ny følger","other":"Fik %s nye følgere"});i['hostedALiveStream']="Hostede en livestream";i['hostedNbSimuls']=p({"one":"Var vært for %s simultanskakarrangement","other":"Var vært for %s simultanskakarrangementer"});i['inNbCorrespondenceGames']=p({"one":"i %1$s korrespondanceparti","other":"i %1$s korrespondancepartier"});i['joinedNbSimuls']=p({"one":"Deltog i %s simultanskakarrangement","other":"Deltog i %s simultanskakarrangementer"});i['joinedNbTeams']=p({"one":"Blev medlem af %s hold","other":"Blev medlem af %s hold"});i['playedNbGames']=p({"one":"Spillede %1$s %2$s parti","other":"Spillede %1$s %2$s partier"});i['playedNbMoves']=p({"one":"Spillede %1$s træk","other":"Spillede %1$s træk"});i['postedNbMessages']=p({"one":"Lavede %1$s indlæg i %2$s","other":"Lavede %1$s indlæg i %2$s"});i['practicedNbPositions']=p({"one":"Øvede %1$s stilling i %2$s","other":"Øvede %1$s stillinger i %2$s"});i['rankedInSwissTournament']=s("Rangeret #%1$s i %2$s");i['rankedInTournament']=p({"one":"Rangeret #%1$s (top %2$s%%) med %3$s parti i %4$s","other":"Rangeret #%1$s (top %2$s%%) med %3$s partier i %4$s"});i['signedUp']="Tilmeldte sig lichess.org";i['solvedNbPuzzles']=p({"one":"Løste %s taktisk opgave","other":"Løste %s taktiske opgaver"});i['supportedNbMonths']=p({"one":"Har støttet lichess.org i %1$s måneder som en %2$s","other":"Har støttet lichess.org i %1$s måneder som en %2$s"})})()