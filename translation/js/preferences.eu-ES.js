"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.preferences)window.i18n.preferences={};let i=window.i18n.preferences;i['bellNotificationSound']="Kanpaiaren jakinarazpen soinua";i['boardCoordinates']="Taularen koordinatutak (A-H, 1-8)";i['boardHighlights']="Taulan markak erakutsi (azken jokaldia eta xakea)";i['bothClicksAndDrag']="Edozein";i['castleByMovingOntoTheRook']="Erregea gazteluaren gainera mugitu";i['castleByMovingTheKingTwoSquaresOrOntoTheRook']="Endrokatzeko modua";i['castleByMovingTwoSquares']="Erregea bi lauki mugitu";i['chessClock']="Xake-erlojua";i['chessPieceSymbol']="Xake piezen ikurra";i['claimDrawOnThreefoldRepetitionAutomatically']="Posizioa hirutan errepikatzen denean berdinketa automatikoki eskatu";i['clickTwoSquares']="Bi laukitan klik eginda";i['confirmResignationAndDrawOffers']="Etsitze eta berdinketa eskaeren baieztapena eskatu";i['correspondenceAndUnlimited']="Posta-xakea,  denbora-eperik gabe";i['correspondenceEmailNotification']="Jaso posta elektronikoz zure posta-bidezko partiden jakinarazpenen zerrenda egunero";i['display']="Erakutsi";i['displayBoardResizeHandle']="Xake-taularen tamaina aldatzeko aukera erakutsi";i['dragPiece']="Pieza arrastatuta";i['explainCanThenBeTemporarilyDisabled']="Partida baten zehar taularen menua erabiliz desaktibatu daiteke";i['explainPromoteToQueenAutomatically']="Eutsi <ctrl> tekla sakatuta sustapenean sustapen automatikoa aldi baterako desaktibatzeko";i['explainShowPlayerRatings']="Horri esker, gunearen puntuazio guztiak ezkutatu daitezke, xakean arreta jartzen laguntzeko. Partidak puntuka izan daitezke oraindik, hau da zuk ikus dezakezuna.";i['gameBehavior']="Partidaren portaera";i['giveMoreTime']="Denbora gehiago eman";i['horizontalGreenProgressBars']="Aurrerabide-barra berdea horizontalki";i['howDoYouMovePieces']="Nola mugitzen dituzu piezak?";i['inCasualGamesOnly']="Puntu-aldaketarik gabeko partidetan bakarrik";i['inCorrespondenceGames']="Posta bidezko partidak";i['inGameOnly']="Partidan zehar bakarrik";i['inputMovesWithTheKeyboard']="Jokaldiak teklatuarekin sartu";i['inputMovesWithVoice']="Egin jokaldiak zure ahotsarekin";i['materialDifference']="Material desoreka";i['moveConfirmation']="Jokaldia baieztatzea";i['moveListWhilePlaying']="Jokaldi-zerrenda partidan zehar";i['notifications']="Jakinarazpenak";i['notifyBell']="Kanpai bidezko jakinarazpena Lichess barruan";i['notifyChallenge']="Erronkak";i['notifyDevice']="Gailua";i['notifyForumMention']="Foroko erantzunean aipatu zaituzte";i['notifyGameEvent']="Posta bidezko partidetan eguneraketa";i['notifyInboxMsg']="Mezu berria postontzian";i['notifyInvitedStudy']="Azterlanreko gonbidapena";i['notifyPush']="Gailuko jakinarazpena Lichessen ez zaudenean";i['notifyStreamStart']="Streamerra zuzenean dago";i['notifyTimeAlarm']="Posta bidezko partidaren denbora amaitzen ari da";i['notifyTournamentSoon']="Txapelketa laster hasiko da";i['notifyWeb']="Nabigatzailea";i['onlyOnInitialPosition']="Hasierako posizioan bakarrik";i['pgnLetter']="Hizkiak (K, Q, R, B, N)";i['pgnPieceNotation']="Jokaldien idazketa";i['pieceAnimation']="Piezen animazioa";i['pieceDestinations']="Piezen norakoak (jokaldi zuzenak eta aurre-jokaldiak)";i['preferences']="Lehentasunak";i['premovesPlayingDuringOpponentTurn']="Aldez aurretik mugitzea (aurkariaren txanda den bitartean mugitu)";i['privacy']="Pribatutasuna";i['promoteToQueenAutomatically']="Dama automatikoki sustatzea";i['sayGgWpAfterLosingOrDrawing']="Txateam \\\"Good game, well played\\\" esan partida galdu edo berdintzean";i['scrollOnTheBoardToReplayMoves']="Mugitu taula gainean jokaldiak ikusteko";i['showFlairs']="Ikusi jokalarien iruditxoak";i['showPlayerRatings']="Erakutsi jokalarien puntuazioak";i['snapArrowsToValidMoves']="Marraztutako geziak legezko jokaldietara mugatu";i['soundWhenTimeGetsCritical']="Soinua jo denbora bukatzear dagoenean";i['takebacksWithOpponentApproval']="Jokaldia atzera botatzea  (aurkariaren onespenarekin)";i['tenthsOfSeconds']="Segundo-hamarrenak erakutsi";i['whenPremoving']="Aldez aurreko jokaldia egiten denean";i['whenTimeRemainingLessThanTenSeconds']="Denbora 10 segundotik behera dagoenean";i['whenTimeRemainingLessThanThirtySeconds']="30 segundo baino gutxiago geratzen denean";i['yourPreferencesHaveBeenSaved']="Zure ezarpenak ondo gorde dira.";i['zenMode']="Zen modua"})()