"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.activity)window.i18n.activity={};let i=window.i18n.activity;i['activity']="Veikla";i['competedInNbSwissTournaments']=p({"one":"Dalyvavo %s šveicariškame turnyre","few":"Dalyvavo %s šveicariškuose turnyruose","many":"Dalyvavo %s šveicariško turnyro","other":"Dalyvavo %s šveicariškų turnyrų"});i['competedInNbTournaments']=p({"one":"Varžėsi %s-ame turnyre","few":"Varžėsi %s-uose turnyruose","many":"Varžėsi %s-yje turnyrų","other":"Varžėsi %s-yje turnyrų"});i['completedNbGames']=p({"one":"Užbaigė %s korespondencinę partiją","few":"Užbaigė %s korespondencines partijas","many":"Užbaigė %s korespondencinių partijų","other":"Užbaigė %s korespondencinių partijų"});i['createdNbStudies']=p({"one":"Sukūrė %s naują studiją","few":"Sukūrė %s naujas studijas","many":"Sukūrė %s naujų studijų","other":"Sukūrė %s naujų studijų"});i['followedNbPlayers']=p({"one":"Pradėjo sekti %s žaidėją","few":"Pradėjo sekti %s žaidėjus","many":"Pradėjo sekti %s žaidėjų","other":"Pradėjo sekti %s žaidėjų"});i['gainedNbFollowers']=p({"one":"Sulaukė %s sekėjo","few":"Sulaukė %s sekėjų","many":"Sulaukė %s sekėjų","other":"Sulaukė %s sekėjų"});i['hostedALiveStream']="Organizavo transliaciją";i['hostedNbSimuls']=p({"one":"Organizavo %s simulą","few":"Organizavo %s simulus","many":"Organizavo %s simulų","other":"Organizavo %s simulų"});i['inNbCorrespondenceGames']=p({"one":"per %1$s korespondencinę partiją","few":"per %1$s korespondencines partijas","many":"per %1$s korespondencinių partijų","other":"per %1$s korespondencinių partijų"});i['joinedNbSimuls']=p({"one":"Dalyvavo %s-me simule","few":"Dalyvavo %s-se simuluose","many":"Dalyvavo %s-yje simulų","other":"Dalyvavo %s-yje simulų"});i['joinedNbTeams']=p({"one":"Prisijungė prie %s komandos","few":"Prisijungė prie %s komandų","many":"Prisijungė prie %s komandų","other":"Prisijungė prie %s komandų"});i['playedNbGames']=p({"one":"Sužaidė %1$s „%2$s“ partiją","few":"Sužaidė %1$s „%2$s“ partijas","many":"Sužaidė %1$s „%2$s“ partijų","other":"Sužaidė %1$s „%2$s“ partijų"});i['playedNbMoves']=p({"one":"Sužaidė %1$s ėjimą","few":"Sužaidė %1$s ėjimus","many":"Sužaidė %1$s ėjimų","other":"Sužaidė %1$s ėjimų"});i['postedNbMessages']=p({"one":"Parašė %1$s žinutę temoje „%2$s“","few":"Parašė %1$s žinutes temoje „%2$s“","many":"Parašė %1$s žinučių temoje „%2$s“","other":"Parašė %1$s žinučių temoje „%2$s“"});i['practicedNbPositions']=p({"one":"Praktikavosi %1$s poziciją per „%2$s“","few":"Praktikavosi %1$s pozicijas per „%2$s“","many":"Praktikavosi %1$s pozicijų per „%2$s“","other":"Praktikavosi %1$s pozicijų per „%2$s“"});i['rankedInSwissTournament']=s("Reitinguotas #%1$s iš %2$s");i['rankedInTournament']=p({"one":"Užėmė #%1$s (tarp %2$s%% geriausiųjų) su %3$s partija, žaidžiant „%4$s“","few":"Užėmė #%1$s (tarp %2$s%% geriausiųjų) su %3$s partijomis, žaidžiant „%4$s“","many":"Užėmė #%1$s (tarp %2$s%% geriausiųjų) su %3$s partijų, žaidžiant „%4$s“","other":"Užėmė #%1$s (tarp %2$s%% geriausiųjų) su %3$s partijų, žaidžiant „%4$s“"});i['signedUp']="Užsiregistravo „Lichess“";i['solvedNbPuzzles']=p({"one":"Išsprendė %s taktinę užduotį","few":"Išsprendė %s taktines užduotis","many":"Išsprendė %s taktinių užduočių","other":"Išsprendė %s taktinių užduočių"});i['supportedNbMonths']=p({"one":"Remia lichess.org %1$s mėn. kaip „%2$s“","few":"Remia lichess.org %1$s mėn. kaip „%2$s“","many":"Remia lichess.org %1$s mėn. kaip „%2$s“","other":"Remia lichess.org %1$s mėn. kaip „%2$s“"})})()