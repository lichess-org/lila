"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.storm)window.i18n.storm={};let i=window.i18n.storm;i['accuracy']="Úspešnosť";i['allTime']="Celkovo";i['bestRunOfDay']="Najlepšie kolo dňa";i['clickToReload']="Kliknite pre opätovné načítanie";i['combo']="Kombo";i['createNewGame']="Vytvoriť nové preteky";i['endRun']="Ukončiť kolo (kláves. skratka: Enter)";i['failedPuzzles']="Neúspešne riešené úlohy";i['getReady']="Pripravte sa!";i['highestSolved']="Najťažšia vyriešená úloha";i['highscores']="Najvyššie skóre";i['highscoreX']=s("Najvyššie skóre: %s");i['joinPublicRace']="Zapojiť sa do verejných pretekov";i['joinRematch']="Zapojiť sa do odvety";i['joinTheRace']="Zapojte sa do pretekov!";i['moves']="Počet ťahov";i['moveToStart']="Odštartujte potiahnutím";i['newAllTimeHighscore']="Nové celkové najvyššie skóre!";i['newDailyHighscore']="Nové najvyššie skóre dňa!";i['newMonthlyHighscore']="Nové najvyššie skóre mesiaca!";i['newRun']="Nové kolo (kláves. skratka: Medzerník)";i['newWeeklyHighscore']="Nové najvyššie skóre týždňa!";i['nextRace']="Ďalšie preteky";i['playAgain']="Hrať znova";i['playedNbRunsOfPuzzleStorm']=p({"one":"Odohrané jedeno kolo %2$s","few":"Obohrané %1$s kolá %2$s","many":"Odohraných %1$s kôl %2$s","other":"Odohraných %1$s kôl %2$s"});i['previousHighscoreWasX']=s("Predchádzajúce najvyššie skóre bolo %s");i['puzzlesPlayed']="Riešené úlohy";i['puzzlesSolved']="vyriešených úloh";i['raceComplete']="Preteky ukončené!";i['raceYourFriends']="Pretekajte sa s priateľmi";i['runs']="Kolá";i['score']="Skóre";i['skip']="preskočiť";i['skipExplanation']="Vynechajte tento ťah aby ste neprišli o kombo! Je možné len raz počas pretekov.";i['skipHelp']="Počas každých pretekov môžete raz vynechať ťah:";i['skippedPuzzle']="Preskočené úlohy";i['slowPuzzles']="Pomaly riešené úlohy";i['spectating']="Divák";i['startTheRace']="Odštartovať preteky";i['thisMonth']="Tento mesiac";i['thisRunHasExpired']="Čas tohto kola vypršal!";i['thisRunWasOpenedInAnotherTab']="Toto kolo ste otvorili v inej záložke!";i['thisWeek']="Tento týždeň";i['time']="Čas";i['timePerMove']="Čas na ťah";i['viewBestRuns']="Prezrieť najlepšie kolá";i['waitForRematch']="Čakať na odvetu";i['waitingForMorePlayers']="Čaká sa kým sa pripojí viac hráčov...";i['waitingToStart']="Čaká sa na štart";i['xRuns']=p({"one":"1 kolo","few":"%s kolá","many":"%s kôl","other":"%s kôl"});i['youPlayTheBlackPiecesInAllPuzzles']="Vo všetkých úlohách ste čierny";i['youPlayTheWhitePiecesInAllPuzzles']="Vo všetkých úlohách ste biely";i['yourRankX']=s("Vaše miesto: %s")})()