"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.preferences)window.i18n.preferences={};let i=window.i18n.preferences;i['bellNotificationSound']="Glacken-Notifikatiounstoun";i['boardCoordinates']="Brietkoordinaten (A-H, 1-8)";i['boardHighlights']="Felder um Briet ervirhiewen (leschten Zuch a Schach)";i['bothClicksAndDrag']="Béides";i['castleByMovingOntoTheRook']="Kinnek op Tuerm beweegen";i['castleByMovingTheKingTwoSquaresOrOntoTheRook']="Rochade-Method";i['castleByMovingTwoSquares']="Kinnek zwee Felder beweegen";i['chessClock']="Schachauer";i['chessPieceSymbol']="Schachfiguresymbol";i['claimDrawOnThreefoldRepetitionAutomatically']="Remis duerch dräifach Stellungswidderhuelung reklaméieren";i['clickTwoSquares']="Zwee Felder klicken";i['confirmResignationAndDrawOffers']="Resignatioun a Remis-Offere confirméieren";i['correspondenceAndUnlimited']="Korrespondenz an onbegrenzt";i['correspondenceEmailNotification']="Deegleg Email mat Lëscht vun Korrespondenzpartien";i['display']="Usiicht";i['displayBoardResizeHandle']="Regeler fir Brietgréisst ze änneren weisen";i['dragPiece']="Figur zéien";i['explainCanThenBeTemporarilyDisabled']="Kann während der Partie desaktivéiert ginn iwwert den Brietmenü";i['explainPromoteToQueenAutomatically']="Dréck ob deng <ctrl> Tasten während der Emwandlung fir temporär déi automatesch Emwandlung ze desaktivéieren";i['explainShowPlayerRatings']="Verstopp all Wäertungen op der Websäit, fir dass du dech voll op de Schach konzentréieren kanns. Partien können ëmmer nach gewäert sinn, et geet just drëm, wat du gesäis.";i['gameBehavior']="Spillverhalen";i['giveMoreTime']="Zäit bäiginn";i['horizontalGreenProgressBars']="Horizontalen gréngen Fortschrëttsbalken";i['howDoYouMovePieces']="Wéi Figuren beweegen?";i['inCasualGamesOnly']="Just an ongewäerten Partien";i['inCorrespondenceGames']="Korrespondenz Schach";i['inGameOnly']="Nëmmen während enger Partie";i['inputMovesWithTheKeyboard']="Zich mat der Tastatur aginn";i['inputMovesWithVoice']="Zich per Sproocherkennung aginn";i['materialDifference']="Materialënnerscheed";i['moveConfirmation']="Zich confirméieren";i['moveListWhilePlaying']="Zuchlëscht wärend dem Spillen";i['notifications']="Benoriichtegungen";i['notifyBell']="Benoriichtegung ob Lichess";i['notifyChallenge']="Erausfuerderungen";i['notifyDevice']="Gerät";i['notifyForumMention']="Forenkommentar ernimmt dech";i['notifyGameEvent']="Korrespondenzpartien Updates";i['notifyInboxMsg']="Nei Privatnoriicht";i['notifyInvitedStudy']="Etüdeninvitatioun";i['notifyPush']="Gerät Benoriichtegung wanns du net ob Lichess bass";i['notifyStreamStart']="Streamer geet live";i['notifyTimeAlarm']="Zäit an Korrespondenzpartie leeft of";i['notifyTournamentSoon']="Turnéier fänkt gleich un";i['notifyWeb']="Web-Browser";i['onlyOnInitialPosition']="Just an Startpositioun";i['pgnLetter']="Buschtaf (K, Q, R, B, N)";i['pgnPieceNotation']="Zuchnotatioun";i['pieceAnimation']="Figurenanimatioun";i['pieceDestinations']="Zilfelder markéieren (legal Zich a Virauszich)";i['preferences']="Astellungen";i['premovesPlayingDuringOpponentTurn']="Virauszich (wärend dem Géigner sengem Zuch spillen)";i['privacy']="Privatsphär";i['promoteToQueenAutomatically']="Automatesch an eng Damm ëmwandelen";i['sayGgWpAfterLosingOrDrawing']="No Defaite oder Remis \\\"Good game, well played\\\" (Gudd Partie, gudd gespillt) soen";i['scrollOnTheBoardToReplayMoves']="Scroll iwwer d\\'Briet fir Zich nozespillen";i['showPlayerRatings']="Spillerwäertungen uweisen";i['snapArrowsToValidMoves']="Feiler können just legal Zich weisen";i['soundWhenTimeGetsCritical']="Toun wann Zäit kritesch gëtt";i['takebacksWithOpponentApproval']="Zeréckhuelen (mat Zoustemmung vum Géigner)";i['tenthsOfSeconds']="Zéngtelsekonnen";i['whenPremoving']="Wann Virauszuch";i['whenTimeRemainingLessThanTenSeconds']="Wann verbleiwend Zäit < 10 Sekonnen";i['whenTimeRemainingLessThanThirtySeconds']="Wann verbleiwend Zäit < 30 Sekonnen";i['yourPreferencesHaveBeenSaved']="Deng Astellungen goufen gespäichert.";i['zenMode']="Zen-Modus"})()