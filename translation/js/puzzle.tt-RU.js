"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.puzzle)window.i18n.puzzle={};let i=window.i18n.puzzle;i['addAnotherTheme']="Икенче тһеманы өстәргә";i['advanced']="Өстәмә";i['bestMove']="Иң яхшы хәрәкәт!";i['clickToSolve']="Чишер өчен сук";i['continueTheStreak']="Сызуны дәвам итегез";i['continueTraining']="Күнегүләр дәвамы";i['didYouLikeThisPuzzle']="Сезгә ошадымы башваткыч?";i['difficultyLevel']="Авырлык дәрәҗәсе";i['easier']="Җиңелрәк";i['easiest']="Иң җиңелрәге";i['example']="Мәсәлән";i['failed']="уңышсыз";i['findTheBestMoveForBlack']="Тап якшы хәрәкәт каралар өчен.";i['findTheBestMoveForWhite']="Тап якшы хәрәкәт аклар өчен.";i['fromGameLink']=s("%s уендан");i['goals']="Морады";i['goodMove']="Якшы хәрәкәт";i['harder']="Авыррак";i['hardest']="Иң авыры";i['hidden']="качырылган";i['history']="Башваткыч тарихы";i['improvementAreas']="Яхшырту өлкәләре";i['jumpToNextPuzzleImmediately']="Киләсе Башваткычка шунда ук сикерегез";i['keepGoing']="Юлга дәвам…";i['lengths']="Озынлык";i['mates']="Матлар";i['motifs']="Зәхәриф";i['nbPointsAboveYourPuzzleRating']=p({"other":"%s Сезнең башваткыч рейтингтан бер максад"});i['nbPointsBelowYourPuzzleRating']=p({"other":"%s сезнең башваткыч рейтингтан бер максад"});i['newStreak']="Яңа сызу";i['normal']="Нормаль";i['notTheMove']="Бу хәрәкәт түгел!";i['origin']="Чыгыш";i['phases']="Фазалар";i['playedXTimes']=p({"other":"Уйналган %s вакыт"});i['puzzleComplete']="Башваткыч чишелгән!";i['puzzleDashboard']="Башваткыч Гөстертактасы";i['puzzleId']=s("Башваткыч %s");i['puzzleOfTheDay']="Бүгенге башваткыч";i['puzzles']="Башваткычлар";i['puzzleSuccess']="Уңышлы!";i['puzzleThemes']="Башваткыч бизәкләре";i['ratingX']=s("Рейтинг: %s");i['recommended']="Тәкдим ителә";i['solved']="чишелгән";i['specialMoves']="Максус хәрәкәтләр";i['streakDescription']="Акрынлап катлаулы башваткычларны чишегез һәм җиңү сызыгы төзегез. Сәгать юк, шуңа күрә вакытыгызны алыгыз. Бер ялгыш хәрәкәт, һәм бу уен бетте! Ләкин сез сессиягә бер хәрәкәтне калдыра аласыз.";i['streakSkipExplanation']="Сезнең юлны саклап калу өчен бу хәрәкәтне ташлагыз! Бер тапкыр гына эшли.";i['strengths']="Көчләр яклары";i['toGetPersonalizedPuzzles']="Персональләштерелгән башваткычлар алу өчен:";i['trySomethingElse']="Башка нәрсәне сынап карагыз.";i['voteToLoadNextOne']="Киләсе йөкләү өчен тавыш бирегез!";i['yourStreakX']=s("Сезнең юл %s")})()