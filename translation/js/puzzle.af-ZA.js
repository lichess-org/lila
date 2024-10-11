"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.puzzle)window.i18n.puzzle={};let i=window.i18n.puzzle;i['addAnotherTheme']="Voeg nog \\'n tema by";i['advanced']="Gevorderd";i['bestMove']="Beste skuif!";i['byOpenings']="Vir openinge";i['clickToSolve']="Druk om op te los";i['continueTheStreak']="Gaan voort met die reeks";i['continueTraining']="Gaan voort met opleiding";i['dailyPuzzle']="Daaglikse Raaisel";i['didYouLikeThisPuzzle']="Hou jy van hierdie raaisel?";i['difficultyLevel']="Moeilikheidsgraad";i['downVote']="Stem teen raaisel";i['easier']="Makliker";i['easiest']="Maklikste";i['example']="Voorbeeld";i['failed']="gefaal";i['findTheBestMoveForBlack']="Vind die beste skuif vir swart.";i['findTheBestMoveForWhite']="Vind die beste skuif vir wit.";i['fromGameLink']=s("Van spel %s");i['fromMyGames']="Van my spelle";i['fromMyGamesNone']="Jy het geen raaisels in die databasis nie, maar Lichess waardeer steeds jou.\n\nSpeel snel en klassieke spelle om jou kanse te verhoog dat een van jou eie raaisels bygevoeg word!";i['fromXGames']=s("Raaisels van %s se spelle");i['fromXGamesFound']=s("%1$s raaisels gevind in %2$s se spelle");i['goals']="Doelwitte";i['goodMove']="Goeie skuif";i['harder']="Moeiliker";i['hardest']="Moeilikste";i['hidden']="verborge";i['history']="Kopkrapper geskiedenis";i['improvementAreas']="Areas van verbetering";i['improvementAreasDescription']="Oefen hierdie om jou progressie te optimaliseer!";i['jumpToNextPuzzleImmediately']="Spring onmiddellik na die volgende kopkrapper";i['keepGoing']="Hou aan…";i['lengths']="Lengtes";i['lookupOfPlayer']="Beloer raaisels van \\'n speler se spelle";i['mates']="Matte";i['motifs']="Redes";i['nbPlayed']=p({"one":"%s gespeel","other":"%s gespeel"});i['nbPointsAboveYourPuzzleRating']=p({"one":"Een punt bo jou raaisel gradering","other":"%s punt bo jou raaisel gradering"});i['nbPointsBelowYourPuzzleRating']=p({"one":"Een punt onder jou raaisel gradering","other":"%s punte onder jou raaisel gradering"});i['nbToReplay']=p({"one":"%s om te herhaal","other":"%s om te herhaal"});i['newStreak']="Nuwe reeks";i['nextPuzzle']="Volgende raaisel";i['noPuzzlesToShow']="Niks om te wys nie, gaan los eers \\'n paar raaisels op!";i['normal']="Normaal";i['notTheMove']="Dit is nie die skuif nie!";i['openingsYouPlayedTheMost']="Openinge wat jy die meeste in gradeerde spelle speel";i['origin']="Oorsprong";i['percentSolved']=s("%s opgelos");i['phases']="Fases";i['playedXTimes']=p({"one":"%s keer gespeel","other":"%s keer gespeel"});i['puzzleComplete']="Raaisel voltooid!";i['puzzleDashboard']="Kopkrapper paneelbord";i['puzzleDashboardDescription']="Oefen, analiseer, verbeter";i['puzzleId']=s("Raaisels %s");i['puzzleOfTheDay']="Raaisels van die dag";i['puzzles']="Raaisels";i['puzzlesByOpenings']="Raaisels vir openinge";i['puzzleSuccess']="Sukses!";i['puzzleThemes']="Raaisel temas";i['ratingX']=s("Gradering: %s");i['recommended']="Aanbeveeldede";i['searchPuzzles']="Soek raaisels";i['solved']="opgelos";i['specialMoves']="Spesiale bewegings";i['streakDescription']="Los toenemend moeiliker raaisels op en bou \\'n wen reeks. Daar is nie \\'n tydsfaktor nie, so vat dit rustig. Een verkeerde skuif en dit stuit! Maar jy kan \\'n skuif per sessie oorslaan.";i['streakSkipExplanation']="Slaan die skuif oor, om jou reeks te behou! Werk slegs een keer per ronde.";i['strengthDescription']="Jy presteer die beste in hierdie temas";i['strengths']="Sterkpunte";i['toGetPersonalizedPuzzles']="Om persoonlike raaisels te kry:";i['trySomethingElse']="Probeer iets anders.";i['upVote']="Stem vir raaisel";i['useCtrlF']="Gebruik Ctrl+f om jou gensteling opening te vind!";i['useFindInPage']="Gebruik die \\\"Find in page\\\" in die blaaier se gids om jou gunsteling opening te vind!";i['voteToLoadNextOne']="Stem om die volgende een te laai!";i['yourPuzzleRatingWillNotChange']="Jou raaisel gradeering sal nie verander nie. Neem kennis dat raaisels is nie \\'n kompetisie nie. Jou gradeering help om die mees geskikte raaisels vir jou vermoë te kies.";i['yourStreakX']=s("Jou wen reeks: %s")})()