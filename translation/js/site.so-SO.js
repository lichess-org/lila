"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.site)window.i18n.site={};let i=window.i18n.site;i['abortGame']="Tuur ciyaartan";i['abortTheGame']="Tuur ciyaarta";i['aboutSimulImage']="50 ciyaarood Fischer wuxuu ka keenay 47 guulood, 2 baraaje iyo 1 guuldarro.";i['accept']="Aqbal";i['accountCanLogin']=s("Hadda waxad ku geli kartaa %s.");i['accountClosed']=s("Akoonka %s wuu xidhanyahay.");i['accountConfirmationEmailNotNeeded']="Uma baahnid iimayl hubsasho ah.";i['accountConfirmed']=s("Akoonka %s waa la hubiyay si guul leh.");i['accountRegisteredWithoutEmail']=s("Akoonka %s wuxuu diwaangashanyay bilaa iimayl.");i['aiNameLevelAiLevel']=s("%1$s heerka %2$s");i['allSet']="Diyaar!";i['analysis']="Looxa falanqaynta";i['anonymous']="Qarsoodi";i['asBlack']="madow ahaan";i['asWhite']="caddaan ahaan";i['averageOpponent']="Horjeedaha celceliska ah";i['averageRatingX']=s("Qiimaynta isku celceliska ah: %s");i['backToGame']="Ku noqo ciyaarta";i['backToTournament']="Ku noqo tartanka";i['bestMoveArrow']="Falaadha tallaabada ugu fiican";i['black']="Madow";i['blackDeclinesDraw']="Madowgu wuu diiday baraaje";i['blackDidntMove']="Madowgu muu dhaqaaqin";i['blackIsVictorious']="Madowgaa badiyey";i['blackLeftTheGame']="Madowgaa ka baxay ciyaarta";i['blackOffersDraw']="Madowgaa dalbaday baraaje";i['blackPlays']="Madowbaa ciyaaraya";i['blackResigned']="Madowgaa isdhiibay";i['blackTimeOut']="Wakhtigaa ka dhacay madowga";i['blankedPassword']="Fure la mid ah ayaad u isticmaashay bog kale. Bogaasna waa la jabsaday. Si ad u ilaaliso nabada akoonkaaga Lichess, waxad u baahantahay fure cusub. Waad ku mahadsantay in ad na fahantay.";i['blog']="Warbixino";i['blunder']="Qalad";i['byCPL']="Inta muhiinka ah";i['calculatingMoves']="Cabirid dhaqdhaqaaq...";i['cancel']="Ka noqo";i['cancelRematchOffer']="Ka noqo codsi ku celin ah";i['capture']="Qabasho";i['casual']="Aan qiimaysnayn";i['casualTournament']="Aan qiimaysnayn";i['changeEmail']="Beddel iimaylka";i['changePassword']="Beddel furaha";i['changeUsername']="Beddel magac-akoonka";i['changeUsernameDescription']="Magac-koonkaaga beddel. Mar keliyaa kuu banana oo aad bedeli karto weynida xarfaha uun.";i['changeUsernameNotSame']="Weynida xarfaha oo kaliiyaa is bedeli kara. Tusaale ahaan \\\"hebel\\\" iyo \\\"Hebel\\\".";i['chat']="U qor";i['chatRoom']="Luuqa";i['cheatDetected']="Dareen khiyaamo";i['checkmate']="Jeg-meyd";i['checkSpamFolder']="Sidoo kale eeg spam folderkaaga, halkaasaad ka heli kartaa. Haddii ay sidaa tahay, u beddel \\\"not spam\\\".";i['chessBasics']="Aasaaska jesta";i['claimADraw']="Ku dhawaaq baraaje";i['clockIncrement']="Taranka saacadda";i['close']="Xidh";i['cloudAnalysis']="Falanqayn hawo";i['coaches']="Tababarayaal";i['collapseVariations']="Burbur kala duwanaanshiyaha";i['community']="Bulshada";i['composeMessage']="Farriin samee";i['computerAnalysis']="Falanqayn komyuutar";i['computerAnalysisAvailable']="Falanqayn komyuutar baa jirta";i['computerAnalysisDisabled']="Falanqayn komyuutar ma furna";i['computersAreNotAllowedToPlay']="Komyuutar iyo qof qish komyuutar isticmaalaya lama ogola. Fadlan ha isticmaalin qish komyuutar ama caawin qof kale markad ciyaaraysid. Iyana ogow in samayska akoono badan aad looga soo hor jeedo, oo haddii ad sidaa samaysid akoonkaaga la xidhi karo.";i['confirmMove']="Hubso tallaabada";i['copyTextToEmail']=s("Koobi garee qoraalka sare oo u soo dir %s");i['copyVariationPgn']="Koobi garee PGNka faracan";i['correspondence']="Maalinle";i['correspondenceChess']="Jes maalinle ah";i['correspondenceDesc']="Jes maalinle ah: hal ama dhowr cisho dhaqaaqiiba";i['cpus']="CPUyada";i['create']="Abuur";i['createAGame']="Bilow ciyaar cusub";i['currentGames']="Ciyaaraha hadda";i['customPosition']="Booska gaarka ah";i['dark']="Madow";i['database']="Keydka weyn";i['daysPerTurn']="Maalmood doorkiiba";i['decline']="Diid";i['delete']="Tuur";i['deleteFromHere']="Ka tuur halkan";i['deleteThisImportedGame']="Tuur ciyaartan la soo geliyey?";i['depthX']=s("Mug %s");i['discussions']="Sheekaysiyada";i['draw']="Baraaje";i['drawByMutualAgreement']="Heshiis baraaje";i['drawn']="Barbardhac";i['drawOfferAccepted']="Waa la aqbalay baraaje";i['drawOfferCanceled']="Waa laga noqday dalbasho baraaje";i['drawOfferSent']="La dir dalbasho baraaje";i['dtzWithRounding']="DTZ50\\\" la soo gaabiyey, kuna salaysan tirada badh-dhaqaaq ee ka hadhay qabashada ama dhaqaaqa askari ee xiga";i['email']="Iimayl";i['emailCanTakeSomeTime']="Wakhti yar sii si ay kuu soo gaadho.";i['emailConfirmHelp']="Caawin hubinta iimaylka";i['emailConfirmNotReceived']="Maad helin iimaylka hubinta diwaangelin ka dib?";i['emailForSignupHelp']="Haddii ay waxba shaqayn waayaan, noosoo dir iimaylkan:";i['emailSent']=s("Iimayl baan u dirnay %s.");i['emailSuggestion']="Wax laguu sheegay ha ka dhigan iimayl. Waxa lagaa xadi karaan akoonka.";i['enable']="Fur";i['engineFailed']="Qalad adeege";i['engineManager']="Maareeyaha mishiinka";i['error.namePassword']="Fadlan magaca-akoonka boggaaga ha ka dhigan fure.";i['error.weakPassword']="Furahani waa caan, si fidud baa loo nasiibin karaa.";i['evaluationGauge']="Qiinqaynta tallaabada";i['eventInProgress']="Ciyaarta hadda";i['expandVariations']="Balaadhi kala duwanaanshiyaha";i['favoriteOpponents']="Horjeedayaasha aad jeceshay";i['fiftyMovesWithoutProgress']="Konton dhaqaaq bilaa natiijo";i['finished']="Dhameystiray";i['flipBoard']="Rog looxa";i['followX']=s("La soco %s");i['forceDraw']="Baraaje dalbo";i['forceResignation']="Guusha qaado";i['forceVariation']="Khasab faracan";i['forgotPassword']="Ma ilowday furaha?";i['forum']="Barta bulshada";i['freeOnlineChess']="Shataranjiga Onlineka ah ee bilaashka ah";i['friends']="Saaxiibo";i['gameAborted']="Ciyaartan waa la tuuray";i['gameInProgress']=s("Waxaad la ciyaaraysaa hadda %s.");i['gameOver']="Ciyaarti way dhamaatay";i['games']="Ciyaaraha";i['gamesPlayed']="Ciyaaraha dhamaaday";i['gameVsX']=s("Ciyaarta >< %1$s");i['goDeeper']="Sii gudagal";i['hangOn']="Is deji!";i['importedByX']=s("Waxa soo geliyey %s");i['importPgn']="Soo geli PGN";i['inaccuracy']="Khalad";i['inbox']="Sanduuqa";i['increment']="Taran";i['incrementInSeconds']="Taran ah sikino";i['infiniteAnalysis']="Falanqayn bilaa xad ah";i['inLocalBrowser']="aaladdaada";i['insufficientMaterial']="Qalab yari";i['invalidUsernameOrPassword']="Magacan-akoonkan ama iimayl khaldan";i['isPrivate']="Gaar ah";i['itsYourTurn']="Waa markaagii!";i['join']="Ku biir";i['joinTheGame']="Kubiir ciyaarta";i['kingInTheCenter']="Dhexdu boqran";i['language']="Luqadda";i['latestForumPosts']="Qoraalkii u dambeeyay ee barta bulshada";i['learnMenu']="Baro";i['level']="Heerka";i['lichessDbExplanation']="Ciyaaraha qiimaysan ee ka dhacay Lichess";i['lifetimeScore']="Dhibcaha abid";i['light']="Iftiin";i['loadingEngine']="Shaqayn adeege...";i['lobby']="Dooro kulan";i['location']="Goobta";i['loginToChat']="Gal luuqa";i['logOut']="Ka bax";i['losing']="Lumin";i['lossOr50MovesByPriorMistake']="Guuldarro ama 50 tallaabo khalad hore dartii";i['lossSavedBy50MoveRule']="Xeerka 50 tallaabo ayaa diiday guuldarro";i['makeMainLine']="Sida ugu caansan ka dhig";i['masterDbExplanation']=s("Ciyaaraha MD ee %1$s+ ciyaartoyda FIDE ee %2$s ilaa %3$s");i['mateInXHalfMoves']=p({"one":"Mayd %s badh-dhaqaaq gudihii","other":"Mayd %s badh-dhaqaaq gudohood"});i['maxDepthReached']="Muggii ugu weynaa la gaadh!";i['maybeIncludeMoreGamesFromThePreferencesMenu']="Malaha kaga soo dar ciyaaro kale meesha doorashada?";i['memory']="Baaxadda";i['minutesPerSide']="Mirirada dhinaciiba";i['mistake']="Qalad";i['mode']="Nooca";i['more']="Ka badan";i['move']="Dhaqaaq";i['moveTimes']="Wakhtiyada tallaabo";i['multipleLines']="Shaxo badan";i['nbBlunders']=p({"one":"%s qalad","other":"%s qaladaad"});i['nbBookmarks']=p({"one":"%s calaamad","other":"%s calaamadood"});i['nbDays']=p({"one":"%s maalin","other":"%s maalmood"});i['nbGames']=p({"one":"%s ciyaar","other":"%s ciyaarood"});i['nbGamesInPlay']=p({"one":"%s ciyaar baa socota","other":"%s ciyaarood baa socda"});i['nbGamesWithYou']=p({"one":"%s mar buu kula kulmay","other":"%s mar buu kula kulmay"});i['nbHours']=p({"one":"%s saacad","other":"%s saacadood"});i['nbInaccuracies']=p({"one":"%s aan fiicnayn","other":"%s aan fiicnayn"});i['nbMinutes']=p({"one":"%s mirir","other":"%s mirir"});i['nbMistakes']=p({"one":"%s halmaam","other":"%s halmaamyo"});i['nbPlayers']=p({"one":"%s ciyaartow","other":"%s ciyaartoy"});i['nbPlaying']=p({"one":"%s ciyaaraya","other":"%s ciyaaraya"});i['nbPuzzles']=p({"one":"%s xujooyinka","other":"%s xujo"});i['nbStudies']=p({"one":"%s cashar","other":"%s cashar"});i['never']="Marna";i['neverTypeYourPassword']="Weligaa ha ku qorin furaha Lichess bog kale!";i['newOpponent']="Horjeede cusub";i['next']="Xiga";i['noGameFound']="Ciyaari ma jirto";i['notes']="Qoraalada";i['offerDraw']="Codso baraaje";i['oneDay']="Hal maalin";i['openingEndgameExplorer']="Furitaan/dhamaad baadhaha";i['openingExplorer']="Furitaan baadhaha";i['openings']="Furitaanada";i['openStudy']="Fur casharka";i['opponent']="Horjeede";i['opponentLeftChoices']="Horjeedahaagi wuu ka baxay ciyaarta. Waxad kala dooran kartaa badis, baraaje ama sug.";i['opponentLeftCounter']=p({"one":"Horjeedahaagi wuu ka baxay ciyaarta. Waad badin kartaa %s sikin gudihii.","other":"Horjeedahaagi wuu ka baxay ciyaarta. Waad badin kartaa %s sikin gudohood."});i['orLetYourOpponentScanQrCode']="Ama qofka kaa soo horjeeda ha sawiro summada QR-kan";i['otherPlayers']="ciyaartoyda kale";i['password']="Erey sir";i['passwordReset']="Cusboonaysii furaha";i['passwordSuggestion']="Fure laguu sheegay ha ka dhigan fure. Waxay Kaa xadi karaan akoonka.";i['pawnMove']="Dhaqaaq askari";i['play']="Ciyaar";i['player']="Ciyaaryahan";i['players']="Ciyaartoyda";i['playFirstOpeningEndgameExplorerMove']="Ciyaar dhaqaaqa ugu horeeya ee furitaan/dhamaad-baadhaha";i['playingRightNow']="Ciyaarta hadda";i['playWithAFriend']="La ciyaar saaxiib";i['playWithTheMachine']="La ciyaar komyuutar";i['points']="Dhibco";i['proceedToX']=s("Ku sii soco %s");i['promoteVariation']="Hore u wad faracan";i['puzzleDesc']="Tababaraha xeeladaha jesta";i['puzzles']="Xujooyin";i['quickPairing']="Kulan degdeg ah";i['raceFinished']="Baratanki wuu dhamaaday";i['randomColor']="Qori tuur";i['rank']="Darajo";i['rankIsUpdatedEveryNbMinutes']=p({"one":"Qiimayntu waxay cusboonaataa mirir kasta","other":"Qiimayntu waxay cusboonaataa %s mirir oo kasta"});i['rankX']=s("Darajo: %s");i['rated']="Qiimaysan";i['ratedTournament']="Qiimaysan";i['rating']="Qiimaynta";i['ratingRange']="Qiimayntayda";i['ratingStats']="Daraasada qiimaynta";i['ratingXOverYGames']=p({"one":"Darajada %1$s ee %2$s ciyaar","other":"Darajada %1$s ee %2$s ciyaarood"});i['realTime']="Imika";i['realtimeReplay']="Waqtiga dhabta ah";i['recentGames']="Ciyaarihii ugu bambeeyey";i['refreshInboxAfterFiveMinutes']="Sug 5 mirir oo ka dib baadh iimaylkaaga.";i['rematch']="Ku celi";i['rematchOfferAccepted']="Waa la aqbalay codsi ku celin ah";i['rematchOfferCanceled']="Waa laga noqday codsi ku celin ah";i['rematchOfferDeclined']="Waa la diiday codsi ku celin ah";i['rematchOfferSent']="La dir codsi ku celin ah";i['rememberMe']="Ha ka bixin";i['removesTheDepthLimit']="Mug bilaa xad ah oo komyuutarkaaga kululaynaya";i['replayMode']="Qaabka ku soo celinta";i['requestAComputerAnalysis']="Dalbo falanqayn komyuutar";i['reset']="Dib u dajin";i['resign']="Is dhiib";i['resignTheGame']="Is dhiib ciyaartan";i['save']="Badbaadiyo";i['send']="Dir";i['showHelpDialog']="Caawintan tus";i['showThreat']="Tus khatarta";i['showVariationArrows']="Tus falaadhaha faracyada";i['signIn']="Gal";i['signUp']="Is diwaangeli";i['signupEmailHint']="Waxan u isticmaalaynaa cusbaynta furahaaga oo keli ah.";i['signupUsernameHint']="Magac-akoon edeb leh dooro. Ma bedeli kartid mar dambe waana la xidhi doonaa magicii edeb darro ah!";i['since']="Ka dib";i['siteDescription']="Ciyaarta jesta oo onlayn ah bilaashna ah. Ku ciyaar jes madal nadiif ah. Uma baahna diwaangelin, ma leh xayeysiis, umana baahna adeeg kale. La ciyaar jes komyuutarka, asxaabtaada amma dadka hawada ku jira.";i['solution']="Xalka";i['sorry']="Waan ka xumahay :(";i['sound']="Cod";i['spectatorRoom']="Qolka daawadaha";i['stalemate']="Ismariwaa";i['standard']="Heerka";i['strength']="Adeyg";i['subject']="Hordhac";i['switchSides']="Beddel kooxda";i['talkInChat']="Fadlan si wanaagsan u isticmal luuqa!";i['theFirstPersonToComeOnThisUrlWillPlayWithYou']="Qofka ugu horreeya ee laynkan soo raacaa kula ciyaaraya.";i['thisGameIsRated']="Ciyaartani way qiimaysantay";i['threeChecks']="Sadex jeg";i['threefoldRepetition']="Ku celin sadex jeer";i['time']="Waqtiga";i['timeControl']="Xadka wakhtiga";i['today']="Maanta";i['toggleLocalEvaluation']="Dooro flanqaynta aaladdaada";i['toggleTheChat']="Fur luuqa";i['toInviteSomeoneToPlayGiveThisUrl']="Si aad u casuumtid qof kula ciyaara, u dir laynkan";i['tools']="Qalabka";i['topGames']="Ciyaaraha ugu sareeya";i['transparent']="Hufan";i['typePrivateNotesHere']="Halkan ku qor qoraalo kuu gooni ah";i['unfollowX']=s("Ka hadh %s");i['unknown']="Lama garanayo";i['unknownDueToRounding']="Guul ama guuldarro kaliya ayaa la dammaanad qaaday haddii khadka miiska miiska lagu taliyay la raaco tan iyo qabsashadii ugu dambeysay ama dhaqaaqii u dambeeyay, taasoo ay ugu wacan tahay isku dhafka suurtagalka ah ee qiyamka DTZ ee saldhigyada miiska Syzygy.";i['unlimited']="Bilaa xad";i['until']="Ka hor";i['username']="Magaca-akoon";i['usernameAlreadyUsed']="Magacan-akoonkan waa la haystaa, fadlan tijaabi mid kale.";i['usernameCanBeUsedForNewAccount']="Magacan-akoonkan waxad ku samayn kartaa akoon cusub";i['usernameCharsInvalid']="Magacan-akoonka waxa u banana xarfo, tiro, xarriiq hoose, iyo xarriiq isku xidhe ah. Xarriiquhu iskuma xigi karaan.";i['usernameNotFound']=s("Maanu helin akoon magacan leh: %s.");i['usernameOrEmail']="Magac-akoon ama iimayl";i['usernamePrefixInvalid']="Magacan-akoonku waa inuu ku bilaabmo xaraf.";i['usernameSuffixInvalid']="Magacan-akoonku waa inuu ku dhamaado xaraf ama tiro.";i['usernameUnacceptable']="Magacan-akoonkan lama aqbali karo.";i['usingServerAnalysis']="Miciinsi falanqayn adeege";i['variant']="Faraca";i['variantEnding']="Dhamaadka noocan";i['variantLoss']="Guukdarro nooc";i['variants']="Faracyada";i['variantWin']="Nooc badis";i['victory']="Guulayso";i['viewInFullSize']="Weynee daaqadda";i['viewRematch']="Eeg ciyaarta ku celinta ah";i['viewTheSolution']="Xalka eeg";i['waitForSignupHelp']="Waanu kuu soo laaban in yar ka dib si an kaaga caawinno diwaangelinta.";i['waiting']="Sugaya";i['waitingForOpponent']="Sugaya herjeede cusub";i['watch']="Daawo";i['watchGames']="Daawo ciyaaro";i['whatSignupUsername']="Magac-akoonkee baad u isticmaashay diwaangelinta?";i['white']="Cadaan";i['whiteDeclinesDraw']="Caddaanku wudu diiday baraaje";i['whiteDidntMove']="Cadaanku muu dhaqaaqin";i['whiteDrawBlack']="Caddaan / Baraaje / Madow";i['whiteIsVictorious']="Cadaankaa badiyey";i['whiteLeftTheGame']="Cadaankaa ka baxay ciyaarta";i['whiteOffersDraw']="Cadaankaa dalbaday baraaje";i['whitePlays']="Cadaanbaa baa ciyaaraya";i['whiteResigned']="Caddaankaa isdhiibay";i['whiteTimeOut']="Wakhtigii ba ka dhamaaday cadaanka";i['why']="Waayo?";i['winning']="Guuleysta";i['winOr50MovesByPriorMistake']="Guul ama 50 tallaabo khalad hore dartii";i['winPreventedBy50MoveRule']="Xeerka 50 tallaabo ayaa diiday guul";i['xJoinedTeamY']=s("%1$s wuxu ku biiray kooxda %2$s");i['xOpeningExplorer']=s("Furitaan baadhaha %s");i['xPostedInForumY']=s("%1$s ayaa ku soo qoray sheekada %2$s");i['yesterday']="Shalay";i['youAreLeavingLichess']="Waad ka baxaysaa Lichess";i['youCantStartNewGame']="Ciyaar kale ma bilaabi kartid inta aanay tani fhamaan.";i['youHaveBeenTimedOut']="Wakhtigii baa kaa dhamaaday.";i['youNeedAnAccountToDoThat']="Akoon baad u baahanahay";i['youPlayTheBlackPieces']="Waxaad tahay kooxda madow";i['youPlayTheWhitePieces']="Waxaad tahay kooxda cad";i['yourOpponentOffersADraw']="Horjeedahaagu wuxu kaa dalbaday baraaje";i['yourOpponentProposesATakeback']="Horjeedahaagu wuxuu kaa dalbaday ka noqosho";i['yourOpponentWantsToPlayANewGameWithYou']="Horjeedahaagu ciyaar cusub buu kaa rabaa";i['yourScore']=s("Dhibcahaaga: %s");i['yourTurn']="Waa markaagii"})()