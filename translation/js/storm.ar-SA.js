"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.storm)window.i18n.storm={};let i=window.i18n.storm;i['accuracy']="الدقة";i['allTime']="كل الأوقات";i['bestRunOfDay']="أفضل جولة لك اليوم";i['clickToReload']="اضغط لإعادة التحميل";i['combo']="مجموعة";i['createNewGame']="أنشئ لعبة جديدة";i['endRun']="انهاء السباق";i['failedPuzzles']="الألغاز التي فشلت في حلها";i['getReady']="استعد!";i['highestSolved']="تقييم أصعب لغز تم حله";i['highscores']="أعلى النتائج";i['highscoreX']=s("أعلى نتيجة: %s");i['joinPublicRace']="انضم للسباقٍ علني";i['joinRematch']="انضم إلى المباراة من جديد";i['joinTheRace']="انضم للسباق!";i['moves']="نقلة";i['moveToStart']="حَرِك لتبدأ";i['newAllTimeHighscore']="أعلى مستوى جديد على الإطلاق!";i['newDailyHighscore']="حققت نتيجة يومية جديدة!";i['newMonthlyHighscore']="حققت نتيجة شهرية جديدة!";i['newRun']="سباق جديد";i['newWeeklyHighscore']="حققت نتيجة أسبوعية جديدة!";i['nextRace']="السباق التالي";i['playAgain']="إلعب مرة أخرى";i['playedNbRunsOfPuzzleStorm']=p({"zero":"لم تلعب أي جولة %2$s","one":"لعبت جولة واحدة من %2$s","two":"لعبت جولتين من %2$s","few":"لعبت %1$s جولات من %2$s","many":"لعبت %1$s جولة من %2$s","other":"لعبت %1$s جولة من %2$s"});i['previousHighscoreWasX']=s("النتيجة العالية السابقة كانت %s");i['puzzlesPlayed']="الألغاز التي لعبتها سابقا";i['puzzlesSolved']="الألغاز التي حللتها سابقا";i['raceComplete']="انتهى السباق!";i['raceYourFriends']="واجه أصدقائك";i['runs']="جولات";i['score']="النتيجة";i['skip']="تخطى";i['skipExplanation']="تخطى هذه الحركة للحفاظ على سلسلة الانتصارات.";i['skipHelp']="يمكنك تخطي حركة واحدة في كل سباق:";i['skippedPuzzle']="الألغاز التي تخطيتها";i['slowPuzzles']="ألغاز بطيئة";i['spectating']="تفرج";i['startTheRace']="بدء السباق";i['thisMonth']="هذا الشهر";i['thisRunHasExpired']="انتهت صلاحية هذا السباق!";i['thisRunWasOpenedInAnotherTab']="تم فتح هذا السباق في علامة تبويب أخرى!";i['thisWeek']="هذا الأسبوع";i['time']="الوقت";i['timePerMove']="الوقت لكل نقلة";i['viewBestRuns']="اعرض أفضل الجولات";i['waitForRematch']="انتظر إعادة اللعب";i['waitingForMorePlayers']="ينتظر انضمام مزيد من الاعبين...";i['waitingToStart']="ينتظر البدء";i['xRuns']=p({"zero":"لا جولات","one":"جولة واحدة","two":"جولتان","few":"%s جولات","many":"%s جولة","other":"%s جولة"});i['youPlayTheBlackPiecesInAllPuzzles']="أنت تلعب بالقطع السوداء في جميع الألغاز";i['youPlayTheWhitePiecesInAllPuzzles']="أنت تلعب بالقطع البيضاء في جميع الألغاز";i['yourRankX']=s("رتبتك %s")})()