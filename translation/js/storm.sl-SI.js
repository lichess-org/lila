"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.storm)window.i18n.storm={};let i=window.i18n.storm;i['accuracy']="Točnost";i['allTime']="Ves čas";i['bestRunOfDay']="Najboljša igra dneva";i['clickToReload']="Klikni za ponovno nalaganje";i['combo']="Zaporedje pravilnih potez";i['createNewGame']="Ustvari novo igro";i['endRun']="Konec igre (bližnjična tipka: vnašalka)";i['failedPuzzles']="Neuspele uganke";i['getReady']="Pripravite se!";i['highestSolved']="Rejting najtežje pravilno rešene uganke";i['highscores']="Nojboljši rezultati";i['highscoreX']=s("Rekord: %s");i['joinPublicRace']="Pridružite se javni tekmi";i['joinRematch']="Pridružite se revanši";i['joinTheRace']="Pridruži se dirki!";i['moves']="Poteze";i['moveToStart']="Premaknite za začetek";i['newAllTimeHighscore']="Nov rekord vseh časov!";i['newDailyHighscore']="Nov dnevni rekord!";i['newMonthlyHighscore']="Nov mesečni rekord!";i['newRun']="Nova igra (bližnjična tipka: preslednica)";i['newWeeklyHighscore']="Nov tedenski rekord!";i['nextRace']="Naslednja dirka";i['playAgain']="Igrajte ponovno";i['playedNbRunsOfPuzzleStorm']=p({"one":"Opravljen en poskus %2$s","two":"Opravljena %1$s poskusa %2$s","few":"Opravljeni %1$s poskusi %2$s","other":"Opravljenih %1$s poskusov %2$s"});i['previousHighscoreWasX']=s("Prejšnji rekord je bil %s");i['puzzlesPlayed']="Igrane uganke";i['puzzlesSolved']="uganke rešene";i['raceComplete']="Dirka končana!";i['raceYourFriends']="Dirka s prijatelji";i['runs']="Igre";i['score']="Rezultat";i['skip']="preskoči";i['skipExplanation']="Preskočite to potezo, da ohranite svojo kombinacijo! Deluje samo enkrat na dirko.";i['skipHelp']="NOVO! Lahko preskočite eno potezo na dirko:";i['slowPuzzles']="Počasne uganke";i['spectating']="Gledanje";i['startTheRace']="Začnite dirko";i['thisMonth']="Ta mesec";i['thisRunHasExpired']="Ta tek je potekel!";i['thisRunWasOpenedInAnotherTab']="Ta tek je odprt v drugem zavihku!";i['thisWeek']="Te teden";i['time']="Čas";i['timePerMove']="Čas na potezo";i['viewBestRuns']="Ogled najboljših iger";i['waitForRematch']="Počakajte na revanšo";i['waitingForMorePlayers']="Čakanje, da se pridruži več igralcev...";i['waitingToStart']="Čakanje na začetek";i['xRuns']=p({"one":"1 poskus","two":"%s poskusa","few":"%s poskusi","other":"%s poskusov"});i['youPlayTheBlackPiecesInAllPuzzles']="Črne figure igrate v vseh ugankah";i['youPlayTheWhitePiecesInAllPuzzles']="Bele figure igrate v vseh ugankah";i['yourRankX']=s("Tvoj rang: %s")})()