"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.activity)window.i18n.activity={};let i=window.i18n.activity;i['activity']="Activity";i['competedInNbSwissTournaments']=p({"one":"Competed in %s Swiss tournament","other":"Competed in %s Swiss tournaments"});i['competedInNbTournaments']=p({"one":"Competed in %s Arena tournament","other":"Competed in %s Arena tournaments"});i['completedNbGames']=p({"one":"Completed %s correspondence game","other":"Completed %s correspondence games"});i['completedNbVariantGames']=p({"one":"Completed %1$s %2$s correspondence game","other":"Completed %1$s %2$s correspondence games"});i['createdNbStudies']=p({"one":"Created %s new study","other":"Created %s new studies"});i['followedNbPlayers']=p({"one":"Started following %s player","other":"Started following %s players"});i['gainedNbFollowers']=p({"one":"Gained %s new follower","other":"Gained %s new followers"});i['hostedALiveStream']="Hosted a live stream";i['hostedNbSimuls']=p({"one":"Hosted %s simultaneous exhibition","other":"Hosted %s simultaneous exhibitions"});i['inNbCorrespondenceGames']=p({"one":"in %1$s correspondence game","other":"in %1$s correspondence games"});i['joinedNbSimuls']=p({"one":"Participated in %s simultaneous exhibition","other":"Participated in %s simultaneous exhibitions"});i['joinedNbTeams']=p({"one":"Joined %s team","other":"Joined %s teams"});i['playedNbGames']=p({"one":"Played %1$s %2$s game","other":"Played %1$s %2$s games"});i['playedNbMoves']=p({"one":"Played %1$s move","other":"Played %1$s moves"});i['postedNbMessages']=p({"one":"Posted %1$s message in %2$s","other":"Posted %1$s messages in %2$s"});i['practicedNbPositions']=p({"one":"Practised %1$s position on %2$s","other":"Practised %1$s positions on %2$s"});i['rankedInSwissTournament']=s("Ranked #%1$s in %2$s");i['rankedInTournament']=p({"one":"Ranked #%1$s (top %2$s%%) with %3$s game in %4$s","other":"Ranked #%1$s (top %2$s%%) with %3$s games in %4$s"});i['signedUp']="Signed up to lichess.org";i['solvedNbPuzzles']=p({"one":"Solved %s training puzzle","other":"Solved %s training puzzles"});i['supportedNbMonths']=p({"one":"Supported lichess.org for %1$s month as a %2$s","other":"Supported lichess.org for %1$s months as a %2$s"})})()