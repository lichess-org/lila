"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.storm)window.i18n.storm={};let i=window.i18n.storm;i['accuracy']="अचूकता";i['allTime']="सर्वकालीन";i['combo']="चालींचा संयोग";i['highestSolved']="सर्वोच्च सोडविलेले";i['highscoreX']=s("उच्च गुणसंख्या: %s");i['moves']="चाली";i['moveToStart']="सुरू करण्यासाठी सोंगटी हलवा";i['newAllTimeHighscore']="नवीन सर्वकालीन उच्चांक!";i['newDailyHighscore']="नवीन दैनिक उच्चांक!";i['newMonthlyHighscore']="नवीन मासिक उच्चांक!";i['newWeeklyHighscore']="नवीन साप्ताहिक उच्चांक!";i['playAgain']="परत खेळा";i['playedNbRunsOfPuzzleStorm']=p({"one":"%2$s ची एक फेरी खेळून झाली","other":"%2$s च्या %1$s फेऱ्या खेळून झाल्या"});i['previousHighscoreWasX']=s("मागील उच्चांक %s होता");i['puzzlesPlayed']="खेळलेली कोडी";i['puzzlesSolved']="कोडी सोडवली";i['score']="गुण";i['skip']="सोडा";i['thisMonth']="या महिन्यात";i['thisWeek']="या आठवड्यात";i['time']="वेळ";i['timePerMove']="वेळ प्रति मुव्ह";i['waitingForMorePlayers']="आणखी खेळाडू दाखल होण्याच्या प्रतिक्षेत...";i['xRuns']=p({"one":"१ प्रयत्न","other":"%s प्रयत्न"});i['youPlayTheBlackPiecesInAllPuzzles']="तुम्ही सर्व कोड्यांमध्ये काळ्या सोंगट्यांनी खेळणार आहात";i['youPlayTheWhitePiecesInAllPuzzles']="तुम्ही सर्व कोड्यांमध्ये पांढऱ्या सोंगट्यांनी खेळणार आहात"})()