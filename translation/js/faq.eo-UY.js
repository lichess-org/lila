"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.faq)window.i18n.faq={};let i=window.i18n.faq;i['accounts']="Kontoj";i['acplExplanation']="La centonsoldato estas la mezurunuo uzata en ŝako kiel reprezento de la avantaĝo. Unu centonsoldato egalas 1/100-onon de soldato. Do, 100 centonsoldatoj = 1 soldato. Tiuj valoroj ludas ne formalan rolon en la ŝakludo, sed estas utilaj al ludantoj, kaj estas nepraj en komputila ŝako por aprezi poziciojn.\n\nLa supra komputila movo perdos nul da centonsoldatoj, sed malpli bonaj movoj malhelpus ĝian pozicion, mezurata per centonsoldatoj.\n\nTiu valoro povas esti uzata kiel indikilo de la kvalito de ludo. Ju malpli da centonsoldatoj oni perdas move, des pli forta la ludo.\n\nLa komputila analizo ĉe Lichess estas funkciigita per stockfish.";i['adviceOnMitigatingAddiction']=s("Ni regule ricevas mesaĝojn de uzantoj, kiuj petas nin por helpi ilin halti troludi.\n\nKiam Lichess ne forigas aŭ forbaras ludantojn krom ĉar malobservos de uzkondiĉoj, ni rekomendi uzon de eksteraj iloj por limi troludado. Kelkaj kutimaj proponoj por retblokiloj estas %1$s, %2$s, and %3$s. Se vi volas daŭri uzi la la retpaĝo, sed ne tentiĝas per rapidaj temporegardoj, tiam vi povus esti interestata pri %4$s, tie estas unu kun %5$s.\n\nKelkaj ludantas povus senti, kiel ties ludanta konduto fariĝas manio. Fakte, la MOS klasifikis ludmanio kiel %6$s, kies esencaj trajtoj estas 1) difekta regado de ludado, 2) pliigado de prioritato donita al ludado, kaj 3) kreskado de ludado, malĝraŭ malbonaj konsekvencoj. Se vi pensas, ke via ŝakludanta konduto sekvas ĉi tiu ŝablono, tiam ni kuraĝigi vin por paroli kun amiko aŭ familmembro kaj/aŭ profesiulo.");i['aHourlyBulletTournament']="hore kugla turniro";i['areThereWebsitesBasedOnLichess']="Ĉu estas retejoj bazitaj sur Lichess?";i['asWellAsManyNMtitles']="multaj naciaj estraj titoloj";i['basedOnGameDuration']=s("Lichess tempokontroloj baziĝas sur laŭtaksa daŭro de ludo = %1$s\nEkzemple, la laŭtaksa daŭro de ludo 5+3 estas 5 × 60 + 40 × 3 = 420 sekundoj.");i['beingAPatron']="esti patrono";i['beInTopTen']="esti en la supraj 10 en ĉi tiu rango.";i['breakdownOfOurCosts']="detaligo de niaj elspezoj";i['canIbecomeLM']="Ĉu mi povas atingi la Lichess Master (LM) titolon?";i['canIChangeMyUsername']="Ĉu mi povas ŝanĝi mian uzantnomon?";i['configure']="agordi";i['connexionLostCanIGetMyRatingBack']="Mi perdis ludon pro malfruo/malkonekto. Ĉu mi povas ricevi la perditajn rangpoentojn?";i['desktop']="labortablo";i['discoveringEnPassant']="Kial soldato povas kapti alian soldaton dum ĝi estas jam pasita? (preterpasa preno)";i['displayPreferences']="aperigi preferojn";i['durationFormula']="(horloĝa komenca horo) + 40 × (horloĝa pliigo)";i['eightVariants']="8 ŝakvariantojn";i['enableAutoplayForSoundsA']="Plej multaj retumiloj povas malhelpi sonon ludi sur ĵus ŝargita paĝo por protekti uzantojn. Imagu se ĉiu retejo povas subite bombardi vin kun aŭdiaj varbiloj.\n\nLa ruĝa silentiga ikono aperas kiam via retumilo malhelpis lichess.org ludi sonon. Ĝenerale tiu limigo foriĝas kiam vi alklaki ion. Sur kelkaj poŝtelefonaj retumiloj, tuŝŝovoj ne estas tipo de alklako. En tiu okazo vi devas tuŝeti la tabulon por permesi sonon ĉe la komenco de la ludo.\n\nNi montras la ruĝan ikonon por averti vin kiam tio okazas. Ofte vi povas eksplice permesi lichess.org ludi sonojn. Ĉi tie estas instrukcio por fari tio sur lastaj versioj de kelkaj popularaj retumiloj.";i['enableAutoplayForSoundsChrome']="1. Iru al lichess.org\n2. Alklaku la ŝlosilan ikonon en la adresbreto\n3. Alklaku Retejajn Agordojn\n4. Permesu Sonon";i['enableAutoplayForSoundsFirefox']="1. Iru al lichess.org\n2. Premu str-i sur Linukso/Vindozo aŭ kmd-i sur MakOS\n3. Alklaku la langeton de Permesoj\n4. Permesu Aŭdion kaj Video sur lichess.org";i['enableAutoplayForSoundsMicrosoftEdge']="1. Alklaku la tri punktojn en la supra dekstra angulo\n2. Alklaku Agordojn\n3. Alklaku Kuketojn kaj Retejan Permesojn\n4. Rulumu malsupre kaj alklaku aŭdvidaĵan aŭtomatan ludigon\n5. Aldonu lichess.org al Permesata";i['enableAutoplayForSoundsQ']="Ebligi aŭtomatan ludaton por sonoj?";i['enableAutoplayForSoundsSafari']="1. Iru al lichess.org\n2. Alklaku Safari-n en la menubreto\n3. Alklaku Agordojn por lichess.org ...\n4. Permesu Ĉion Aŭtomatan Ludigon";i['enableDisableNotificationPopUps']="Ŝalti aǔ malŝalti komentojn en ŝprucfenestroj?";i['enableZenMode']=s("Ŝaltu amikan reĝimon en %1$s, aŭ per premo de %2$s dum ludo.");i['explainingEnPassant']=s("Tio estas laŭleĝa movo nomata «preterpasa preno». La Vikipedia paĝo donas %1$s.\n\nĜin priskribas la sekcioj 3.7.3.1 kaj 3.7.3.2 el la %2$s:\n\n«Soldato, kiu okupas kvadraton sur la sama vico kiel kontraŭa soldato, kiu ĵus saltis je du kvadratoj dum unu movo de sia origina pozicio povas kaptiĝi kvazaŭ ĝi movis sole je unu kvadrato. Ĉi tiu kapto estas nur laŭleĝa tuj post tiu dukvadrata movo, kaj nomiĝas ‹preterpasa preno›, alinome ‹en passant›.»\n\nVidu la %3$s pri ĉi tiu movo por ekzerci je ĝi.");i['fairPlay']="Honesta ludo";i['fairPlayPage']="paĝon de honesta ludo";i['faqAbbreviation']="Plej Oftaj Demandoj";i['fewerLobbyPools']="malpli da vestiblaj grupoj";i['fideHandbook']="FIDE manlibro";i['fideHandbookX']=s("FIDE-a manlibro %s");i['findMoreAndSeeHowHelp']=s("Vi povas ekscii pli pri %1$s (inkluzive de %2$s). Se vi volas helpi Lichess volontulante vian tempon kaj kapablojn, ekzistas multaj %3$s.");i['frequentlyAskedQuestions']="Plej Oftaj Demandoj";i['gameplay']="Ludmaniero";i['goldenZeeExplanation']="ZugAddict elsendis kaj por la antaŭa du horojn li provis venki kontraŭ la A.I. nivelo 8 en 1+0 ludo, sensukcese. Thibault al li diras, ke se li sukcese faras ĝin sur la elsendo, tiam li ricevos unikan trofeon. Unu horon post, li disbatis Stockfish, kaj la promeso estis honorita.";i['goodIntroduction']="bona enkonduko";i['guidelines']="gvidlinioj";i['havePlayedARatedGameAtLeastOneWeekAgo']="ludis rangan ludon ene la lasta semajno por ĉi tiu rango,";i['havePlayedMoreThanThirtyGamesInThatRating']="ludis pli ol 30 rangaj ludoj ĉe donita rango,";i['hearItPronouncedBySpecialist']="Aŭskultu ĝin prononcita de fakulo.";i['howBulletBlitzEtcDecided']="Kial estas Kugla, Blitza, kaj alia tempregado decidis?";i['howCanIBecomeModerator']="Kiel mi fariĝis moderatoro?";i['howCanIContributeToLichess']="Kiel mi povas kontribui kun Lichess?";i['howDoLeaderoardsWork']="Kiel rangoj kaj rangtabuloj funkcias?";i['howToHideRatingWhilePlaying']="Kiel kaŝi mian rangon dum la ludoj?";i['howToThreeDots']="Kiel...";i['inferiorThanXsEqualYtimeControl']=s("< %1$ss = %2$s");i['inOrderToAppearsYouMust']=s("Por alveni al %1$s vi devus:");i['insufficientMaterial']="Perdi pro tempo, egaleco kaj nesufiĉa materialo";i['isCorrespondenceDifferent']="Ĉu koresponda ŝako diferencas de normala ŝako?";i['keyboardShortcuts']="Kiuj flumklavoj estas tie?";i['keyboardShortcutsExplanation']="Kelkaj Lichess paĝoj havas flumklavojn, ke vi povas uzi. Provi premu la \\\"?\\\" klavon ĉe studa, analiza, puzla, aŭ luda paĝo por listo de disponeblaj flumklavoj.";i['leavingGameWithoutResigningExplanation']="Se via kontraŭulo ofte abortas/eliras ludojn, tiam li estos \\\"luda forigita\\\" kio signifas, ke li estas portempe forigita de ludo. Tio ne estas publike montrata ĉe ties profilo. Se tia konduto daŭras, la longo de luda forigo pliiĝas - kaj daŭrigita konduto de tia naturo eble alkondukis onin al fermato de ties konto.";i['leechess']="li-ĉes";i['lichessCanOptionnalySendPopUps']="Lichess povas laŭvole sendi ŝprucfenestrajn sciigojn, ekzemple kiam estas via vico aŭ kiam vi ricevante privatan mesaĝon.\n\nAlklaku la ŝlosilan ikonon apud la lichess.org retadreso en la URL breto de via retumilo.\n\nTiam elektu ĉu permesi aŭ bloki sciigoj de Lichess.";i['lichessCombinationLiveLightLibrePronounced']=s("Lichess estas kombino de realtempe/malpeza/libera (angle: live/light/libre) kaj ŝako (angle: chess). Ĝi estas prononcita %1$s.");i['lichessFollowFIDErules']=s("Se okazas, ke timo de ludanto finiĝas, tiu ludanto ĝenerale malvenkos la ludon. Tamen, la ludo egalvenkiĝas se la pozicio estas do, ke la kontraŭulo ne povas ŝakmati la reĝon de la ludanto per ĉiu ebla serio de leĝaj movoj (%1$s).\n\nEn maloftaj kazoj tiu aŭtomata decido estas malfacila (devigitaj vicoj, fortresoj). Defaŭlte ni ĉiam subteni la ludanto, kiu ne elĉerpas sian tempon.\n\nNotu, ke eblas ŝakmati kun unu ĉevalo aŭ kuriero se la kontraŭulo havas ŝakpeco, kiu povus bloki la regon.");i['lichessPoweredByDonationsAndVolunteers']="Lichess ekzistas danke al donacoj de patronoj kaj klopodoj de volontula teamo.";i['lichessRatings']="Lichess rangoj";i['lichessRecognizeAllOTBtitles']=s("Lichess agnoskas ĉiuj FIDE titolojn gajnis el OTB (fizike kun aliulo) luda, kaj ankaŭ %1$s. Ĉi tie estas listo de FIDE titoloj:");i['lichessSupportChessAnd']=s("Lichess subtenas norman ŝakon kaj %1$s.");i['lichessTraining']="trejniĝo en Lichess";i['lichessUserstyles']="Lichess uzantstiloj";i['lMtitleComesToYouDoNotRequestIt']="Ĉi tiu honora titolo estas neoficiala kaj nur ekzistas ĉe Lichess.\n\nNi rare aljuĝas ĝin al alte noteblaj ludantoj, kiuj estas bonaj civitanoj de Lichess, laŭ nia bontrovo. Vi ne ricevas la LM titolon, la LM titolo akiras vin. Se vi kvalifikus, vi ricevos mesaĝon de ni pri ĝi kaj la elekteblon akcepti aŭ malakcepti.\n\nNe petu por la LM titolo.";i['mentalHealthCondition']="memstara mensmalsano";i['notPlayedEnoughRatedGamesAgainstX']=s("La ludanto ne finiĝis sufiĉe da rangaj ludoj kontraŭ %1$s en la rangkategorio.");i['notPlayedRecently']="La ludanto ne ludis sufiĉe da lastatempaj ludoj. Depende de la nombro da ludoj, kiujn vi ludis, eble daŭros ĉirkaŭ unu jaro da senaktiveco, por ke via rango reprovizoriĝas.";i['notRepeatedMoves']="Ni ne ripetis movojn. Kial tamen la ludo estis egalvenko pro ripetado?";i['noUpperCaseDot']="Ne.";i['otherWaysToHelp']="aliaj formoj por helpi";i['ownerUniqueTrophies']=s("Tiu trofeo estas unika en la historio de Lichess, neniu krom %1$s iam havos ĝin.");i['pleaseReadFairPlayPage']=s("Por pli informo, bonvole legu nian %s");i['positions']="pozicioj";i['preventLeavingGameWithoutResigning']="Kio estas fari pri ludantoj forlasantaj ludojn sen rezigni?";i['provisionalRatingExplanation']="La demandosigno signifis, ke la rango estas provizora. Kialoj ampleksas:";i['ratingDeviationLowerThanXinChessYinVariants']=s("posedas rangan devion sub %1$s, en norma ŝako, kaj malpli ol %2$s en variantoj,");i['ratingDeviationMorethanOneHundredTen']="Konkrete, tio signifas, ke la Glicko-2 devio estas pli alta ol 110. La devio estas la nivelo de kredo, kiu la sistemo havas en la taksado. Ju pli malalta la devio, des pli stabila estas la taksado.";i['ratingLeaderboards']="ĉefo tabulo de rango";i['ratingRefundExplanation']="Minuton post la momento kiam la ludanto estas markita kiel trompa, la poentoj de la lasta lastaj 40 rangaj ludoj, dum la lastaj tri tagoj, estos demetintaj. Si iu alia perdis poentojn en tiuj ludoj (pro malvenko aŭ egalvenko), kaj ties rango ne estis provizora, tiu rehavos poentojn. La kvanto da poentoj ricevotaj estas limigita de via plej alta rango kaj de la progreso de via rango post tiu ludo. (Ezkemple, se via rango multe supreniris post tiu ludo, vi povus rehavi neniun poenton aŭ parton de tio, kion vi estus devinta ricevi. Rehavo estos neniam pli ol 150 poentoj).";i['ratingSystemUsedByLichess']="Atingoj estas kalkulita per la Glicko-2 atinga metodo ellaborita de Mark Glickman. Tiu estas tre populara atinga metodo, kaj estas uzata de signifa nombro de ŝakorganazaĵoj. (FIDE estas notinda kontraŭekzamplo, ĉar ili uzas la neĝisdatan Elo atingan sistemon).\n\nFundamente, Glicko atingoj uzi \\\"kredajn intervalojn\\\" kiam kalkulas kaj montras vian atingon. Kiam vi unue komencas uzi la retejon, via atingo komencas ĉe 1500 ± 1000. La 1500 reprezentas vian atingon kaj la 1000 reprezentas la kredan intervalon.\n\nBaze, la sistemo estas 95% certa, ke vian atingo estas ie inter 500 kaj 2500. Ĝi estas malcertega. Ĉar tio, kiam ludanto komencas, ties atingo ŝanĝas tre rapide, eble kelkcent poentoj samtempe. Tamen, post kelkaj gamoj kontraŭ establitaj ludantoj la kreda intervalo maldikiĝas, kaj la nombro de poentoj gajnas/perdas post ĉiu gamo malpliiĝas.\n\nAlia afero notinda estas ke kiam tempo pasas, la kreda intervalo pliiĝas. Tiu permesis vin gajni/perdi poentoj pli rapide por kongrui ĉio ŝanĝoj en vian lertnivelo dum tiam.";i['repeatedPositionsThatMatters']=s("Trifoja ripetado estas pro ripetado de %1$s, ne movoj. Ripetado ne devas okazi sekve.");i['secondRequirementToStopOldPlayersTrustingLeaderboards']="La dua postulo estas tiel, ke ludantoj kiuj ne plu uzas iliajn kontojn ĉesi plenigi rangtabulojn.";i['showYourTitle']=s("Se vi havas OTB titolon, vi povas peti por montranta ĝin ĉe via konto per kompleti la %1$s, inkluzive klaran bildon de identiga dokumento/carto kaj memfoton de vi tenante la dokumenton/karton.\n\nKontroli kiel titolita ludanton ĉe Lichess donas atingo por ludi en la Titolataj Arenaj eventoj.\n\nFine, tiu estas honora %2$s titolo.");i['similarOpponents']="kontraŭuloj kun simila forteco";i['stopMyselfFromPlaying']="Ĉu haltigi min mem el ludo?";i['superiorThanXsEqualYtimeControl']=s("≥ %1$ss = %2$s");i['threeFoldHasToBeClaimed']=s("Ripetado necesas esti reklamaciita de unu de la ludantoj. Vi povas fari tion premante la butonon ke estas montrita aŭ oferante egalvenkon antaŭ vian finan ripetan movon, ne gravos se la kontraŭbatalanto malakceptas la egalvenkan oferon, ĝi estos akceptita ĉiel. Vi povas ankaŭ %1$s Lichess por aŭtomate reklamacii ripetadojn por vi. Aldone, kvinfoja ripetado ĉiam tuje finiĝos la ludon.");i['threefoldRepetition']="Triobla ripeto";i['threefoldRepetitionExplanation']=s("Se pozicio okazas 3 fojoj, ludantoj povas reklamacii kiel egalvenko por %1$s. Lichess efektivigas la oficialajn FIDE regulojn, kiel priskribiĝas en Artikolo 9.2 de la %2$s.");i['threefoldRepetitionLowerCase']="triobla ripeto";i['titlesAvailableOnLichess']="Kiuj titoloj ekzistas ĉe Lichess?";i['uniqueTrophies']="Unikaj trofeoj";i['usernamesCannotBeChanged']="Ne uzantnomoj ne povas esti ŝanĝita por teknikaj kaj praktikaj kialoj. Uzantnomoj estas materialaj en tro da lokoj: datumbazoj, eksportoj, protokoloj, kaj mensoj de homoj. Vi povas unue ĝustigi la majuskligo.";i['usernamesNotOffensive']=s("Ĝenerale, uzantnomoj ne povus esti: ofenda, imitanta de aliulo, aŭ varbanta. Vi povas legi plu pri la %1$s.");i['verificationForm']="kontrola formularo";i['viewSiteInformationPopUp']="Vidi komentan ŝprucfenestron de la retejo";i['watchIMRosenCheckmate']=s("Spektu la internacian mastron Eric Rosen ŝakmati %s.");i['wayOfBerserkExplanation']=s("Por akiri ĝin, hiimgosu defiis sin berserki kaj venki ciujn ludojn de %s.");i['weCannotDoThatEvenIfItIsServerSideButThatsRare']="Bedaŭrinde, ni ne povas realjuĝas rangajn poentojn por ludoj malvenkis per prokrasto aŭ malkonekto, sendepende se la problemo estis ĉe via aŭ nia fino. La lasta tamen, estas tre malofta. Ankaŭ notu, ke kiam Lichess restarigis kaj vi malvenki per tempo pro tio, ni halti la ludo por preventi maljustan malvenkon.";i['weRepeatedthreeTimesPosButNoDraw']="Ni ripetis pozicion trifoje. Kial la ludo ne estis egalvenko?";i['whatIsACPL']="Kio estas la Averaĝa ĉentonpeona perdo (ACPP/ACPL)?";i['whatIsProvisionalRating']="Kial demandsigno (?) staras apud rango?";i['whatUsernameCanIchoose']="Kio povas esti mia uzantnomo?";i['whatVariantsCanIplay']="Kiujn ŝakvariantojn mi povas ludi en Lichess?";i['whenAmIEligibleRatinRefund']="Kiam mi memage reakiros perditajn poentojn pro trompantoj?";i['whichRatingSystemUsedByLichess']="Kiun rangan sistemon Lichess uzas?";i['whyAreRatingHigher']="Kial estas rangoj pli alta ol aliaj retejoj kaj organizaĵoj kiel FIDE, USCF, kaj la ICC?";i['whyAreRatingHigherExplanation']="Estas plej bona se oni ne pensas pri atingoj kiel absolutaj nombroj, aŭ kompari ilin kontraŭ aliaj organizaĵoj. Malsamaj organizaĵoj havas malsimilajn nivelojn de ludantoj, malsamaj atingaj sistemoj (Elo, Glicko, Glicko-2, aŭ modifita versio de la antaŭskribita). Tiuj faktoroj povas severe efiki la absolutajn nobrojn (atingoj).\n\nEstas plej bona se oni pensas pri atingoj kiel relativaj nombroj (anstataŭ absolutaj nombroj): En ludantaro, ties relativaj difrencoj en atingoj helpos vin estimi kiu estos venki/egalvenki/malvenki, kaj kiom ofte. Dirante \\\"mi havas X atingon\\\" signifas neniom, krom se estas aliaj ludantoj por kontraŭkompari tiun atingon.";i['whyIsLichessCalledLichess']="Kial Lichess nomiĝas tiel?";i['whyIsLilaCalledLila']=s("Simile, la fontkodo por Lichess, %1$s, signifas li[chess in sca]la, ĉar la plejparto de Lichess estas skribita en %2$s, intuicia programlingvo.");i['whyLiveLightLibre']="Realtempe, ĉar ludoj estas ludataj kaj spektataj plentempe 24/7; malpeza kaj libera pro la fakto, ke Lichess estas malferma-kodo kaj sen proprietaj rubaĵoj, kiuj multas en aliaj retejoj.";i['yesLichessInspiredOtherOpenSourceWebsites']=s("Jes. Lichess efektive inspiris aliajn malfermkodajn retejojn, kiuj uzas niajn %1$s, %2$s aŭ %3$s.");i['youCannotApply']="Ne eblas peti por fariĝi moderatoro. Se ni vidi iun, kiun ni pensi, estus bone kiel moderatoro, ni kontaktos ilin direkte.";i['youCanUseOpeningBookNoEngine']="Ĉe Lichess, la ĉefa diferenco en reguloj por koresponda ŝako estas, ke malferma libro estas permesita. La uzado de motoroj daŭre estas malpermesita kaj rezultigos esti markita por motora asistado. Kvankam, ICCF permesas motora uzado en korespondo, Lichess ne permesas."})()