"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.site)window.i18n.site={};let i=window.i18n.site;i['aiNameLevelAiLevel']=s("%1$slevel%2$s");i['analysis']="Bhodhi rekuongorora";i['asBlack']="achitamba neBlack";i['asWhite']="achitamba neWhite";i['averageRatingX']=s("Mwero pakati: %s");i['black']="Tema";i['blackDidntMove']="Black haana kutamba";i['blackIsVictorious']="Tema yawhina";i['blackLeftTheGame']="Itema yabuda mumutambo";i['blackPlays']="Itema yakutamba";i['blackResigned']="Itema yaregera";i['blunder']="Kukanganisa";i['calculatingMoves']="Tirikumboverenga maMoves, Mira...";i['capture']="Kudya mwana";i['chat']="Kutaura";i['cheatDetected']="Tabata kubirira";i['checkmate']="Wadyiwa";i['claimADraw']="Tora draw";i['close']="Vhara";i['cloudAnalysis']="Ongororo muCloud";i['computerAnalysis']="Analysis yemushini";i['computerAnalysisAvailable']="Analysis yemushini iripo";i['computerAnalysisDisabled']="Muwongororo weComputer wabviswa";i['cpus']="CPUs";i['createAGame']="Gadzira mutambo";i['currentGames']="Ma games aizvezvi";i['database']="Zvinopa user sarudzo pakati pelichess kana Master databases";i['deleteFromHere']="Dzima muno";i['depthX']=s("Udzamu %s");i['draw']="Draw";i['drawn']="Draw";i['engineFailed']="KuLoader Engine hakuna kubudirira";i['flipBoard']="Tenderedza pekutambira";i['forceDraw']="Daidza draw";i['forceResignation']="Tora win";i['forceVariation']="Manikidzira mutsara";i['gameOver']="Mutambo wapera";i['goDeeper']="Dzamisa";i['inaccuracy']="Inaccuracy";i['infiniteAnalysis']="Analysis isingapere";i['inLocalBrowser']="mubrowser remuchina uno";i['insufficientMaterial']="Vana vasingakwane";i['itsYourTurn']="Yave nguva yako!";i['joinTheGame']="Pinda mumutamba";i['kingInTheCenter']="Mambo pakati";i['level']="Level";i['loadingEngine']="Injini irikulodha...";i['logOut']="Buda";i['losing']="Inodyisa";i['makeMainLine']="Kudza mutsara uno";i['masterDbExplanation']=s("Mitambo 2 million yakatambwa paBoard chairo chairo, haikona online nevanwe Mwero ye %1$s+ kubva pa %2$s to %3$s");i['memory']="Rangariro";i['mistake']="Chitadzo";i['move']="Move";i['moveTimes']="Kwafambwa kangani";i['nbBookmarks']=p({"one":"%s bookmark","other":"%s bookmarks"});i['nbGames']=p({"one":"%s mutambo","other":"%s vatambo"});i['nbPlayers']=p({"one":"%s mutambi","other":"%s vatambi"});i['newOpponent']="Mumwe muvengi";i['offerDraw']="Kumbira draw";i['opponentLeftChoices']="Munhu abuda mumutambo.Unogona kutora win, kutora draw, kana kumumirira";i['opponentLeftCounter']=p({"one":"Munhu abuda mumutambo.Unogona kutora win nema seconds anoti %s.","other":"Munhu abuda mumutambo.Unogona kutora win nema seconds anoti %s."});i['pawnMove']="Kufamba kwePawn";i['playWithAFriend']="Tamba ne shamwari";i['playWithTheMachine']="Tamba ne Mushini";i['promoteVariation']="Kwidza mutsara uno";i['raceFinished']="Mutambi wapera";i['randomColor']="Chero divi";i['recentGames']="Mitambo ichangopfuura";i['removesTheDepthLimit']="Bvisa maLimit anodziisa mushini wako";i['requestAComputerAnalysis']="Kumbira analysis yemushini";i['resign']="Siya game";i['showThreat']="Ratidza Chanongedzwa";i['signIn']="Pinda";i['signUp']="Register";i['stalemate']="Stalemate";i['strength']="Hugoni";i['talkInChat']="Taura zvakanaka nevamwe!";i['theFirstPersonToComeOnThisUrlWillPlayWithYou']="Munhu wekutanga kuuya ne URL ndiye anotamba newe.";i['threeChecks']="MaChecks matatu";i['threefoldRepetition']="Kudzokorodza katatu";i['toggleLocalEvaluation']="Batidza/dzima ongororo yemuchina uno";i['toggleTheChat']="Kuchinja pekutaura";i['toInviteSomeoneToPlayGiveThisUrl']="Kudaidza munhu kuti mutambe shandisa URL iyi";i['topGames']="Mitambo yepamusoro soro";i['unknown']="Zvinobuda kwokupedzisira kwemutambo tablebase kana chibudochenzvimbo yakapihwa isingazivikanwe";i['usingServerAnalysis']="Ongororo yeServer";i['variantEnding']="Hapachisina chekutamba";i['variantLoss']="Kudyiwa kuVariant";i['variantWin']="Kukunda kuVariant";i['viewInFullSize']="Wona mu size ihombe";i['waiting']="Kumirira";i['waitingForOpponent']="Kumirira munhu wekutamba naye";i['white']="Chena";i['whiteDidntMove']="White haana kutamba";i['whiteDrawBlack']="White/Mangange/Black";i['whiteIsVictorious']="Chena yawhina";i['whiteLeftTheGame']="Ichena yabuda mumutambo";i['whitePlays']="Ichena yakutamba";i['whiteResigned']="Ichena yaregera";i['winning']="Inokundisa";i['youNeedAnAccountToDoThat']="Kudiwa Account kuti uite izvozvo";i['youPlayTheBlackPieces']="Tamba nezvitema";i['youPlayTheWhitePieces']="Tamba nezvichena";i['yourOpponentWantsToPlayANewGameWithYou']="Munhu arikuda kutamba newe futi";i['yourTurn']="Nguva yako"})()