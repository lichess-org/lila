"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.storm)window.i18n.storm={};let i=window.i18n.storm;i['accuracy']="Tikslumas";i['allTime']="Visų laikų";i['bestRunOfDay']="Geriausia dienos eilė";i['clickToReload']="Spauskite norėdami perkrauti";i['combo']="Iš eilės";i['createNewGame']="Kurti naują žaidimą";i['endRun']="Baigti eilę (klavišas: įvesti)";i['failedPuzzles']="Nepavykę galvosūkiai";i['getReady']="Pasiruoškite!";i['highestSolved']="Daugiausia išspręsta";i['highscores']="Geriausi rezultatai";i['highscoreX']=s("Rekordas: %s");i['joinPublicRace']="Prisijungti prie viešų lenktynių";i['joinRematch']="Prisijungti revanšui";i['joinTheRace']="Prisijungti prie lenktynių!";i['moves']="Ėjimų";i['moveToStart']="Norėdami pradėti padarykite ėjimą";i['newAllTimeHighscore']="Naujas visų laikų rekordas!";i['newDailyHighscore']="Naujas kasdieninis rekordas!";i['newMonthlyHighscore']="Naujas mėnesinis rekordas!";i['newRun']="Nauja eilė (klavišas: tarpas)";i['newWeeklyHighscore']="Naujas savaitinis rekordas!";i['nextRace']="Kitos lenktynės";i['playAgain']="Žaisti dar";i['playedNbRunsOfPuzzleStorm']=p({"one":"Žaista viena %2$s eilė","few":"Žaistos %1$s %2$s eilės","many":"Žaista %1$s %2$s eilės","other":"Žaista %1$s %2$s eilių"});i['previousHighscoreWasX']=s("Praėjęs rekordas buvo %s");i['puzzlesPlayed']="Sužaista galvosūkių";i['puzzlesSolved']="išspręsta užduočių";i['raceComplete']="Lenktynės baigtos!";i['raceYourFriends']="Lenktyniauti su draugais";i['runs']="Eilės";i['score']="Taškų";i['skip']="praleisti";i['skipExplanation']="Praleiskite ėjimą norėdami išlaikyti seką! Veikia tik kartą per lenktynes.";i['skipHelp']="NAUJA! Per lenktynes galite praleisti vieną ėjimą:";i['skippedPuzzle']="Praleista užduotis";i['slowPuzzles']="Lėti galvosūkiai";i['spectating']="Stebima";i['startTheRace']="Pradėti lenktynes";i['thisMonth']="Šį mėnesį";i['thisRunHasExpired']="Ši eilė pasibaigė!";i['thisRunWasOpenedInAnotherTab']="Ši eilė atidaryta kitoje kortelėje!";i['thisWeek']="Šią savaitę";i['time']="Laikas";i['timePerMove']="Laiko per ėjimą";i['viewBestRuns']="Peržiūrėti geriausias eiles";i['waitForRematch']="Laukti revanšo";i['waitingForMorePlayers']="Laukiama daugiau žaidėjų...";i['waitingToStart']="Laukiama pradžios";i['xRuns']=p({"one":"1 eilė","few":"%s eilės","many":"%s eilės","other":"%s eilių"});i['youPlayTheBlackPiecesInAllPuzzles']="Visose užduotyse žaidžiate juodaisiais";i['youPlayTheWhitePiecesInAllPuzzles']="Visose užduotyse žaidžiate baltaisiais";i['yourRankX']=s("Jūsų reitingas: %s")})()