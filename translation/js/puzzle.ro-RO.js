"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.puzzle)window.i18n.puzzle={};let i=window.i18n.puzzle;i['addAnotherTheme']="Adăugați o altă temă";i['advanced']="Avansat";i['bestMove']="Cea mai bună mutare!";i['byOpenings']="După deschideri";i['clickToSolve']="Dă click pentru a rezolva";i['continueTheStreak']="Continuă secvența";i['continueTraining']="Continuați antrenamentul";i['dailyPuzzle']="Puzzle Zilnic";i['didYouLikeThisPuzzle']="Ți-a plăcut această problemă?";i['difficultyLevel']="Nivel de dificultate";i['downVote']="Vot negativ";i['easier']="Mai ușor";i['easiest']="Cel mai ușor";i['example']="Exemplu";i['failed']="eșuat";i['findTheBestMoveForBlack']="Găsiți cea mai bună mutare pentru negru.";i['findTheBestMoveForWhite']="Găsiți cea mai bună mutare pentru alb.";i['fromGameLink']=s("Din partida %s");i['fromMyGames']="Din jocurile mele";i['fromMyGamesNone']="Nu ai nici o problemă în baza de date, dar Lichess tot te apreciază foarte mult.\nJoacă jocuri rapide și clasice pentru a crește șansele de a vedea adăugată o problemă extrasă din partidele tale!";i['fromXGames']=s("Puzzle-uri din jocurile %s");i['fromXGamesFound']=s("%1$s probleme găsite în %2$s jocuri");i['goals']="Obiective";i['goodMove']="Mutare bună";i['harder']="Mai Greu";i['hardest']="Cel mai greu";i['hidden']="ascuns";i['history']="Istoric probleme de șah";i['improvementAreas']="Zone de îmbunătățit";i['improvementAreasDescription']="Antreneaza-te pentru a-ti optimiza progresul!";i['jumpToNextPuzzleImmediately']="Sari imediat la problema următoare";i['keepGoing']="Continuați cu următoarea mutare…";i['lengths']="Lungimi";i['lookupOfPlayer']="Caută puzzle-uri din jocurile unui jucător";i['mates']="Mate";i['motifs']="Motive";i['nbPlayed']=p({"one":"%s jucat","few":"%s jucate","other":"%s jucate"});i['nbPointsAboveYourPuzzleRating']=p({"one":"Un punct deasupra ratingului tău de puzzle-uri","few":"%s puncte deasupra ratingului tău de puzzle-uri","other":"%s de puncte deasupra ratingului tău de puzzle-uri"});i['nbPointsBelowYourPuzzleRating']=p({"one":"Un punct sub ratingul tău de puzzle-uri","few":"%s puncte sub ratingul tău de puzzle-uri","other":"%s de puncte sub ratingul tău de puzzle-uri"});i['nbToReplay']=p({"one":"%s de rejucat","few":"%s de rejucat","other":"%s de rejucat"});i['newStreak']="Începe o nouă secvență";i['nextPuzzle']="Puzzle-ul următor";i['noPuzzlesToShow']="Nimic de arătat, joacă mai întâi câteva puzzle-uri!";i['normal']="Normal";i['notTheMove']="Nu este asta mișcarea!";i['openingsYouPlayedTheMost']="Deschideri pe care le-ai jucat cel mai mult în meciurile evaluate";i['origin']="Origine";i['percentSolved']=s("%s rezolvate");i['phases']="Faze";i['playedXTimes']=p({"one":"Jucat %s dată","few":"Jucat de %s ori","other":"Jucat de %s ori"});i['puzzleComplete']="Puzzle complet!";i['puzzleDashboard']="Panoul de control pentru probleme de șah";i['puzzleDashboardDescription']="Antreneaza-te, analizează, fii mai bun";i['puzzleId']=s("Problema de șah %s");i['puzzleOfTheDay']="Problema zilei";i['puzzles']="Probleme de șah";i['puzzlesByOpenings']="Puzzle-uri după deschidere";i['puzzleSuccess']="Succes!";i['puzzleThemes']="Teme pentru problemele de șah";i['ratingX']=s("Scor: %s");i['recommended']="Recomandare";i['searchPuzzles']="Caută puzzle-uri";i['solved']="rezolvat";i['specialMoves']="Mutări speciale";i['streakDescription']="Rezolvă probleme din ce în ce mai grele și construiește un șir de victorii consecutive. Nu există limită de timp, deci nu te grăbi. O mișcare greșită și jocul s-a terminat! Dar poți sări peste o mutare în fiecare sesiune.";i['streakSkipExplanation']="Sări peste această mutare pentru a-ți păstra șirul de probleme consecutive reușite! Funcționează o singură dată pe sesiune.";i['strengthDescription']="Obțineți cele mai bune rezultate la aceste teme";i['strengths']="Puncte tari";i['toGetPersonalizedPuzzles']="Pentru a primi probleme de șah personalizate:";i['trySomethingElse']="Încercă altceva.";i['upVote']="Îmi place";i['useCtrlF']="Folosește Ctrl+f pentru a găsi deschiderea favorită!";i['useFindInPage']="Folosește \\\"Găsește în pagină\\\" în meniul browser-ului pentru a găsi deschiderea ta preferată!";i['voteToLoadNextOne']="Votează pentru a-l încărca pe următorul!";i['yourPuzzleRatingWillNotChange']="Evaluarea ta la puzzle-uri nu se va schimba. Iți amintim că puzzle-urile nu sunt o competiție. Evaluarea ajută la selectarea celor mai bune puzzle-uri care corespund nivelului tău.";i['yourStreakX']=s("Probleme consecutive: %s")})()