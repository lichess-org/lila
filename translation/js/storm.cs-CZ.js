"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.storm)window.i18n.storm={};let i=window.i18n.storm;i['accuracy']="Přesnost";i['allTime']="Za celou dobu";i['bestRunOfDay']="Nejlepší pokus dne";i['clickToReload']="Kliknutím obnovte";i['combo']="Kombo";i['createNewGame']="Vytvořit nový závod";i['endRun']="Ukončit pokus (klávesa: Enter)";i['failedPuzzles']="Neúspěšné úlohy";i['getReady']="Připravte se!";i['highestSolved']="Nejtěžší vyřešená úloha";i['highscores']="Nejvyšší skóre";i['highscoreX']=s("Nejvyšší skóre: %s");i['joinPublicRace']="Připojit se k veřejnému závodu";i['joinRematch']="Připojit se k odvetě";i['joinTheRace']="Připojte se k závodu!";i['moves']="Tahy";i['moveToStart']="Zahrajte tah pro zahájení";i['newAllTimeHighscore']="Nový osobní rekord!";i['newDailyHighscore']="Nové denní maximum!";i['newMonthlyHighscore']="Nové měsíční maximum!";i['newRun']="Nový pokus (klávesa: Mezerník)";i['newWeeklyHighscore']="Nové týdenní maximum!";i['nextRace']="Další závod";i['playAgain']="Hrát znovu";i['playedNbRunsOfPuzzleStorm']=p({"one":"Odehrán jeden %2$s","few":"Odehrány %1$s pokusy %2$s","many":"Odehráno %1$s her %2$s","other":"Odehráno %1$s běhů z %2$s"});i['previousHighscoreWasX']=s("Předchozí osobní rekord byl %s");i['puzzlesPlayed']="Přehrané úlohy";i['puzzlesSolved']="úloh vyřešeno";i['raceComplete']="Závod ukončen!";i['raceYourFriends']="Závodit s přáteli";i['runs']="Pokusy";i['score']="Skóre";i['skip']="přeskočit";i['skipExplanation']="Přeskočte tento tah pro zachování své série! Funguje pouze jednou za závod.";i['skipHelp']="V každém závodě můžete přeskočit jeden tah:";i['skippedPuzzle']="Přeskočené puzzle";i['slowPuzzles']="Pomalé úlohy";i['spectating']="Sledování";i['startTheRace']="Zahájit závod";i['thisMonth']="Tento měsíc";i['thisRunHasExpired']="Tento závod byl již smazán!";i['thisRunWasOpenedInAnotherTab']="Váš závod byl otevřen na další stránce!";i['thisWeek']="Tento týden";i['time']="Čas";i['timePerMove']="Čas na tah";i['viewBestRuns']="Zobrazit nejlepší pokusy";i['waitForRematch']="Počkejte na další kolo";i['waitingForMorePlayers']="Čeká se na připojení dalších hráčů...";i['waitingToStart']="Čeká se na zahájení";i['xRuns']=p({"one":"1 pokus","few":"%s pokusy","many":"%s pokusů","other":"%s pokusů"});i['youPlayTheBlackPiecesInAllPuzzles']="Ve všech úlohách máte černé figury";i['youPlayTheWhitePiecesInAllPuzzles']="Ve všech úlohách máte bílé figury";i['yourRankX']=s("Pořadí v závodu: %s")})()