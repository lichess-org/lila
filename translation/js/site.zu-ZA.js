"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.site)window.i18n.site={};let i=window.i18n.site;i['abortGame']="Lahla umdlalo";i['accept']="Vuma";i['aiNameLevelAiLevel']=s("%1$s Izinga %2$s");i['allSet']="Konke kusethiwe!";i['analysis']="Ibhodi Yokuhlola";i['asBlack']="Ngokumnyama";i['asWhite']="Ngokumhlophe";i['averageRatingX']=s("Isilinganiselwa esijwayelekile: %s");i['backToTournament']="Buyela kumqhudelwano";i['bestMoveArrow']="Umcibisholo wokuhambisa ohamba phambili";i['black']="Mnyama";i['blackDeclinesDraw']="Imnyama iyenqaba ukudweba";i['blackDidntMove']="Umnyama akadlalanga";i['blackIsVictorious']="Omnyama unqobile";i['blackLeftTheGame']="Omnyama ushiye umdlalo";i['blackOffersDraw']="Okumnyama okunikezwayo";i['blackPlays']="Okumnyama ukudlala";i['blackResigned']="Okumnyama kususwe";i['blackTimeOut']="Isikhathi siphelili mnyama";i['blunder']="Ukuphaphalaza";i['bookmarkThisGame']="Beka uphawu lokubekisa lo mdlalo";i['byCPL']="Ngu-CPL";i['calculatingMoves']="Sibala imidlalo yakho...";i['cancel']="Khansela";i['cancelRematchOffer']="Khansela ukunikezwa kokuphindwayo";i['capture']="Impango";i['casual']="Okuvamile";i['casualTournament']="Okuvamile";i['changeEmail']="Shintsha i-imeyili";i['changePassword']="Shintsha iphasiwedi";i['changeUsername']="Shintsha igama lomsebenzisi";i['changeUsernameDescription']="Shintsha igama lakho lomsebenzisi. Lokhu kungenziwa kanye kuphela futhi uvunyelwe kuphela ukushintsha imeko yezinhlamvu ezisegama lakho lomsebenzisi.";i['changeUsernameNotSame']="Icala lezinhlamvu kuphela elingashintsha. Isibonelo \\\"johndoe\\\" kuya ku- \\\"JohnDoe\\\".";i['chat']="Ingxoxo";i['chatRoom']="Igumbi lokuxoxa";i['cheatDetected']="Utholakala urobha";i['checkmate']="Mui_season_name_76";i['claimADraw']="Funa umdwebo";i['close']="Vala";i['cloudAnalysis']="Ukuhlaziywa kwamafu";i['composeMessage']="Qamba umyalezo";i['computerAnalysis']="Ukuhlaziywa kwekhompyutha";i['computerAnalysisAvailable']="Ukuhlaziywa kwekhompyutha kuyatholakala";i['computerAnalysisDisabled']="Kucinyiwe ukuhlaziywa kwekhompuyuthwa";i['computersAreNotAllowedToPlay']="Amakhompyutha nabadlali abasizwa ngamakhompyutha abavunyelwe ukuthi badlale. Uyacelwa ukuthi ungatholi lusizo kwizinjini ze-chess, imininingwane yolwazi, noma kwabanye abadlali ngenkathi udlala. Futhi qaphela ukuthi ukwenza ama-akhawunti amaningi kudikibile kakhulu futhi ama-accounting amaningi ngokweqile angaholela ekuvinjelweni.";i['confirmMove']="Qinisekisa ukunyakaza";i['correspondence']="Ukuxhumana";i['cpus']="Ama-CPU";i['createAGame']="Dala umdlalo";i['currentGames']="Imidlalo yamanje";i['database']="Database";i['daysPerTurn']="Izinsuku ngakunye";i['decline']="Ehla";i['delete']="Susa";i['deleteFromHere']="Susa kusukela lapha";i['deleteThisImportedGame']="Susa lo mdlalo ongenisiwe?";i['depthX']=s("Ukujula %s");i['discussions']="Izingxoxo";i['draw']="Dweba";i['drawn']="Ashayekile";i['drawOfferAccepted']="Dweba okunikezwayo okwamukelwe";i['drawOfferCanceled']="Dweba ukunikezwa kukhanseliwe";i['drawOfferSent']="Dweba okunikezwayo kuthunyelwe";i['email']="I-imeyili";i['enable']="Nika amandla";i['engineFailed']="Injini iyahluleka ukuqhubeka";i['evaluationGauge']="Igeji yokuhlola";i['eventInProgress']="Iyadlala manje";i['exportGames']="Thekelisa imidlalo";i['finished']="Kuqediwe";i['flipBoard']="Ibhodi le-Flip";i['forceDraw']="Dweba ucingo";i['forceResignation']="Imangalo yokunqoba";i['forceVariation']="Phoqelela ukuhlukahluka";i['forgotPassword']="Ukhohlwe iphasiwedi?";i['forum']="Iforamu";i['freeOnlineChess']="I-Chess Yamahhala e-Inthanethi";i['friends']="Bangani";i['gameAborted']="Umdlalo uhoxisiwe";i['gameOver']="Game Over";i['games']="Imidlalo";i['gamesPlayed']="Imidlalo edlaliwe";i['giveNbSeconds']=p({"one":"Nikeza i-%s yesibili","other":"Nikeza i-%s imizuzwana"});i['goDeeper']="Hamba ngokujulile";i['importPgn']="Ngenisa PGN";i['inaccuracy']="Ukunemba";i['inbox']="Ibhokisi lokungenayo";i['incrementInSeconds']="Ukwanda ngamasekhondi";i['infiniteAnalysis']="Ukuhlaziywa okungenamkhawulo";i['inLocalBrowser']="kusiphequluli sendawo";i['insufficientMaterial']="Izinto ezinganele";i['itsYourTurn']="Yithuba lakho!";i['joinTheGame']="Joyina umdlalo";i['kingInTheCenter']="Inkosi phakathi";i['latestForumPosts']="Okuthunyelwe okusha kwenkundla";i['level']="Izinga";i['loadingEngine']="Ilayisha injini ...";i['loginToChat']="Ngena ngemvume ukuze uxoxe";i['logOut']="Phuma ngemvume";i['losing']="Ukulahleka";i['lossSavedBy50MoveRule']="Ukulahlekelwa kuvinjelwe umthetho wokuhamba ongu-50";i['makeMainLine']="Yenza umugqa owodwav";i['masterDbExplanation']=s("Imidlalo eyizigidi ezimbili ye-OTB yabadlali abalinganiselwa ku-%1$s+ FIDE kusuka ku-%2$s kuye ku-%3$s");i['mateInXHalfMoves']=p({"one":"Checkmate ku-%s ukuhambisa uhhafu","other":"Checkmate ku-%s ukuhamba uhhafu"});i['maybeIncludeMoreGamesFromThePreferencesMenu']="Mhlawumbe faka eminye imidlalo kusuka kumenyu yokuncamelayo?";i['memory']="Inkumbulo";i['minutesPerSide']="Amaminithi ohlangothini";i['mistake']="Iphutha";i['mode']="Imodi";i['moreThanNbPerfRatedGames']=p({"one":"%1$s %2$s kukalwe umdlalo","other":"%1$s %2$s imidlalo ekaliwe"});i['moreThanNbRatedGames']=p({"one":"%s kukalwe umdlalo","other":"%s imidlallo ekaliwe"});i['move']="Hambisa";i['moveTimes']="Izikhathi ze-Movie";i['multipleLines']="Imigqa eminingi";i['nbDays']=p({"one":"%s usuku","other":"%s izinsuku"});i['nbDraws']=p({"one":"%s ukulingana","other":"%s ukulingana"});i['nbGames']=p({"one":"%s umdlalo","other":"%s imidlalo"});i['nbGamesWithYou']=p({"one":"%s umdlalo nawe","other":"%s imidlalo nawe"});i['nbHours']=p({"one":"%s ihora","other":"%s amahora"});i['nbLosses']=p({"one":"%s ukulahlekelwa","other":"%s ukulahlekelwa"});i['nbPlayers']=p({"one":"%s isidlali","other":"%s abadlali"});i['nbPlaying']=p({"one":"%s ukudlala","other":"%s ukudlala"});i['nbPuzzles']=p({"one":"%s iphazili","other":"%s emaphazili"});i['nbRated']=p({"one":"%s kulinganiswe","other":"%s kulinganiswe"});i['nbStudies']=p({"one":"%s funda","other":"%s izifundo"});i['nbTournamentPoints']=p({"one":"%s amaphuzu Iomqhudelwano","other":"%s amaphuzu omqhudelwano"});i['nbWins']=p({"one":"%s ukunqoba","other":"%s ukuwina"});i['needNbMoreGames']=p({"one":"Udinga ukudlala %s okuningi umdlalo ukalwe","other":"Udinga ukudlala %s okuningi imidlalo ukalwe"});i['needNbMorePerfGames']=p({"one":"Udinga ukudlala %1$s okuningi %2$s umdlalo ukalwe","other":"Udinga ukudlala %1$s okuningi %2$s imidlalo ukalwe"});i['newOpponent']="Umphikisi omusha";i['noGameFound']="Awukho umdlalo otholakele";i['offerDraw']="Umdwebo Wokunikeza";i['oneDay']="Usuku olulodwa";i['openingExplorer']="Umhloli wokuvula";i['openingExplorerAndTablebase']="Ukuvula umhloli & ithebula";i['openStudy']="Vula isifundo";i['opponentLeftChoices']="Omunye umdlali kungenzeka ukuthi ushiye lo mdlalo. Ungacela ukunqoba, shayela umdlalo umdwebo, noma ulinde.";i['password']="Iphasiwedi";i['passwordReset']="Ukusetha kabusha kwephasiwedi";i['pawnMove']="Ukuhamba kwenduku";i['play']="Dlala";i['players']="Abadlali";i['playingRightNow']="Iyadlala manje";i['playWithAFriend']="Ukudlala nomngane";i['playWithTheMachine']="Ukudlala emshinini";i['promoteVariation']="Khuthaza ukushintshashintsha";i['proposeATakeback']="Phakamisa ukubuyisa";i['raceFinished']="Umjaho uqedile";i['randomColor']="Okungahleliwe";i['rank']="Isikhundla";i['rankX']=s("Izinga: %s");i['rated']="Kukalwe";i['ratedTournament']="Kukalwe";i['rating']="Isilinganiso";i['ratingRange']="Ububanzi besilinganiso";i['ratingStats']="Izibalo zesilinganiso";i['realTime']="Isikhathi sangempela";i['realtimeReplay']="Isikhathi sangempela";i['recentGames']="Imidlalo yakamuva";i['rematch']="Phinda ubuke";i['rematchOfferAccepted']="Ukuphinda kunikezwe kwamukelwe";i['rematchOfferCanceled']="Ukuphinda okunikezwayo kukhanseliwe";i['rematchOfferDeclined']="Ukunikezwa okuphindwayo kunqatshiwe";i['rematchOfferSent']="Ukuphinda kunikezwe kuthunyelwe";i['removesTheDepthLimit']="Isusa umkhawulo wokujula, futhi igcina ikhompyutha yakho ifudumele";i['replayMode']="Phinda wenze imodi";i['requestAComputerAnalysis']="Cela ukuhlaziywa kwekhompyutha";i['resign']="Yeka";i['send']="Thumela";i['showThreat']="Bonisa usongo";i['signIn']="Ngena ngemvume";i['signUp']="Bhalisa";i['spectatorRoom']="Igumbi lokubuka";i['stalemate']="I-Stalemate";i['standard']="Okujwayelekile";i['strength']="Amandla";i['subject']="Isihloko";i['takeback']="Buyela";i['takebackPropositionAccepted']="Ukubuyisela emuva kwamukelwe";i['takebackPropositionCanceled']="Ukubuyisela kukhanseliwe";i['takebackPropositionDeclined']="Ukubuyisa kwenqatshiwe";i['takebackPropositionSent']="Ukubuya kuthunyelwe";i['talkInChat']="Sicela ujabule engxoxweni!";i['theFirstPersonToComeOnThisUrlWillPlayWithYou']="Umuntu wokuqala oza kule URL uzodlala nawe.";i['thematic']="Izingqikithi";i['thisAccountViolatedTos']="Le akhawunti yephule Imigomo Yesevisi ye-Lichess";i['thisGameIsRated']="Kukalwe lo mdlalo";i['threeChecks']="Ukuhlola ezintathu";i['threefoldRepetition']="Ukuphindaphinda kathathu";i['time']="Isikhathi";i['timeControl']="Ukulawulwa kwesikhathi";i['today']="Namuhla";i['toggleLocalEvaluation']="Guqula ukuhlola kwendawo";i['toggleTheChat']="Guqula ingxoxo";i['toInviteSomeoneToPlayGiveThisUrl']="Ukuze umeme othile ukudlala, unike lolu kheli";i['topGames']="Best imidlalo";i['tournament']="Umqhudelwano";i['tournamentPoints']="Amaphuzu okuncintisana";i['tournaments']="Imiqhudelwano";i['unknown']="Akwaziwa";i['unlimited']="Akunamkhawulo";i['username']="Igama lomsebenzisi";i['usernameOrEmail']="Igama lomsebenzisi noma i-imeyili";i['usingServerAnalysis']="Ukusebenzisa ukuhlaziywa kweseva";i['variant']="Okuhlukile";i['variantEnding']="Ukuphela okuhlukile";i['variantLoss']="Ukulahlekelwa okuhlukile";i['variants']="Okuhlukile";i['variantWin']="Ukuhluka okuhlukile";i['viewInFullSize']="Buka ngosayizi ogcwele";i['viewRematch']="Bheka ukuphinda unikeze";i['viewTournament']="Buka umqhudelwano";i['waiting']="Ukulinda";i['waitingForOpponent']="Elinde isitha";i['white']="Okumhlophe";i['whiteDeclinesDraw']="Imidwebo emhlophe yenqaba";i['whiteDidntMove']="Umhlophe akadlalanga";i['whiteDrawBlack']="Mhlophe / Dweba / Mnyama";i['whiteIsVictorious']="Umhlophe unqobile";i['whiteLeftTheGame']="Umhlophe ushiye umdlalo";i['whiteOffersDraw']="White unikeza ukudweba";i['whitePlays']="Mhlophe ukuze udlale";i['whiteResigned']="Ukumiswa okumhlophe";i['whiteTimeOut']="Isikhathi siphelili elimhlophe";i['winning']="Ukunqoba";i['winPreventedBy50MoveRule']="Ukunqoba kuvinjelwe umthetho wokuhamba wama-50";i['xOpeningExplorer']=s("%s ukuvula umhloli");i['xPostedInForumY']=s("%1$s kuthunyelwe kusihloko %2$s");i['yesterday']="Izolo";i['youHaveBeenTimedOut']="Uphelelwe isikhathi.";i['youNeedAnAccountToDoThat']="Udinga i-akhawunti ukukwenza lokho";i['youPlayTheBlackPieces']="Udlala izingcezu ezimnyama";i['youPlayTheWhitePieces']="Udlala izingcezu ezimhlophe";i['yourOpponentOffersADraw']="Umphikisi wakho unikeza umdwebo";i['yourOpponentProposesATakeback']="Umphikisi wakho uphakamisa ukubuyiswa";i['yourOpponentWantsToPlayANewGameWithYou']="Umphikisi wakho ufuna ukudlala umdlalo omusha nawe";i['yourPerfRatingIsProvisional']=s("Isilinganiso sakho se-%s esesikhashana");i['yourPerfRatingIsTooHigh']=s("Isilinganiso sakho se-%1$s (%2$s) siphezulu kakhulu");i['yourTurn']="Ithuba lakho"})()