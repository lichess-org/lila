"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.swiss)window.i18n.swiss={};let i=window.i18n.swiss;i['absences']="Отсъствия";i['comparison']="Сравнение";i['dutchSystem']="Холандската система";i['earlyDrawsAnswer']="В турнири по швейцарската система игрите не могат да завършат с реми преди да са изиграни 30 хода. Въпреки че това правило не може да предотврати предватително договорени ремита, то прави договарянето на реми по време на игра по-тудно.";i['FIDEHandbook']="Правилника на ФИДЕ";i['forbiddedUsers']="Само играчите от този списък ще могат да участват, освен ако списъкът е празен. Въведете само по едно потребителско име на ред.";i['forbiddenPairings']="Забранени съчетавания";i['forbiddenPairingsHelp']="Имена на играчите които не трябва да играят един срещу друг (например роднини). Въведете по две имена на линия, разделени с интервал.";i['identicalForbidden']="Забранени";i['identicalPairing']="Идентични съчетавания";i['lateJoin']="Късно включване";i['lateJoinA']="Да, докато не са започнали повече от половината рундове; например в 11-рундов турнир по швейцарската система играчи могат да се присъединят преди началото на рунд 6, а в 12-рундов, преди началото на рунд 7.\nКъсно присъединяващите се получават една точка, дори ако са пропуснали няколко рунда.";i['lateJoinUntil']="Да, преди повече от половината рундове да са започнали";i['moreRoundsThanPlayersQ']="Какво става ако турнир има повече рундове отколкото играчи?";i['nbRounds']=p({"one":"%s рунд","other":"%s рунда"});i['newSwiss']="Нов швейцарски турнир";i['nextRound']="Следващ рунд";i['numberOfGames']="Брой партии";i['numberOfGamesAsManyAsPossible']="Толкова, колкото могат да се изиграят за даденото време";i['numberOfGamesPreDefined']="Задава се предварително, еднакво за всички играчи";i['numberOfRounds']="Брой рундове";i['numberOfRoundsHelp']="Нечетен брой рундове оптимално балансира на цветовете за всички играчи.";i['oneRoundEveryXDays']=p({"one":"Един рунд на ден","other":"Един рунд всеки %s дена"});i['ongoingGames']=p({"one":"Текуща партия","other":"Текущи партии"});i['pairingsA']=s("По %1$s, осъществена чрез %2$s, според %3$s.");i['pairingSystem']="Система за съчетаване";i['pairingWaitTimeArena']="Бързо: не се изчакват всички играчи";i['pairingWaitTimeSwiss']="Бавно: изчакват се всички играчи";i['pause']="Пауза";i['pauseSwiss']="Да, но може да намали броя на рундовете";i['playYourGames']="Играйте игрите си";i['pointsCalculationA']="Победа дава една точка, реми - половин точка, а загубата не носи точки. Когато играч не може да бъде съчетан, получва енда точка за автоматична победа.";i['pointsCalculationQ']="Как се изчисляват точките?";i['possibleButNotConsecutive']="Възможни, но не последователни";i['predefinedUsers']="Позволете само на определени играчи да участват";i['restrictedToTeamsA']="Турнири по швейцарската система не са подходящи за онлайн шах. Те изискват точност, отдаденост и търпение от играчите.\nСмятаме, че тези условия е по-вероятно да бъдат изпълнени в отборни турнири, отколкото в глобални турнири.";i['roundInterval']="Интервал между рундовете";i['roundRobinA']="Бихме искали да го добавим, но за съжаление Round Robin не работи онлайн. Причината е, че няма справедлив начин да се справи с играчите, които напускат турнира по-рано. Не можем да очакваме, че всички играчи ще изиграят всичките си игри в дадено онлайн събитие. В резултат, повечето Round Robin турнири биха били несправедливи, което противоречи на самата причина те да съществуват. Най-близкото до онлайн Round Robin турнир е швейцарски турнир с много голям брой рундове. Тогава всички възможни сдвоявания ще бъдат изиграни преди края на турнира.";i['roundsAreStartedManually']="Рундовете се пускат на ръка";i['similarToOTB']="Подобно на класическите турнири";i['sonnebornBergerScore']="показател на Зонеборн-Бергер";i['startingIn']="Започва след";i['startingSoon']="Скоро започва";i['streaksAndBerserk']="Поредици и Берсерк";i['swissTournaments']="Швейцарски турнири";i['swissVsArenaA']="В турнир по швейцарската система всички участници играят еднакъв брой игри и могат да играят един срещу друг само веднъж.\nТова е добър избор за клубове и официални турнири.";i['swissVsArenaQ']="Кога да използваме швейцарски турнири вместо арени?";i['tiebreaksCalculationA']=s("Чрез %s.\nСумата от точките на противниците, срещу които състезателят е спечелил, плюс половината от точките на противниците, с които е завършил реми.");i['tournDuration']="Продължителност на турнира";i['tournStartDate']="Начална дата на турнира";i['unlimitedAndFree']="Неограничено и безплатно";i['viewAllXRounds']=p({"one":"Вижте рунда","other":"Вижте всички %s рунда"});i['xMinutesBetweenRounds']=p({"one":"%s минута между рундовете","other":"%s минути между рундовете"});i['xRoundsSwiss']=p({"one":"%s рунд по швейцарската система","other":"%s рунда по швейцарската система"});i['xSecondsBetweenRounds']=p({"one":"%s секунда между рундовете","other":"%s секунди между рундовете"})})()