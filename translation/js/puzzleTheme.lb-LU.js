"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.puzzleTheme)window.i18n.puzzleTheme={};let i=window.i18n.puzzleTheme;i['advancedPawn']="Virgeréckelte Bauer";i['advancedPawnDescription']="Ee vu denge Baueren ass déif an der géignerescher Stellung virgeréckelt an dreet méiglecherweis ëmzewandelen.";i['advantage']="Virdeel";i['advantageDescription']="Nëtz deng Geleeënheet fir en decisive Virdeel ze kréien. (200cp ≤ eval ≤ 600cp)";i['anastasiaMate']="Anastasia-Matt";i['anastasiaMateDescription']="E Sprénger an en Tuerm oder eng Damm schaffen zesummen fir de géigneresche Kinnek tëschent dem Rand vum Briet an enger vu senge Figuren matt ze setzen.";i['arabianMate']="Arabesche Matt";i['arabianMateDescription']="E Sprénger an en Turm schaffen zesummen fir de géigneresche Kinnek am Eck vum Briet matt ze setzen.";i['attackingF2F7']="Ugrëff op f2 oder f7";i['attackingF2F7Description']="En Ugrëff den sech op d\\'Baueren op f2 oder f7 konzentréiert, wéi z. B. bei der Fegatello-Variant.";i['attraction']="Hinlenkung oder Magnéit";i['attractionDescription']="En Oftausch oder Opfer datt eng géigneresch Figur ob e Feld invitéiert oder forcéiert datt eng Folgetaktik erlaabt.";i['backRankMate']="Grondreiematt";i['backRankMateDescription']="Setz de Kinnek op der Grondrei matt, wann en do vun sengen eegene Figuren ageklemmt ass.";i['bishopEndgame']="Leefer Endspill";i['bishopEndgameDescription']="En Endspill mat nëmmen Leefer a Baueren.";i['bodenMate']="Buedem-Matt";i['bodenMateDescription']="Zwee ugräifend Leefer op sech kräizegen Diagonalen setzen den Kinnek matt, deen duerch seng eege Figuren behënnert ass.";i['capturingDefender']="Schlo de Verteideger";i['capturingDefenderDescription']="D\\'Schloen vun enger Figur, déi fir d\\'Deckung vun enger anerer Figur zoustänneg war, soudass déi elo ongedeckten Figur mam nächsten Zuch kann geschloen ginn.";i['castling']="Rochéieren";i['castlingDescription']="Bréng de Kinnek a Sécherheet an den Tuerm op Ugrëffspositoun.";i['clearance']="Räumung";i['clearanceDescription']="En Zuch, meeschtens mat Tempo, deen de Wee ob e Feld, eng Linn oder eng Diagonal fräi mëscht an eng Folgetaktik erlaabt.";i['crushing']="Vernichtend";i['crushingDescription']="Fann d\\'Gaffe vum Géigner, fir e vernichtenden Virdeel ze erhalen. (eval ≥ 600cp)";i['defensiveMove']="Defensiven Zuch";i['defensiveMoveDescription']="E prezisen Zuch oder eng Sequenz vun Zich, déi gespillt musse ginn fir keen Material oder Virdeel ze verléieren.";i['deflection']="Oflenkung";i['deflectionDescription']="En Zuch, deen eng géigneresch Figur vun enger anerer Aufgab oflenkt, wéi zum Beispill d\\'Deckung vun engem wichtegen Feld. Heiansdo och \\\"Iwwerlaaschtung\\\" genannt.";i['discoveredAttack']="Ofzuchsugrëff";i['discoveredAttackDescription']="Eng Figur, déi den Ugrëff vun enger laangfeldreger Figur (zum Beispill en Tuerm) blockéiert, aus dem Wee beweegen.";i['doubleBishopMate']="Leeferpuermatt";i['doubleBishopMateDescription']="Zwee ugräifend Leefer op niewenteneen leienden Diagonalen setzen den Kinnek matt, deen duerch seng eege Figuren behënnert ass.";i['doubleCheck']="Dubbelschach";i['doubleCheckDescription']="Schach mat zwou Figuren gläichzäiteg ginn als Resultat vun engem Ofzuchsugrëff, wou souwuel déi gespillte Figur ewéi och déi opgedeckte Figur de géigneresche Kinnek ugräifen.";i['dovetailMate']="Cozio-Matt";i['dovetailMateDescription']="Eng Damm setzt e niewestohende Kinnek matt, deem seng eenzeg zwee Fluchtfelder duerch seng eegen Figuren behënnert sinn.";i['endgame']="Endspill";i['endgameDescription']="Eng Taktik an der leschter Phase vun der Partie.";i['enPassantDescription']="Eng Taktik bezüglech der \\\"en passant\\\" Reegel, bei där e Bauer e géigneresche Bauer schloen kann, deen un em mat engem Dubbelschrëtt aus der Ausgangspositioun laanscht gaangen ass.";i['equality']="Egalitéit";i['equalityDescription']="Komm aus enger verluerener Positioun zeréck a sécher dir e Remis oder ausgeglache Positioun. (eval ≤ 200cp)";i['exposedKing']="Exponéiert Kinnek";i['exposedKingDescription']="Eng Taktik bezüglech engem Kinnek, dee vun wéinege Figuren verdeedegt gëtt, wat oft zu Schachmatt féiert.";i['fork']="Forschett";i['forkDescription']="En Zuch, bei deem déi gespillte Figur zwou géigneresch Figuren gläichzäiteg ugräift.";i['hangingPiece']="Hänkend Figur";i['hangingPieceDescription']="Eng Taktik, bei där eng géignerescher Figur net oder ongenügend gedeckt ass an fräi ze schloen ass.";i['healthyMix']="Gesonde Mix";i['healthyMixDescription']="E bësse vun allem. Du weess net wat dech erwaart, dowéinst muss op alles preparéiert sinn! Genau wéi bei echte Partien.";i['hookMate']="Hokenmatt";i['hookMateDescription']="Schachmatt mat engem Tuerm, Sprénger a Bauer zesummen mat engem géigneresche Bauer deen dem géigneresche Kinnek e Fluchtfeld hëlt.";i['interference']="Ënnerbriechung";i['interferenceDescription']="Eng Figur tëschent zwou géigneresch Figuren beweegen, fir eng oder béid géigneresch Figuren onverdeedegt ze loossen, wéi zum Beispill e Sprénger op engem verdeedegte Feld tëschent zwee Tierm.";i['intermezzo']="Zwëschenzuch";i['intermezzoDescription']="Amplaz den erwaardenen Zuch ze spillen, spill als éischt en Zuch deen eng direkt Bedroung poséiert, op deen de Géigner äntweren muss.";i['kingsideAttack']="Ugrëff um Kinneksfligel";i['kingsideAttackDescription']="En Ugrëff op den géigneresche Kinnek, nodeem en op der Kinnekssäit rochéiert huet.";i['knightEndgame']="Sprénger Endspill";i['knightEndgameDescription']="En Endspill nëmmen mat Sprénger a Baueren.";i['long']="Laang Aufgab";i['longDescription']="Dräi Zich fir ze gewannen.";i['master']="Meeschter-Partien";i['masterDescription']="Aufgabe aus Partie vu Spiller mat engem Titel.";i['masterVsMaster']="Partië vu Meeschter géint Meeschter";i['masterVsMasterDescription']="Aufgabe aus Partie tëschent zwee Spiller mat engem Titel.";i['mate']="Schachmatt";i['mateDescription']="Gewann d\\'Partie mat Stil.";i['mateIn1']="Matt an 1";i['mateIn1Description']="Mattsetzen an engem Zuch.";i['mateIn2']="Matt an 2";i['mateIn2Description']="Mattsetzen an zwee Zich.";i['mateIn3']="Matt an 3";i['mateIn3Description']="Mattsetzen an dräi Zich.";i['mateIn4']="Matt a 4";i['mateIn4Description']="Mattsetzen a véier Zich.";i['mateIn5']="Matt a 5 oder méi";i['mateIn5Description']="Fann eng laang Sequenz un Zich, déi schachmatt gëtt.";i['middlegame']="Mëttelspill";i['middlegameDescription']="Eng Taktik an der zweeter Phase vun der Partie.";i['oneMove']="Een-Zuch Aufgab";i['oneMoveDescription']="Eng Aufgab déi nëmmen een Zuch erfuerdert.";i['opening']="Eröffnung";i['openingDescription']="Eng Taktik an der éischter Phase vun der Partie.";i['pawnEndgame']="Baueren Endspill";i['pawnEndgameDescription']="En Endspill mat just Baueren.";i['pin']="Fesselung";i['pinDescription']="Eng Taktik bezüglech Fesselungen, wou eng Figur sech net beweegen kann, ouni en Ugrëff op eng aner méi héichwäerteg Figur ze erlaben.";i['playerGames']="Partie vu Spiller";i['playerGamesDescription']="Sich no Aufgaben, déi aus denge Partien, oder aus de Partie vun anere Spiller generéiert goufen.";i['promotion']="Ëmwandlung";i['promotionDescription']="Wandel e Bauer zu enger Dame oder Liichtfigur ëm.";i['puzzleDownloadInformation']=s("Dës Aufgaben sinn ëffentlech zougänglech an kënnen ënner %s erofgelueden ginn.");i['queenEndgame']="Dammen Endspill";i['queenEndgameDescription']="En Endspill mat just Dammen a Baueren.";i['queenRookEndgame']="Damm an Tuerm";i['queenRookEndgameDescription']="En Endspill nëmmen mat Dammen, Tierm a Baueren.";i['queensideAttack']="Ugrëff um Dammefligel";i['queensideAttackDescription']="En Ugrëff op de géigneresche Kinnek, nodeem en op der Dammesäit rochéiert huet.";i['quietMove']="Rouegen Zuch";i['quietMoveDescription']="En Zuch dee weder e Schach oder Schlagzuch ass oder eng direkt Drohung kréiert, mee eng verstoppte méi grouss Drohung virbereet.";i['rookEndgame']="Tuerm Endspill";i['rookEndgameDescription']="En Endspill nëmmen mat Tierm a Baueren.";i['sacrifice']="Opfer";i['sacrificeDescription']="Eng Taktik wou een kuerzfristeg Material opgëtt fir no enger forcéierter Sequenz laangfristeg e Virdeel ze hunn.";i['short']="Kuerz Aufgab";i['shortDescription']="Zwee Zich fir ze gewannen.";i['skewer']="Spiiss";i['skewerDescription']="E Motiv mat enger wertvoller Figur déi ugegraff gëtt a beim Fortbeweegen erlaabt, dass eng manner wertvoll Figur hannendrunn ugegraff oder geschloe gëtt. Den Inverse vun enger Fesselung.";i['smotheredMate']="Erstéckte Matt";i['smotheredMateDescription']="E Schachmatt duerch e Sprénger deem den Kinnek net entkomme kann, well hien vun sengen eegenen Figuren ëmkreest (erstéckt) gëtt.";i['superGM']="Super-GM-Partien";i['superGMDescription']="Aufgabe vu Partie vun de beschte Spiller vun der Welt.";i['trappedPiece']="Gefaange Figur";i['trappedPieceDescription']="Eng Figur kann dem Schlagzuch net entkommen, well hir Zich begrenzt sinn.";i['underPromotion']="Ënnerwandlung";i['underPromotionDescription']="Ëmwandlung zu engem Sprénger, Leefer oder Tuerm.";i['veryLong']="Ganz laang Aufgab";i['veryLongDescription']="Véier oder méi Zich fir ze gewannen.";i['xRayAttack']="Rëntgen-Ugrëff";i['xRayAttackDescription']="Eng Figur attackéiert oder verdeedegte Feld duerch eng géigneresch Figur.";i['zugzwang']="Zugzwang";i['zugzwangDescription']="De Géigner huet eng begrenzten Unzuel un Zich an all Zuch verschlechtert seng Positioun."})()