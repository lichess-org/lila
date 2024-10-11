"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.puzzleTheme)window.i18n.puzzleTheme={};let i=window.i18n.puzzleTheme;i['advancedPawn']="Vorgruckte Puur";i['advancedPawnDescription']="En Pur isch tüf i di gägnerisch Schtellig vorgruckt und droht sich umzuwandle.";i['advantage']="Vorteil";i['advantageDescription']="Nutz dini Chance, en entscheidende Vorteil z\\'erlange. (200 Hundertstelpure ≤ Bewertung ≤ 600 Hundertstelpure)";i['anastasiaMate']="Anastasia\\'s Matt";i['anastasiaMateDescription']="En Schpringer und en Turm oder e Dame schaffed zäme, um de König, zwüschet em Brättrand und sine eigene Figure, z\\'verwütsche.";i['arabianMate']="Arabischs Matt";i['arabianMateDescription']="En Schpringer und en Turm schaffed zäme, um de König im Egge z\\'fange.";i['attackingF2F7']="Agriff uf f2 oder f7";i['attackingF2F7Description']="En Agriff, wo sich uf d\\'Pure uf f2 oder f7 konzentriert.";i['attraction']="Aziehigschraft";i['attractionDescription']="En Tusch oder es Opfer, wo e gägnerischi Figur uf es Fäld länkt oder zwingt, was dänn e Folgetaktik ermöglicht.";i['backRankMate']="Grundreihe Matt";i['backRankMateDescription']="De König uf de Grundreihe matt setze, wänn er dur sini eigene Figure blockiert isch.";i['bishopEndgame']="Läufer Ändschpil";i['bishopEndgameDescription']="Es Ändschpil, nur mit Läufer und Pure.";i['bodenMate']="Boden Matt";i['bodenMateDescription']="Zwei Läufer, uf sich chrüzende Diagonale, setzed en König matt, wo dur eigeni Figure behinderet wird.";i['capturingDefender']="Schlag de Verteidiger";i['capturingDefenderDescription']="Schlag e Figur, wo e Anderi entscheidend deckt, dass die Ungschützti im nächschte Zug gschlage werde chann.";i['castling']="Rochade";i['castlingDescription']="Bring de König in Sicherheit und schick de Turm in Agriff.";i['clearance']="Öffnig";i['clearanceDescription']="En Zug - oft mit Tämpo - wo es Fäld, e Linie oder e Diagonale für e folgendi, taktischi Idee frei macht.";i['crushing']="Vernichtend";i['crushingDescription']="Find de gägnerisch Patzer und chumm zume vernichtende Vorteil. (Bewertig ≥ 600 Hundertschtel-Pure)";i['defensiveMove']="Verteidigungszug";i['defensiveMoveDescription']="En gnaue Zug oder e Zugfolg, wo nötig isch, um kei Material oder Vorteil z\\'verlüre.";i['deflection']="Ablänkig";i['deflectionDescription']="En Zug, wo e gägnerischi Figur devo ablänkt e Figur oder es wichtigs Fäld z\\'schütze. Wird au als \\\"Überlaschtig\\\" bezeichnet.";i['discoveredAttack']="Abzugsagriff";i['discoveredAttackDescription']="Me nimmt (z. B. en Schpringer) wo vorher en Agriff dur e anderi, wit weg stehendi, Figur (z. B. en Turm) blockiert hät, us em Wäg.";i['doubleBishopMate']="Läuferpaar Matt";i['doubleBishopMateDescription']="Zwei Läufer, uf näbenand ligende Diagonale, setzed en König matt, wo dur eigeni Figure behinderet wird.";i['doubleCheck']="Doppelschach";i['doubleCheckDescription']="Abzug mit doppletem Schachgebot, wobi die vorher verdeckti Figur- und die Abzogeni, de König glichzitig agrifed.";i['dovetailMate']="Schwalbeschwanz Matt";i['dovetailMateDescription']="Mit de Dame - diräkt bim König - matt setze, wobi sini Fluchtfälder dur eigeni Figure verschtellt sind.";i['endgame']="Ändschpil";i['endgameDescription']="E Taktik für die letscht Fase vum Schpiel.";i['enPassantDescription']="E Taktik wo \\\"En-Passant\\\" beinhaltet - e Regle wo en Pur cha en gägnerische Pur schlaa, wänn de ihn mit em \\\"Zwei-Fälder-Zug\\\" übergange hät.";i['equality']="Usglich";i['equalityDescription']="Befrei dich us verlorener Schtellig und sicher dir es Remis oder en usglicheni Schtellig. (Bewertig ≤ 200 Hundertstelpure)";i['exposedKing']="Exponierte König";i['exposedKingDescription']="E Taktik wo de König nu vu wenige Figure verteidigt wird und oft zu Schachmatt fühert.";i['fork']="Gable";i['forkDescription']="En Zug wobi die zogeni Figur glichzitig 2 gägnerischi Figure agrift.";i['hangingPiece']="Hängendi Figur";i['hangingPieceDescription']="E Taktik wo e gägnerischi Figur zwenig oder gar nöd deckt isch und drum, mit Vorteil, gschlage werde cha.";i['healthyMix']="En gsunde Mix";i['healthyMixDescription']="Es bitzli vu Allem, me weiss nöd was eim erwartet, drum isch mer uf alles g\\'fasst - genau wie bi richtige Schachschpiel.";i['hookMate']="Hake Matt";i['hookMateDescription']="Schachmatt mit Turm, Schpringer und Pur und eim gägnerische Pur, wo em König d\\'Flucht verschperrt.";i['interference']="Störig";i['interferenceDescription']="E Figur zwüsche 2 gägnerischi Figure stelle, um einere oder beide de Schutz znäh - z. B. en Schpringer uf es verteidigts Fäld, zwüsche 2 Türm, stelle.";i['intermezzo']="Zwüschezug";i['intermezzoDescription']="Anstatt de erwarteti Zug, zerscht en Andere mache, wo diräkt droht, so dass de Gägner muess reagiere. Isch au bekannt als \\\"Zwüschezug\\\".";i['kingsideAttack']="Agriff am Königsflügel";i['kingsideAttackDescription']="En Agriff uf de gägnerisch König, nachdem er am Königsflügel d\\'Rochade gmacht hät.";i['knightEndgame']="Schpringer Ändschpil";i['knightEndgameDescription']="Es Ändschpiel, nur mit Schpringer und Pure.";i['long']="Mehrzügigi Ufgab";i['longDescription']="3 Züg zum Sieg.";i['master']="Meischter Schpiel";i['masterDescription']="Ufgabe us Schpiel vu Schpiller mit Titel.";i['masterVsMaster']="Meischter gäge Meischter Schpiel";i['masterVsMasterDescription']="Ufgabe us Schpiel vu 2 Schpiller mit Titel.";i['mate']="Schachmatt";i['mateDescription']="Günn das Schpiel mit Schtil.";i['mateIn1']="Matt in 1";i['mateIn1Description']="Schachmatt mit 1 Zug.";i['mateIn2']="Matt in 2";i['mateIn2Description']="Schachmatt mit 2 Züg.";i['mateIn3']="Matt in 3";i['mateIn3Description']="Schachmatt mit 3 Züg.";i['mateIn4']="Matt in 4";i['mateIn4Description']="Schachmatt mit 4 Züg.";i['mateIn5']="Matt in 5 oder meh";i['mateIn5Description']="Find e langi Mattfüherig.";i['middlegame']="Mittelschpiel";i['middlegameDescription']="E Taktik für die zweit Fase vum Schpiel.";i['oneMove']="1-zügigi Ufgab";i['oneMoveDescription']="E Ufgab, mit nur 1 Zug.";i['opening']="Eröffnig";i['openingDescription']="E Taktik für die erscht Fase vum Schpiel.";i['pawnEndgame']="Pure Ändschpiel";i['pawnEndgameDescription']="Es Ändschpiel nur mit Pure.";i['pin']="Fesslig";i['pinDescription']="E Taktik mit Fesslig, wo sich e Figur nöd bewege cha, ohni de Angriff uf e stärcheri Figur z\\'verrate.";i['playerGames']="Schpiller-Schpiel";i['playerGamesDescription']="Suech nach Ufgabe us dine Schpiel oder Ufgabe us Schpiel vu Andere.";i['promotion']="Umwandlig";i['promotionDescription']="En Pur zur Dame oder andere Figur umwandle.";i['puzzleDownloadInformation']=s("Die Ufgabe sind öffentlich, mer channs abelade under %s.");i['queenEndgame']="Dame Ändschpiel";i['queenEndgameDescription']="Es Ändschpiel nur mit Dame und Pure.";i['queenRookEndgame']="Dame und Turm";i['queenRookEndgameDescription']="Es Ändschpiel nur mit Dame Türm und Pure.";i['queensideAttack']="Agriff am Dameflügel";i['queensideAttackDescription']="En Agriff uf de gägnerisch König, nachdem er am Dameflügel d\\'Rochade gmacht hät.";i['quietMove']="Schtille Zug";i['quietMoveDescription']="En Zug wo kei \\\"Schach\\\" bütet, wo nüt schlaht und au nöd droht öppis z\\'schlah, wo aber - verschteckt - e unvermeidlichi Drohig dur en schpöter folgende Zug vorbereitet.";i['rookEndgame']="Turm Ändschpiel";i['rookEndgameDescription']="Es Ändschpiel nur mit Türm und Pure.";i['sacrifice']="Opfer";i['sacrificeDescription']="E Taktik, wo churzfrischtig Material gopferet wird, um dänn mit erzwungener Zugfolg wieder en Vorteil z\\'günne.";i['short']="Churzi Ufgab";i['shortDescription']="2 Züg zum Sieg.";i['skewer']="Schpiess (Hinderschtellig)";i['skewerDescription']="Bim \\\"Schpiess\\\" wird e Figur, wo vor ere Andere staht - oft wird sie au dezue zwunge, sich vor die Ander z\\'schtelle - agriffe, so dass sie us em Wäg muess und ermöglicht, die Hinder z\\'schlah. Quasi umgekehrti Fesslig.";i['smotheredMate']="Erschtickigs Matt";i['smotheredMateDescription']="Es Schachmatt mit em Springer, wo sich de König nöd bewege cha, will er vu sine eigene Figuren umstellt-, also vollkomme igschlosse, wird.";i['superGM']="Super-Grossmeischter-Schpiel";i['superGMDescription']="Ufgabe us Schpiel, vu de beschte Schpiller uf de Wält.";i['trappedPiece']="G\\'fangeni Figur";i['trappedPieceDescription']="E Figur cha em Schlah nöd entgah, will sie nur begränzt Züg mache cha.";i['underPromotion']="Underverwandlig";i['underPromotionDescription']="Umwandlig in \\\"nur\\\" en Schpringer, Läufer oder Turm.";i['veryLong']="Sehr langi Ufgab";i['veryLongDescription']="4 oder meh Züg bis zum Sieg.";i['xRayAttack']="Röntge Agriff";i['xRayAttackDescription']="E Figur attackiert oder verteidigt es Fäld dur e gägnerischi Figur.";i['zugzwang']="Zugszwang";i['zugzwangDescription']="De Gägner hät nur e limitierti Azahl Züg und Jede verschlächteret sini Schtellig."})()