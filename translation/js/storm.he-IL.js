"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.storm)window.i18n.storm={};let i=window.i18n.storm;i['accuracy']="דיוק";i['allTime']="אי פעם";i['bestRunOfDay']="הריצה הטובה ביותר היום";i['clickToReload']="לחץ/י כדי לטעון מחדש";i['combo']="רצף";i['createNewGame']="צרו משחק חדש";i['endRun']="סיים ריצה";i['failedPuzzles']="פאזלים שנכשלת בפתרונם";i['getReady']="התכונן!";i['highestSolved']="הפאזל הכי קשה שנפתר";i['highscores']="שיאים";i['highscoreX']=s("הניקוד הגבוה ביותר: %s");i['joinPublicRace']="הצטרפו למירוץ פומבי";i['joinRematch']="הצטרף/י למשחק חוזר";i['joinTheRace']="הצטרפו למירוץ!";i['moves']="מהלכים";i['moveToStart']="בצע/י מהלך כדי להתחיל";i['newAllTimeHighscore']="שיא חדש בכל הזמנים!";i['newDailyHighscore']="שיא יומי חדש!";i['newMonthlyHighscore']="שיא חודשי חדש!";i['newRun']="סיבוב חדש";i['newWeeklyHighscore']="שיא שבועי חדש!";i['nextRace']="המירוץ הבא";i['playAgain']="שחק/י שוב";i['playedNbRunsOfPuzzleStorm']=p({"one":"שוחקה ריצה אחת של %2$s","two":"שוחקו %1$s ריצות של %2$s","many":"שוחקו %1$s ריצות של %2$s","other":"שוחקו %1$s ריצות של %2$s"});i['previousHighscoreWasX']=s("השיא הקודם היה %s");i['puzzlesPlayed']="פאזלים ששוחקו";i['puzzlesSolved']="חידות נפתרו";i['raceComplete']="המירוץ הושלם!";i['raceYourFriends']="התחרה/י עם חבריך";i['runs']="ריצות";i['score']="ניקוד";i['skip']="דלג/י";i['skipExplanation']="דלג/י על מהלך זה כדי לשמור על הרצף שלך! ניתן לדלג רק פעם אחת בכל מירוץ.";i['skipHelp']="את/ה יכול/ה לדלג על מסע פעם אחת בכל מרוץ:";i['skippedPuzzle']="החידה שדילגת עליה";i['slowPuzzles']="פאזלים שהתעכבת בפתרונם";i['spectating']="צופה";i['startTheRace']="התחילו את המירוץ";i['thisMonth']="החודש";i['thisRunHasExpired']="המרוץ הסתיים!";i['thisRunWasOpenedInAnotherTab']="המרוץ הזה נפתח בחלון אחר!";i['thisWeek']="השבוע";i['time']="זמן";i['timePerMove']="זמן לכל מהלך";i['viewBestRuns']="צפו בריצות הכי טובות";i['waitForRematch']="המתינו למשחק חוזר";i['waitingForMorePlayers']="מחכים שעוד שחקנים יצטרפו...";i['waitingToStart']="ממתינים... נתחיל בקרוב";i['xRuns']=p({"one":"ניסיון אחד","two":"%s ניסיונות","many":"%s ניסיונות","other":"%s נסיונות"});i['youPlayTheBlackPiecesInAllPuzzles']="את/ה משחק/ת בשחור בכל החידות";i['youPlayTheWhitePiecesInAllPuzzles']="את/ה משחק/ת בלבן בכל החידות";i['yourRankX']=s("המיקום שלך: %s")})()