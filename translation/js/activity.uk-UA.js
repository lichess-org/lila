"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.activity)window.i18n.activity={};let i=window.i18n.activity;i['activity']="Активність";i['competedInNbSwissTournaments']=p({"one":"Завершив %s турнір за швейцарською системою","few":"Завершив %s турніри за швейцарською системою","many":"Завершив %s турнірів за швейцарською системою","other":"Завершив %s турніри за швейцарською системою"});i['competedInNbTournaments']=p({"one":"Змагався в %s турнірі","few":"Змагався в %s турнірах","many":"Змагався в %s турнірах","other":"Змагався в %s турнірів"});i['completedNbGames']=p({"one":"Зіграно %s заочну гру","few":"Зіграно %s заочні гри","many":"Зіграно %s заочних ігор","other":"Зіграно %s заочних ігор"});i['completedNbVariantGames']=p({"one":"Зіграно %1$s %2$s заочну гру","few":"Зіграно %1$s %2$s заочні гри","many":"Зіграно %1$s %2$s заочних ігор","other":"Зіграно %1$s %2$s заочних ігор"});i['createdNbStudies']=p({"one":"Створено %s нове дослідження","few":"Створено %s нові дослідження","many":"Створено %s нових досліджень","other":"Створено %s нових досліджень"});i['followedNbPlayers']=p({"one":"Підписався на %s гравця","few":"Почав спостерігати за %s гравцями","many":"Почав спостерігати за %s гравцями","other":"Почав спостерігати за %s гравцями"});i['gainedNbFollowers']=p({"one":"Отримав %s нового підписника","few":"Отримав %s нових підписників","many":"Отримав %s нових підписників","other":"Отримав %s нових підписників"});i['hostedALiveStream']="Проведено пряму трансляцію";i['hostedNbSimuls']=p({"one":"Провів %s сеанс одночасної гри","few":"Провів %s сеанси одночасної гри","many":"Провів %s сеансів одночасної гри","other":"Провів %s сеансів одночасної гри"});i['inNbCorrespondenceGames']=p({"one":"у %1$s заочній грі","few":"у %1$s заочних іграх","many":"у %1$s заочних іграх","other":"у %1$s заочних ігор"});i['joinedNbSimuls']=p({"one":"Брав участь у %s сеансі одночасної гри","few":"Брав участь в %s сеансах одночасної гри","many":"Брав участь в %s сеансах одночасної гри","other":"Брав участь в %s сеансів одночасної гри"});i['joinedNbTeams']=p({"one":"Приєднався до %s команди","few":"Приєднався до %s команд","many":"Приєднався до %s команд","other":"Приєднався до %s команд"});i['playedNbGames']=p({"one":"Зіграно %1$s гру в %2$s","few":"Зіграно %1$s гри в %2$s","many":"Зіграно %1$s ігор в %2$s","other":"Зіграно %1$s ігор в %2$s"});i['playedNbMoves']=p({"one":"Зроблено %1$s хід","few":"Зроблено %1$s ходи","many":"Зроблено %1$s ходів","other":"Зроблено %1$s ходів"});i['postedNbMessages']=p({"one":"Опубліковано %1$s повідомлення в %2$s","few":"Опубліковано %1$s повідомлення в %2$s","many":"Опубліковано %1$s повідомлень в %2$s","other":"Опубліковано %1$s повідомлень в %2$s"});i['practicedNbPositions']=p({"one":"Виконано %1$s вправу в %2$s","few":"Виконано %1$s вправи в %2$s","many":"Виконано %1$s вправ в %2$s","other":"Виконано %1$s вправ в %2$s"});i['rankedInSwissTournament']=s("Зайняв #%1$s місце в %2$s");i['rankedInTournament']=p({"one":"Досяг #%1$s місця (кращі %2$s%%), зіграна %3$s гра, в турнірі: %4$s","few":"Досяг #%1$s місця (кращі %2$s%%), зіграно %3$s гри, турнір: %4$s","many":"Досяг #%1$s місця (кращі %2$s%%), зіграно %3$s ігор, турнір: %4$s","other":"Досяг #%1$s місця (кращі %2$s%%), зіграно %3$s ігор, турнір: %4$s"});i['signedUp']="Зареєструвався на lichess.org";i['solvedNbPuzzles']=p({"one":"Вирішено %s тактичну задачу","few":"Вирішено %s тактичні задачі","many":"Вирішено %s тактичних задач","other":"Вирішено %s тактичних задач"});i['supportedNbMonths']=p({"one":"Підтримує lichess.org протягом %1$s місяця як %2$s","few":"Підтримує lichess.org протягом %1$s місяців як %2$s","many":"Підтримує lichess.org протягом %1$s місяців як %2$s","other":"Підтримує lichess.org протягом %1$s місяців як %2$s"})})()