"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.activity)window.i18n.activity={};let i=window.i18n.activity;i['activity']="Актыўнасць";i['competedInNbSwissTournaments']=p({"one":"Паўдзельнічаў(-ла) у %s турніры па швейцарскай сістэме","few":"Паўдзельнічаў(-ла) у %s турнірах па швейцырскай сістэме","many":"Паўдзельнічаў(-ла) у %s турнірах па швейцырскай сістэме","other":"Паўдзельнічаў(-ла) у %s турнірах па швейцырскай сістэме"});i['competedInNbTournaments']=p({"one":"Паўдзельнічаў у %s турніры","few":"Паўдзельнічаў у %s турнірах","many":"Паўдзельнічаў у %s турнірах","other":"Паўдзельнічаў у %s турнірах"});i['completedNbGames']=p({"one":"Згуляна %s гульня па ліставанні","few":"Згуляна %s гульні па ліставанні","many":"Згуляна %s гульняў па ліставанні","other":"Згуляна %s гульняў па ліставанні"});i['createdNbStudies']=p({"one":"Стварыў %s новае даследаванне","few":"Стварыў %s новыя даследаванні","many":"Стварыў %s новых даследаванняў","other":"Стварыў %s новых даследаванняў"});i['followedNbPlayers']=p({"one":"Падпісаўся на %s гульца","few":"Падпісаўся на %s гульцоў","many":"Падпісаўся на %s гульцоў","other":"Падпісаўся на %s гульцоў"});i['gainedNbFollowers']=p({"one":"%s новы падпісчык","few":"%s новых падпісчыкі","many":"%s новых падпісчыкаў","other":"%s новых падпісчыкаў"});i['hostedALiveStream']="Правялі прамую трансляцыю";i['hostedNbSimuls']=p({"one":"Правёў %s сеанс адначасовай гульні","few":"Правёў %s сеансы адначасовай гульні","many":"Правёў %s сеансаў адначасовай гульні","other":"Правёў %s сеансаў адначасовай гульні"});i['inNbCorrespondenceGames']=p({"one":"у %1$s гульні па ліставанні","few":"у %1$s гульнях па ліставанні","many":"у %1$s гульнях па ліставанні","other":"у %1$s гульнях па ліставанні"});i['joinedNbSimuls']=p({"one":"Браў удзел у %s сеансе адначасовай гульні","few":"Браў удзел у %s сеансах адначасовай гульні","many":"Браў удзел у %s сеансах адначасовай гульні","other":"Браў удзел у %s сеансах адначасовай гульні"});i['joinedNbTeams']=p({"one":"Далучыўся да %s каманды","few":"Далучыўся да %s каманд","many":"Далучыўся да %s каманд","other":"Далучыўся да %s каманд"});i['playedNbGames']=p({"one":"Згуляна %1$s гульня ў %2$s","few":"Згуляна %1$s гульняў у %2$s","many":"Згуляна %1$s гульняў у %2$s","other":"Згуляна %1$s гульняў у %2$s"});i['playedNbMoves']=p({"one":"Згуляны %1$s ход","few":"Згуляна %1$s хады","many":"Згуляна %1$s хадоў","other":"Згуляна %1$s хадоў"});i['postedNbMessages']=p({"one":"Апублікавана %1$s паведамленне ў %2$s","few":"Апублікавана %1$s паведамленняў у %2$s","many":"Апублікавана %1$s паведамленняў у %2$s","other":"Апублікавана %1$s паведамленняў у %2$s"});i['practicedNbPositions']=p({"one":"Практыкавана пазіцыя %1$s у %2$s","few":"Практыкаваны пазіцыі %1$s у %2$s","many":"Практыкаваны пазіцыі %1$s у %2$s","other":"Практыкаваны пазіцыі %1$s у %2$s"});i['rankedInSwissTournament']=s("Скончыў на %1$s месцы ў %2$s");i['rankedInTournament']=p({"one":"Заняў %1$s месца (лепшыя %2$s%%) турніру %4$s, узяўшы ўдзел у %3$s гульні","few":"Заняў %1$s месца (лепшыя %2$s%%) турніру %4$s, узяўшы ўдзел у %3$s гульнях","many":"Заняў %1$s месца (лепшыя %2$s%%) турніру %4$s, узяўшы ўдзел у %3$s гульнях","other":"Заняў %1$s месца (лепшыя %2$s%%) турніру %4$s, узяўшы ўдзел у %3$s гульнях"});i['signedUp']="Зарэгістраваўся на lichess";i['solvedNbPuzzles']=p({"one":"Вырашана %s задача","few":"Вырашана %s задач","many":"Вырашана %s задач","other":"Вырашана %s задач"});i['supportedNbMonths']=p({"one":"Падтрымаў lichess.org на %1$s месяц як %2$s","few":"Падтрымаў lichess.org на %1$s месяцы як %2$s","many":"Падтрымаў lichess.org на %1$s месяцаў як %2$s","other":"Падтрымаў lichess.org на %1$s месяцаў як %2$s"})})()