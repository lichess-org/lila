"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.storm)window.i18n.storm={};let i=window.i18n.storm;i['accuracy']="ನಿಖರತೆ";i['allTime']="ಎಲ್ಲ ಸಮಯದಲ್ಲು";i['bestRunOfDay']="ದಿನದ ಅತ್ಯುತ್ತಮ ಓಟ";i['clickToReload']="ಮರುಲೋಡ್ ಮಾಡಲು ಕ್ಲಿಕ್ ಮಾಡಿ";i['combo']="ಕಾಂಬೊ";i['createNewGame']="ಹೊಸ ಆಟವನ್ನು ರಚಿಸಿ";i['endRun']="ಮುಕ್ತಾಯದ ಓಟ (ಹಾಟ್‌ಕೀ: ನಮೂದಿಸಿ)";i['failedPuzzles']="ವಿಫಲವಾದ ಒಗಟುಗಳು";i['getReady']="ತಯಾರಾಗು!";i['highestSolved']="ಅತಿ ಹೆಚ್ಚು ಪರಿಹರಿಸಲಾಗಿದೆ";i['highscores']="ಹೆಚ್ಚಿನ ಅಂಕಗಳು";i['highscoreX']=s("ಹೈಸ್ಕೋರ್: %s");i['joinPublicRace']="ಸಾರ್ವಜನಿಕ ಓಟಕ್ಕೆ ಸೇರಿಕೊಳ್ಳಿ";i['joinRematch']="ಮರುಪಂದ್ಯಕ್ಕೆ ಸೇರಿಕೊಳ್ಳಿ";i['joinTheRace']="ಓಟದ ಸೇರಿ!";i['moves']="ಚಲಿಸುತ್ತದೆ";i['moveToStart']="ಪ್ರಾರಂಭಿಸಲು ಸರಿಸಿ";i['newAllTimeHighscore']="ಹೊಸ ಸಾರ್ವಕಾಲಿಕ ಹೈಸ್ಕೋರ್!";i['newDailyHighscore']="ಹೊಸ ದೈನಂದಿನ ಹೈಸ್ಕೋರ್!";i['newMonthlyHighscore']="ಹೊಸ ಮಾಸಿಕ ಹೈಸ್ಕೋರ್!";i['newRun']="ಹೊಸ ಓಟ (ಹಾಟ್‌ಕೀ: ಸ್ಪೇಸ್)";i['newWeeklyHighscore']="ಹೊಸ ಸಾಪ್ತಾಹಿಕ ಹೈಸ್ಕೋರ್!";i['nextRace']="ಮುಂದಿನ ಓಟ";i['playAgain']="ಪುನಃ ಆಡು";i['playedNbRunsOfPuzzleStorm']=p({"one":"%2$s ನ ಒಂದು ಓಟವನ್ನು ಆಡಿದ್ದಾರೆ","other":"%2$s ನ %1$s ರನ್‌ಗಳನ್ನು ಆಡಲಾಗಿದೆ"});i['previousHighscoreWasX']=s("ಹಿಂದಿನ ಹೈಸ್ಕೋರ್ %s ಆಗಿತ್ತು");i['puzzlesPlayed']="ಪದಬಂಧ ಆಡಿದರು";i['puzzlesSolved']="ಒಗಟುಗಳನ್ನು ಪರಿಹರಿಸಲಾಗಿದೆ";i['raceComplete']="ರೇಸ್ ಪೂರ್ಣಗೊಂಡಿದೆ!";i['raceYourFriends']="ನಿಮ್ಮ ಸ್ನೇಹಿತರನ್ನು ರೇಸ್ ಮಾಡಿ";i['runs']="ರನ್s";i['score']="ಸ್ಕೋರ್";i['skip']="ಬಿಟ್ಟುಬಿಡಿ";i['skipExplanation']="ನಿಮ್ಮ ಕಾಂಬೊವನ್ನು ಸಂರಕ್ಷಿಸಲು ಈ ಕ್ರಮವನ್ನು ಬಿಟ್ಟುಬಿಡಿ! ಪ್ರತಿ ಜನಾಂಗಕ್ಕೆ ಒಮ್ಮೆ ಮಾತ್ರ ಕೆಲಸ ಮಾಡುತ್ತದೆ.";i['skipHelp']="ನೀವು ಪ್ರತಿ ರೇಸ್‌ಗೆ ಒಂದು ಚಲನಚಿತ್ರವನ್ನು ಬಿಟ್ಟುಬಿಡಬಹುದು:";i['skippedPuzzle']="ತಪ್ಪಿಸಿದ ಒಗಟು";i['slowPuzzles']="ನಿಧಾನ ಒಗಟುಗಳು";i['spectating']="ವೀಕ್ಷಿಸುತ್ತಿದೆ";i['startTheRace']="ಓಟವನ್ನು ಪ್ರಾರಂಭಿಸಿ";i['thisMonth']="ಈ ತಿಂಗಳು";i['thisRunHasExpired']="ಈ ಓಟದ ಅವಧಿ ಮುಗಿದಿದೆ!";i['thisRunWasOpenedInAnotherTab']="ಈ ರನ್ ಅನ್ನು ಮತ್ತೊಂದು ಟ್ಯಾಬ್‌ನಲ್ಲಿ ತೆರೆಯಲಾಗಿದೆ!";i['thisWeek']="ಈ ವಾರ";i['time']="ಸಮಯ";i['timePerMove']="ಪ್ರತಿ ಚಲನೆಗೆ ಸಮಯ";i['viewBestRuns']="ಉತ್ತಮ ರನ್‌ಗಳನ್ನು ವೀಕ್ಷಿಸಿ";i['waitForRematch']="ಮರುಪಂದ್ಯಕ್ಕಾಗಿ ನಿರೀಕ್ಷಿಸಿ";i['waitingForMorePlayers']="ಇನ್ನಷ್ಟು ಆಟಗಾರರು ಸೇರಲು ನಿರೀಕ್ಷಿಸಲಾಗುತ್ತಿದೆ...";i['waitingToStart']="ಪ್ರಾರಂಭಿಸಲು ಕಾಯಲಾಗುತ್ತಿದೆ";i['xRuns']=p({"one":"1 ರನ್","other":"%s ರನ್‌ಗಳು"});i['youPlayTheBlackPiecesInAllPuzzles']="ನೀವು ಎಲ್ಲಾ ಒಗಟುಗಳಲ್ಲಿ ಕಪ್ಪು ತುಣುಕುಗಳನ್ನು ಆಡುತ್ತೀರಿ";i['youPlayTheWhitePiecesInAllPuzzles']="ನೀವು ಎಲ್ಲಾ ಒಗಟುಗಳಲ್ಲಿ ಬಿಳಿ ತುಣುಕುಗಳನ್ನು ಆಡುತ್ತೀರಿ";i['yourRankX']=s("ನಿಮ್ಮ ಶ್ರೇಣಿ: %s")})()