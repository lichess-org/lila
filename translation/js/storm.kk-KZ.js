"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.storm)window.i18n.storm={};let i=window.i18n.storm;i['accuracy']="Дәлдік";i['allTime']="Жалпы";i['bestRunOfDay']="Бір күннің үздік кезеңі";i['clickToReload']="Қайта бастау";i['combo']="Комбо";i['createNewGame']="Жаңа ойын құру";i['endRun']="Кезеңді аяқтау (жылдам перне: Enter)";i['failedPuzzles']="Шешілмеген жұмбақтар";i['getReady']="Дайынсыз ба!";i['highestSolved']="Шешілген ең жоғарғы рейтинг";i['highscores']="Рекордтар";i['highscoreX']=s("Рекорд: %s");i['joinPublicRace']="Жалпыға ашық бәйгеге қосылу";i['joinRematch']="Қайта ойнаға қосылу";i['joinTheRace']="Бәйгеге қосылу!";i['moves']="Жүрістер";i['moveToStart']="Бастау үшін жүріңіз";i['newAllTimeHighscore']="Жаңа рекорд!";i['newDailyHighscore']="Жаңа күндік рекорд!";i['newMonthlyHighscore']="Жаңа айлық рекорд!";i['newRun']="Жаңа кезең (жылдам перне: бос орын)";i['newWeeklyHighscore']="Жаңа апталық рекорд!";i['nextRace']="Келесі бәйге";i['playAgain']="Қайтадан ойнау";i['playedNbRunsOfPuzzleStorm']=p({"one":"%2$s кезеңнің бірі ойналды","other":"%2$s кезеңнің %1$s ойналды"});i['previousHighscoreWasX']=s("Алдыңғы рекорд %s");i['puzzlesPlayed']="Ойналған жұмбақтар";i['puzzlesSolved']="жұмбақ шешілді";i['raceComplete']="Бәйге аяқталды!";i['raceYourFriends']="Достармен бәйгеге түсу";i['runs']="Кезеңдер";i['score']="Ұпай";i['skip']="бас тарту";i['skipExplanation']="Өз тізбегіңізді сақтау үшін осы жүрістен бас тарту! Әр бәйгеде бір рет.";i['skipHelp']="Сіз әр бәйгеде бір жүрістен ғана бас тарта аласыз:";i['skippedPuzzle']="Қараусыз жұмбақ";i['slowPuzzles']="Баяу жұмбақтар";i['spectating']="Көріп отырғандар";i['startTheRace']="Бәйге бастау";i['thisMonth']="Осы ай";i['thisRunHasExpired']="Бұл бәйгенің мерзімі бітті!";i['thisRunWasOpenedInAnotherTab']="Бұл бәйге басқа бетте ашылған!";i['thisWeek']="Осы апта";i['time']="Уақыт";i['timePerMove']="Бір жүріс уақыты";i['viewBestRuns']="Үздік кезеңдерді көру";i['waitForRematch']="Қайта ойнауды күту";i['waitingForMorePlayers']="Көбірек ойыншылардың қосылуын күтеміз...";i['waitingToStart']="Басталуын күтеміз";i['xRuns']=p({"one":"1 кезең","other":"%s кезең"});i['youPlayTheBlackPiecesInAllPuzzles']="Барлық жұмбақтарда сіз қара түспен ойнайсыз";i['youPlayTheWhitePiecesInAllPuzzles']="Барлық жұмбақтарда сіз ақ түспен ойнайсыз";i['yourRankX']=s("Сіздің орныңыз: %s")})()