"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.storm)window.i18n.storm={};let i=window.i18n.storm;i['accuracy']="Ճշգրտություն";i['allTime']="Ամբողջ ընթացքում";i['bestRunOfDay']="Օրվա լավագույն փորձը";i['clickToReload']="Հպեք՝ վերագործարկելու համար";i['combo']="Քոմբո";i['createNewGame']="Ստեղծել նոր խաղ";i['endRun']="Ավարտել փորձը («Մուտք» ստեղն)";i['failedPuzzles']="Չլուծված խնդիրներ";i['getReady']="Պատրաստվե՜ք";i['highestSolved']="Շատ բարդ խնդիր";i['highscores']="Ռեկորդներ";i['highscoreX']=s("Ռեկորդ՝ %s");i['joinPublicRace']="Մասնակցել ընդհանուր մրցավազքին";i['joinRematch']="Միանալ ռևանշին";i['joinTheRace']="Միանալ մրցավազքին";i['moves']="Քայլ";i['moveToStart']="Սկսելու համար քայլ կատարեք";i['newAllTimeHighscore']="Բոլոր ժամանակների նոր ռեկորդ";i['newDailyHighscore']="Օրվա նո՛ր ռեկորդ";i['newMonthlyHighscore']="Ամսվա նոր ռեկորդ";i['newRun']="Նոր փորձ («Բացատ» ստեղն)";i['newWeeklyHighscore']="Շաբաթվա նոր ռեկորդ";i['nextRace']="Հաջորդ մրցավազքը";i['playAgain']="Խաղալ նորից";i['playedNbRunsOfPuzzleStorm']=p({"one":"Խաղացվել է մեկ շարք %2$s-ում","other":"Խաղացվել է %1$s շարք %2$s-ում"});i['previousHighscoreWasX']=s("Նախորդ ռեկորդը եղել է %s");i['puzzlesPlayed']="Խաղացված խնդիրներ";i['puzzlesSolved']="խնդիրներ լուծվել են";i['raceComplete']="Մրցավազքն ավարտված է";i['raceYourFriends']="Մրցել ընկերների հետ";i['runs']="Շարքեր";i['score']="Արդյունք";i['skip']="բաց թողնել";i['skipExplanation']="Բաց թողնել այս քայլը՝ շարքը պահպանելու համար։ Մրցավազքի ընթացքում կարելի է օգտագործել միայն մեկ անգամ։";i['skipHelp']="Մրցավազքի ընթացքում Դուք կարող եք բաց թողնել մեկ քայլ.";i['skippedPuzzle']="Բաց թողնված խնդիր";i['slowPuzzles']="Երկար լուծելի խնդիրներ";i['spectating']="Դիտում";i['startTheRace']="Սկսել մրցավազքը";i['thisMonth']="Այս ամիս";i['thisRunHasExpired']="Այս շարքի ժամանակը սպառվե՛լ է:";i['thisRunWasOpenedInAnotherTab']="Այս շարքը բացվել է մեկ ա՛յլ ներդիրում:";i['thisWeek']="Այս շաբաթ";i['time']="Ժամանակ";i['timePerMove']="Ժամանակ քայլին";i['viewBestRuns']="Դիտել լավագույն փորձերը";i['waitForRematch']="Սպասում ենք ռևանշի";i['waitingForMorePlayers']="Սպասում ենք այլ խաղացողների...";i['waitingToStart']="Սպասում ենք մեկնարկին";i['xRuns']=p({"one":"1 փորձ","other":"%s փորձ"});i['youPlayTheBlackPiecesInAllPuzzles']="Դուք խաղում եք սև խաղաքարերով բոլոր խնդիրներում";i['youPlayTheWhitePiecesInAllPuzzles']="Դուք խաղում եք սպիտակ խաղաքարերով բոլոր խնդիրներում";i['yourRankX']=s("Ձեր տեղը՝ %s")})()