"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.puzzle)window.i18n.puzzle={};let i=window.i18n.puzzle;i['addAnotherTheme']="Aghjunghje un altru tema";i['advanced']="Avanzatu";i['bestMove']="U colpu u più forte!";i['clickToSolve']="Clicheghja per risolve";i['continueTheStreak']="Cuntinuate a filara";i['continueTraining']="Cuntinuà à addestrà si";i['didYouLikeThisPuzzle']="Vi hà piaciutu stu ?";i['difficultyLevel']="Livellu di difficultà";i['downVote']="Vutà contru à issu prublema";i['easier']="Faciule";i['easiest']="Faciule faciule";i['example']="Esempiu";i['failed']="fiascatu";i['findTheBestMoveForBlack']="Truvate u colpu u più forte pè i neri.";i['findTheBestMoveForWhite']="Truvate u colpu u più forte pè i bianchi.";i['fromGameLink']=s("Strattu di a partita %s");i['fromMyGames']="Strattu di e mo partite";i['fromMyGamesNone']="Ùn avete prublemi in a databasa, ma li piaceria à Lichess d\\' avè ne.\n\nGhjucate e partite rapide è e partite classiche per aumintà a prubabilità d\\' avè un  strattu di e vostre partite!";i['fromXGames']=s("Prublemi stratti di e partite di %s");i['fromXGamesFound']=s("%1$s prublemi trovi in %2$s partite");i['goals']="Scopu";i['goodMove']="Bellu colpu";i['harder']="Difficiule";i['hardest']="Difficiule difficiule";i['hidden']="piattu";i['history']="Storia di i prublemi";i['improvementAreas']="Duminii da miglurà";i['improvementAreasDescription']="Addestrate vi in issi duminii pà prugressà du modu ottimu!";i['jumpToNextPuzzleImmediately']="Passà à u prossimu publemu subitu subitu";i['keepGoing']="Cuntinuate…";i['lengths']="Lunghezza";i['lookupOfPlayer']="Cercà i prublemi in e partite di l\\' altri ghjucadori";i['mates']="Scaccumatu";i['motifs']="Figura";i['nbPlayed']=p({"one":"%s ghjucatu","other":"%s ghjucati"});i['nbPointsAboveYourPuzzleRating']=p({"one":"Un puntu sopra à a vostra classifica di","other":"%s punti sopra à a vostra classifica di"});i['nbPointsBelowYourPuzzleRating']=p({"one":"Un puntu sottu à a vostra classifica di","other":"%s punti sottu à a vostra classifica di"});i['nbToReplay']=p({"one":"%s à ghjucà da torna","other":"%s à ghjucà da torna"});i['newStreak']="Filara nova";i['nextPuzzle']="Prossimu prublema";i['noPuzzlesToShow']="Nunda à mustrà, ghjucate parechji prublemi innanzu!";i['normal']="Nurmale";i['notTheMove']="Ùn hè u colpu bonu!";i['origin']="Origine";i['percentSolved']=s("%s risolti");i['phases']="Mumentu";i['playedXTimes']=p({"one":"Ghjucatu %s volta","other":"Ghjucatu %s volte"});i['puzzleComplete']="Prublemu finitu!";i['puzzleDashboard']="Statistichi di i prublemi";i['puzzleDashboardDescription']="Addestrà, analisà, migliurà";i['puzzleId']=s("Prublemu%s");i['puzzleOfTheDay']="Prubemu di u ghjornu";i['puzzles']="Prublemi";i['puzzleSuccess']="Successu!";i['puzzleThemes']="Temi di i prublemi";i['ratingX']=s("Classifica: %s");i['recommended']="Cunsigliatu";i['searchPuzzles']="Cercà i prublemi";i['solved']="risoltu";i['specialMoves']="Colpu speciale";i['streakDescription']="Risulvite i prublemi di modu prugressivu è custruite una filara di vittorie. Ùn ci hè riloghju, dunque pudete piglià u tempu. Un sbagliu, è a partita hè persa! Pudete saltà un colpu durante una sessione.";i['streakSkipExplanation']="Lasciate corre issu colpu pè guardà a vostra filara. Ùn la pudete fà ch\\' una volta durante issa sessione.";i['strengthDescription']="Avete riesciutu di modu ottimu in issi temi";i['strengths']="A maestria";i['toGetPersonalizedPuzzles']="Per uttene prubemi persunalizati:";i['trySomethingElse']="Prova un altru colpu.";i['upVote']="Vutà pà stu prublema";i['voteToLoadNextOne']="Vutate pà carigà u prossimu!";i['yourPuzzleRatingWillNotChange']="A vostra classifica di  ùn hà da cambià. Issi prublemi ùn sò una cumpetizione. A vostra classifica ci permette di prupone vi prublemi addattati à u vostru livellu attuale.";i['yourStreakX']=s("A vostra filara:%s")})()