"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.puzzleTheme)window.i18n.puzzleTheme={};let i=window.i18n.puzzleTheme;i['advancedPawn']="Frífinna";i['advancedPawnDescription']="Ein finna, ið umskapast ella hóttir við at umskapast, er lykilin til taktikkin.";i['advantage']="Fyrimunur";i['advantageDescription']="Tak av møguleikanum at fáa avgerandi fyrimun. (200cp ≤ eval ≤ 600cp)";i['anastasiaMate']="Mát Anastasiu";i['anastasiaMateDescription']="Riddari og rókur ella frúgv samstarva um at fanga mótstøðukongin millum síðuna á talvborðinum og eitt vinarligt sinnað fólk.";i['arabianMate']="Arábiskt mát";i['arabianMateDescription']="Ein riddari og ein rókur samstarva um at fanga mótstøðukongin í einum av hornunum á talvborðinum.";i['attackingF2F7']="Álop á f2 ella f7";i['attackingF2F7Description']="Eitt álop, ið savnar seg um f2 ella f7-finnuna, eins og í fegatello-álopinum (Fried Liver Attack).";i['attraction']="Atdráttur";i['attractionDescription']="Eitt umbýti ella offur, ið eggjar ella noyðir eitt mótleikarafólk til ein punt, ið síðani letur upp fyri eini taktiskari atgerð.";i['backRankMate']="Mát á aftasta rað";i['backRankMateDescription']="Set kongin skák og mát á aftasta rað, har egnu fólk hansara byrgja hann inni.";i['bishopEndgame']="Bispaendaspæl";i['bishopEndgameDescription']="Endaspæl við bispum og finnum.";i['bodenMate']="Bodensmát";i['bodenMateDescription']="Tveir bispar, ið leypa á á krossandi hornalinjum (diagonalum), seta kongin, ið er forðaður av sínum egna fólki, skák og mát.";i['capturingDefender']="Tak fólkið, ið verjir";i['capturingDefenderDescription']="At beina eitt fólk burtur, ið hevur týdning í verjuni av einum øðrum fólki. Hetta ger tað møguligt at taka fólkið, ið nú er óvart, í einum seinni leiki.";i['castling']="At leypa í borg";i['castlingDescription']="Flyt kongin í tryggleika, og tak rókin í nýtslu, so hann fær lopið á.";i['clearance']="Rudding";i['clearanceDescription']="Ein leikur, ofta við tempo, ið ruddar ein punt, eitt rað ella eina tvørlinju, ið gevur møguleika fyri einari taktiskari atgerð.";i['crushing']="At knúsa";i['crushingDescription']="Finn mistakið hjá mótleikaranum til tess at ogna tær knúsandi fyrimun. (eval ≥ 600cp)";i['defensiveMove']="Verjuleikur";i['defensiveMoveDescription']="Ein ávísur leikur ella ein røð av leikum, ið eru neyðugir, um sleppast skal undan at missa fólk ella annan fyrimun.";i['deflection']="Avbending";i['deflectionDescription']="Ein leikur, ið dregur eitt mótstøðufólk burtur frá at útynna eina aðra uppgávu; eitt nú at ansa eftir einum týdningarmiklum punti.";i['discoveredAttack']="Avdúkað álop";i['discoveredAttackDescription']="At flyta eitt fólk, ið frammanundan forðaði einum fólki í at leypa á; eitt nú at flyta ein riddara, ið stendur framman fyri ein rók.";i['doubleBishopMate']="Tvífalt bispamát";i['doubleBishopMateDescription']="Tveir bispar, ið leypa á á tveimum grannahornalinjum, seta kongin, ið er forðaður av sínum egna fólki, skák og mát.";i['doubleCheck']="Tvískák";i['doubleCheckDescription']="At skáka við tveimum fólkum samstundis. Úrslit av einum ávdúkaraálopi, har bæði fólkið, ið flutti, og fólkið, ið varð avdúkað, leypa á mótstøðukongin.";i['dovetailMate']="Sýlt mát (dúgvuvelamát)";i['dovetailMateDescription']="Ein frúgv stendur beint við mótstøðukongin og setir hann skák og mát, tí at kongsins egnu fólk forða konginum í at flýggja til einastu tveir puntarnar, ið eru tøkir.";i['endgame']="Endatalv";i['endgameDescription']="Taktisk atgerð í seinasta skeiðinum av talvinum.";i['enPassantDescription']="Taktisk atgerð, ið inniber at taka í framlopi, har ein finna kann taka eina mótstøðufinnu, ið er komin at standa undir liðini á henni, aftaná at finnan í fyrsta leiki sínum júst er flutt tveir puntar fram.";i['equality']="Javnstøða";i['equalityDescription']="Kom afturíaftur úr eini tapandi støðu, og tryggja tær remis ella eina javna støðu. (eval ≤ 200cp)";i['exposedKing']="Kongur í andgletti";i['exposedKingDescription']="Taktisk atgerð móti kongi, ið bert hevur fá verndarfólk um seg. Ber ofta skák og mát við sær.";i['fork']="Gaffil";i['forkDescription']="Leikur, har flutta fólkið loypur á tvey mótstøðufólk í senn.";i['hangingPiece']="Hangandi fólk";i['hangingPieceDescription']="Taktisk atgerð móti einum mótstøðufólki, ið ikki er vart ella ikki nóg væl vart, og tí lætt at taka.";i['healthyMix']="Sunt bland";i['healthyMixDescription']="Eitt sindur av øllum. Tú veitst ikki, hvat tú kanst vænta tær, so ver til reiðar til alt! Júst sum í veruligum talvum.";i['hookMate']="Húkamát";i['hookMateDescription']="Skák og mát við róki, riddara og finnu, sum saman við einari fíggindafinnu forða mótstøðukonginum í at sleppa til rýmingar.";i['interference']="Uppílegging";i['interferenceDescription']="Flyt eitt fólk millum tvey mótstøðufólk, so annað mótstøðufólkið stendur óvart ella bæði standa óvard; flyt t.d. ein riddara á ein vardan punt millum tveir rókar.";i['intermezzo']="Millumleikur";i['intermezzoDescription']="Ístaðin fyri at leika tann væntaða leikin, skalt tú leika ein annan leik, ið er ein hóttandi vandi, ið mótleikarin má varða seg ímóti her og nú. Leikurin er eisini kendur sum \\\"Zwischenzug\\\" ella \\\"In between\\\".";i['kingsideAttack']="Álop kongamegin";i['kingsideAttackDescription']="Álop á mótstøðukongin, aftaná at hann er lopin í borg kongamegin.";i['knightEndgame']="Riddaraendatalv";i['knightEndgameDescription']="Endatalv við riddarum og finnum.";i['long']="Long uppgáva";i['longDescription']="Tríggir leikir, so er vunnið.";i['master']="Meistaratalv";i['masterDescription']="Uppgávur úr talvum, ið telvarar við meistaraheitum hava telvað.";i['masterVsMaster']="Meistari móti meistaratalvum";i['masterVsMasterDescription']="Uppgávur úr talvum millum tveir telvarar við meistaraheitum.";i['mate']="Mát";i['mateDescription']="Vinn talvið við stíli.";i['mateIn1']="Mát í einum";i['mateIn1Description']="Set skák og mát í einum leiki.";i['mateIn2']="Mát í tveimum";i['mateIn2Description']="Set skák og mát í tveimum leikum.";i['mateIn3']="Mát í trimum";i['mateIn3Description']="Set skák og mát í trimum leikum.";i['mateIn4']="Mát í fýra";i['mateIn4Description']="Set skák og mát í fýra leikum.";i['mateIn5']="Mát í fimm ella fleiri";i['mateIn5Description']="Finn útav eini langari mátraðfylgju.";i['middlegame']="Miðtalv";i['middlegameDescription']="Taktisk atgerð í seinna skeiði av talvinum.";i['oneMove']="Uppgáva við einum leiki";i['oneMoveDescription']="Uppgáva, ið bert krevur ein leik.";i['opening']="Byrjanartalv";i['openingDescription']="Taktisk atgerð í fyrsta skeiðinum av talvinum.";i['pawnEndgame']="Finnuendatalv";i['pawnEndgameDescription']="Endatalv við finnum burturav.";i['pin']="Binding";i['pinDescription']="Taktisk atgerð við bindingum, har eitt fólk ikki er ført fyri at flyta uttan at lata upp fyri álopi á eitt fólk við hægri virði.";i['promotion']="Umskapan";i['promotionDescription']="Ein finna, ið umskapast ella hóttir við at umskapast, er lykilin til taktikkin.";i['queenEndgame']="Frúgvaendatalv";i['queenEndgameDescription']="Endatalv við frúgvum og finnum burturav.";i['queenRookEndgame']="Frúgv og rókur";i['queenRookEndgameDescription']="Endatalv við frúm, rókum og finnum.";i['queensideAttack']="Álop frúgvamegin";i['queensideAttackDescription']="Álop á mótstøðukongin, aftaná at hann er lopin í borg frúgvamegin.";i['quietMove']="Stillførur leikur";i['quietMoveDescription']="Leikur, ið hvørki skákar ella tekur, men slóðar fyri eini hóttan, ið ikki slepst undan, í einum seinni leiki.";i['rookEndgame']="Rókaendatalv";i['rookEndgameDescription']="Endatalv við rókum og finnum.";i['sacrifice']="Offur";i['sacrificeDescription']="Taktisk atgerð, ið inniber at geva fólk burtur, við tí fyri eyga at vinna sær ein fyrimun seinni aftaná eina røð av tvungnum leikum.";i['short']="Stutt uppgáva";i['shortDescription']="Tveir leikir, so er vunnið.";i['skewer']="Spjót";i['skewerDescription']="Ein hugsan, ið inniber, at eitt fólk við høgum virði, ið verður álopið, flytur burtur, soleiðis at eitt fólk við lægri virði kann verða tikið ella vera fyri álopi. Tað øvuta av eini binding.";i['smotheredMate']="Kvalt mát";i['smotheredMateDescription']="Skák og mát, framt av einum riddara, har mátaði kongurin ikki er førur fyri at flyta, tí at hann er umgyrdur (ella kvaldur) av egnum fólki.";i['superGM']="Superstórmeistaratalv";i['superGMDescription']="Uppgávur úr talvum, ið heimsins bestu telvarar hava telvað.";i['trappedPiece']="Innibyrgt fólk";i['trappedPieceDescription']="Eitt fólk er ikki ført fyri at sleppa sær undan at verða tikið, tí tað hevur avmarkaðar leikmøguleikar.";i['underPromotion']="Undirumskapan";i['underPromotionDescription']="Umskapan til riddara, bisp ella rók.";i['veryLong']="Sera long uppgáva";i['veryLongDescription']="Fýra leikir ella fleiri til tess at vinna.";i['xRayAttack']="Geisling";i['xRayAttackDescription']="Eitt fólk loypur á ella verjir ein punt gjøgnum eitt mótstøðufólk.";i['zugzwang']="Leiktvingsil";i['zugzwangDescription']="Mótleikarin hevur avmarkaðar møguleikar at flyta, og allir leikir gera støðu hansara verri."})()