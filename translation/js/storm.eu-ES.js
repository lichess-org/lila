"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.storm)window.i18n.storm={};let i=window.i18n.storm;i['accuracy']="Zehaztasuna";i['allTime']="Hasieratik";i['bestRunOfDay']="Eguneko saiakera onena";i['clickToReload']="Egin klik berriz kargatzeko";i['combo']="Jarraiak";i['createNewGame']="Sortu partida berria";i['endRun']="Amaitu saiakera (enter)";i['failedPuzzles']="Huts egindako ariketak";i['getReady']="Prest!";i['highestSolved']="Ebatzitako altuena";i['highscores']="Puntuazio altuenak";i['highscoreX']=s("Marka: %s");i['joinPublicRace']="Sartu lasterketa publikora";i['joinRematch']="Sartu berriz jokatzera";i['joinTheRace']="Sartu lasterketara!";i['moves']="Jokaldiak";i['moveToStart']="Mugitu hasteko";i['newAllTimeHighscore']="Marka berria!";i['newDailyHighscore']="Eguneko marka berria!";i['newMonthlyHighscore']="Hileko marka berria!";i['newRun']="Saiakera berria (espazioa)";i['newWeeklyHighscore']="Asteko marka berria!";i['nextRace']="Hurrengo lasterketa";i['playAgain']="Jokatu berriz";i['playedNbRunsOfPuzzleStorm']=p({"one":"%2$s ariketaren saiakera bat egin duzu","other":"%2$s ariketaren %1$s saiakera egin dituzu"});i['previousHighscoreWasX']=s("Aurreko marka %s zen");i['puzzlesPlayed']="Jokatutako ariketak";i['puzzlesSolved']="ariketa ebatzita";i['raceComplete']="Lasterketa amaitu da!";i['raceYourFriends']="Jokatu zure lagunekin";i['runs']="Saiakerak";i['score']="Puntuazioa";i['skip']="salto egin";i['skipExplanation']="Jokaldi hau saltatu zure bolada mantentzeko! Lasterketa bakoitzean behin bakarrik erabili dezakezu.";i['skipHelp']="Lasterketa bakoitzean jokaldi bat saltatu dezakezu:";i['skippedPuzzle']="Salto egindako ariketa";i['slowPuzzles']="Ariketa geldoak";i['spectating']="Ikusten";i['startTheRace']="Hasi lasterketa";i['thisMonth']="Hilabete honetan";i['thisRunHasExpired']="Lasterketa hau iraungi egin da!";i['thisRunWasOpenedInAnotherTab']="Lasterketa hau beste fitxa baten zabaldu da!";i['thisWeek']="Aste honetan";i['time']="Denbora";i['timePerMove']="Jokaldiko denbora";i['viewBestRuns']="Ikusi saiakera onenak";i['waitForRematch']="Itxaron berriz jokatzeko";i['waitingForMorePlayers']="Jokalari gehiago sartzeko zain...";i['waitingToStart']="Hasteko zain";i['xRuns']=p({"one":"Saiakera 1","other":"%s saiakera"});i['youPlayTheBlackPiecesInAllPuzzles']="Ariketa guztietan pieza beltzekin jokatuko duzu";i['youPlayTheWhitePiecesInAllPuzzles']="Ariketa guztietan pieza zuriekin jokatuko duzu";i['yourRankX']=s("Zure sailkapena: %s")})()