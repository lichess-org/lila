"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.faq)window.i18n.faq={};let i=window.i18n.faq;i['accounts']="Računi";i['acplExplanation']="Stotina kmeta je merska enota, ki se v šahu uporablja kot prikaz prednosti. Stotina kmeta je enak 1/100 kmeta. Torej 100 stotin kmeta = 1 kmet. Te vrednote nimajo formalne vloge v igri, so pa uporabne za igrače in so bistvene za računalniški šah za ocenjevanje položajev.\n\nZgornja poteza računalnika bo izgubila nič stotin kmeta, manjše poteze pa bodo poslabšale položaj, merjeno v stotinah kmeta.\n\nTo vrednost lahko uporabimo kot pokazatelj kakovosti igre. Manj kot stotin kmeta izgubimo na potezo, močnejša je igra.\n\nRačunalniško analizo lichess poganja Stockfish.";i['aHourlyBulletTournament']="urni Bullet turnir";i['areThereWebsitesBasedOnLichess']="Ali obstajajo spletna mesta, ki temeljijo na Lichess-u?";i['asWellAsManyNMtitles']="veliko državnih mojstrskih naslovov";i['basedOnGameDuration']=s("Nadzor časa na lichessu temelji na ocenjenem trajanju igre = %1$s\nNa primer, predvideno trajanje igre 5 + 3 je 5 × 60 + 40 × 3 = 420 sekund.");i['beingAPatron']="biti pokrovitelj";i['beInTopTen']="biti med prvimi 10-imi v tem rejtingu.";i['breakdownOfOurCosts']="razčlenitev naših stroškov";i['canIbecomeLM']="Ali lahko dobim Lichess Master (LM) naslov?";i['canIChangeMyUsername']="Ali lahko spremenim svoje uporabniško ime?";i['configure']="nastavi";i['connexionLostCanIGetMyRatingBack']="Izgubil sem partijo zaradi zakasnitve/prekinitve povezave. Ali lahko dobim nazaj svoje rejtinške točke?";i['desktop']="namizje";i['discoveringEnPassant']="Zakaj lahko kmet vzame drugega kmeta, ko je že ob njem? (en passant)";i['displayPreferences']="prikaži nastavitve";i['durationFormula']="(začetni čas ure) + 40 × (prirastek ure)";i['eightVariants']="6 šahovskih različic";i['enableAutoplayForSoundsA']="Večina brskalnikov lahko prepreči predvajanje zvoka na sveže naloženi strani, da zaščiti uporabnike. Predstavljajte si, da bi vas lahko vsako spletno mesto takoj zasulo z zvočnimi oglasi.\n\nRdeča ikona za izklop zvoka se prikaže, ko vaš brskalnik lichess.org prepreči predvajanje zvoka. Običajno se ta omejitev odpravi, ko nekaj kliknete. V nekaterih mobilnih brskalnikih se vlečenje kosa na dotik ne šteje kot klik. V tem primeru se morate dotakniti table, da omogočite zvok na začetku vsake igre.\n\nPrikažemo rdečo ikono, da vas opozorimo, ko se to zgodi. Pogosto lahko lichess.org izrecno dovolite predvajanje zvokov. Tu so navodila, kako to narediti v najnovejših različicah nekaterih priljubljenih brskalnikov.";i['enableAutoplayForSoundsChrome']="1. Pojdite na lichess.org\n2. Kliknite ikono ključavnice v naslovni vrstici\n3. Kliknite Nastavitve mesta\n4. Dovoli zvok";i['enableAutoplayForSoundsFirefox']="1. Pojdite na lichess.org\n2. Pritisnite Ctrl-i v sistemu Linux/Windows ali cmd-i v sistemu MacOS\n3. Kliknite zavihek Dovoljenja\n4. Dovolite avdio in video na lichess.org";i['enableAutoplayForSoundsMicrosoftEdge']="1. Kliknite tri pike v zgornjem desnem kotu\n2. Kliknite Nastavitve\n3. Kliknite Piškotki in dovoljenja za spletno mesto\n4. Pomaknite se navzdol in kliknite Samodejno predvajanje medijev\n5. Dodajte lichess.org v Dovoli";i['enableAutoplayForSoundsQ']="Omogočiti samodejno predvajanje za zvoke?";i['enableAutoplayForSoundsSafari']="1. Pojdite na lichess.org\n2. V menijski vrstici kliknite Safari\n3. Kliknite Nastavitve za lichess.org ...\n4. Dovoli vse samodejno predvajanje";i['enableDisableNotificationPopUps']="Omogočiti ali onemogočiti pojavna okna?";i['enableZenMode']=s("Omogočite Zen-način v %1$s ali s pritiskom na %2$s med partijo.");i['explainingEnPassant']=s("To je pravilna poteza, znana kot \\\"en passant\\\". Članek v Wikipediji vsebuje %1$s.\n\nOpisano je v oddelku 3.7 (d) %2$s:\n\n\\\"Kmet, ki zaseda polje na enakem mestu kot in na sosednji koloni nasprotnikov kmet, ki je pravkar napredoval za dva polj v eni potezi od prvotnega polja, lahko vzame nasprotnikovega kmeta, kot da bi bil slednji premaknjen samo eno polje. zajemanje je po poteku le zakonito na poti in se imenuje \\\"en passant\\\" zajemanje. \\\"\n\nGlejte %3$s na tej potezi za nekaj vaj z njo.");i['fairPlay']="Poštena igra";i['fairPlayPage']="stran za pošteno igro";i['faqAbbreviation']="Odgovori na pogosto zastavljena vprašanja";i['fideHandbookX']=s("Priročnik FIDE %s");i['findMoreAndSeeHowHelp']=s("Več o %1$s (vključno z %2$s). Če želite Lichessu pomagati s prostovoljnim časom in spretnostmi, je tukaj veliko %3$s.");i['frequentlyAskedQuestions']="Pogosto zastavljena vprašanja";i['gameplay']="Igranje";i['goldenZeeExplanation']="ZugAddict je pretakal in zadnji dve uri je poskušal premagati A.I. stopnja 8 v igri 1 + 0, brez uspeha. Thibault mu je rekel, da bo, če bo to uspešno opravil v toku, dobil edinstven pokal. Eno uro kasneje je razbil Stockfish in obljuba je bila izpolnjena.";i['goodIntroduction']="dober uvod";i['guidelines']="smernice";i['havePlayedARatedGameAtLeastOneWeekAgo']="zadnji teden odigrati ocenjeno partijo za ta rejting,";i['havePlayedMoreThanThirtyGamesInThatRating']="odigrati najmanj 30 ocenjenih partij s tem rejtingom";i['hearItPronouncedBySpecialist']="Poslušajte, kako ime izgovori strokovnjak.";i['howBulletBlitzEtcDecided']="Kako so izbrane Bullet, Blitz in druge časovne kontrole izbrane?";i['howCanIBecomeModerator']="Kako lahko postanem moderator?";i['howCanIContributeToLichess']="Kako lahko prispevam k Lichess-u?";i['howDoLeaderoardsWork']="Kako delujejo uvrstitve in lestvice najboljših?";i['howToHideRatingWhilePlaying']="Kako skriti rejting med igranjem?";i['howToThreeDots']="Kako...";i['inferiorThanXsEqualYtimeControl']=s("> %1$ss = %2$s");i['inOrderToAppearsYouMust']=s("Če želite priti na %1$s, morate:");i['insufficientMaterial']="Izguba po času, remi in premalo figur";i['isCorrespondenceDifferent']="Ali se dopisni šah razlikuje od običajnega šaha?";i['keyboardShortcuts']="Katere bližnjice na tipkovnici obstajajo?";i['keyboardShortcutsExplanation']="Nekatere strani Lichess imajo bližnjice na tipkovnici, ki jih lahko uporabite. Poskusite pritisniti \\'?\\' tipko na strani študije, analize, uganke ali igre za seznam razpoložljivih bližnjic na tipkovnici.";i['leavingGameWithoutResigningExplanation']="Če vaš nasprotnik pogosto prekine / zapusti igre, se jim dodeli oznaka \\\"igranje prepovedano\\\", kar pomeni, da jim lahko začasno prepovejo igranje iger. To javno ni navedeno na njihovem profilu. Če se to vedenje nadaljuje, se dolžina prepovedi igranja poveča - in takšno dolgotrajno vedenje lahko privede do zaprtja računa.";i['leechess']="li-čes";i['lichessCanOptionnalySendPopUps']="Lichess lahko pošilja pojavna obvestila, na primer, ko ste na vrsti ali ste prejeli zasebno sporočilo.\n\nKliknite ikono ključavnice poleg naslova lichess.org v vrstici URL vašega brskalnika.\n\nNato izberite, ali želite dovoliti ali blokirati obvestila strani Lichess.";i['lichessCombinationLiveLightLibrePronounced']=s("Lichess je kombinacija live/light/ libre in šaha. Izgovarja se %1$s.");i['lichessFollowFIDErules']=s("V primeru, da enemu igralcu zmanjka časa, ta igralec običajno izgubi igro. Vendar je igra remizirana, če je položaj tak, da nasprotnik z nobeno možno serijo zakonitih potez (%1$s) ne more matirati kralja igralca.\n\nV redkih primerih je težko samodejno odločiti (prisilne črte, trdnjave). Privzeto smo vedno na strani igralca, ki mu ni zmanjkalo časa.\n\nUpoštevajte, da se lahko pari z enim skakačem ali lovcem, če ima nasprotnik figuro, ki bi lahko blokirala kralja.");i['lichessPoweredByDonationsAndVolunteers']="Lichess poganjajo donacije pokroviteljev in prizadevanja ekipe prostovoljcev.";i['lichessRatings']="Lichess ratingi";i['lichessRecognizeAllOTBtitles']=s("Lichess prepozna vse naslove FIDE, pridobljene z igro OTB (čez ploščo), pa tudi %1$s. Tu je seznam naslovov FIDE:");i['lichessSupportChessAnd']=s("Lichess podpira standardni šah in %1$s.");i['lichessTraining']="Lichess usposabljanje";i['lMtitleComesToYouDoNotRequestIt']="Ta častni naslov je neuraden in obstaja samo na Lichessu.\n\nPo lastni presoji ga redko podelimo izjemnim igralcem, ki so dobri državljani Lichessa. Ne dobiš naslova LM, naslov LM pride do tebe. Če izpolnjujete pogoje, boste od nas prejeli sporočilo v zvezi s tem in izbiro, da sprejmete ali zavrnete.\n\nNe zahtevajte naslova LM.";i['notPlayedEnoughRatedGamesAgainstX']=s("Igralec še ni zaključil dovolj rangiranih iger proti %1$s v kategoriji rangiranja.");i['notPlayedRecently']="Igralec zadnje čase ni igral dovolj partij. Glede na število odigranih partij, ste lahko približno eno leto nedejavni, da bo vaša ocena spet postala začasna.";i['notRepeatedMoves']="Potez nismo ponavljali. Zakaj je bila igra še vedno remizirana s ponavljanjem?";i['noUpperCaseDot']="Ne.";i['otherWaysToHelp']="drugi načini pomoči";i['ownerUniqueTrophies']=s("Ta pokal je edinstven v zgodovini Lichessa, nihče drug kot %1$s ga ne bo imel nikoli.");i['pleaseReadFairPlayPage']=s("Za več informacij preberite naš %s");i['positions']="pozicije";i['preventLeavingGameWithoutResigning']="Kaj se naredi, ko igralci zapustijo igre, ne da bi predali?";i['provisionalRatingExplanation']="Vprašaj pomeni, da je rating začasen. Razlogi vključujejo:";i['ratingDeviationLowerThanXinChessYinVariants']=s("imeti odstopanje rejtinga nižje kot %1$s v klasičnem šahu in nižje kot %2$s v različicah,");i['ratingDeviationMorethanOneHundredTen']="Konkretno to pomeni, da je Glicko-2 odstopanje večje od 110. Odstopanje je stopnja zaupanja sistema v rejting. Nižje kot je odstopanje, bolj stabilena je rejting.";i['ratingLeaderboards']="lestvica najboljših";i['ratingRefundExplanation']="Minuto po tem, ko je igralec zaznamovan, se začne njegovih 40 zadnjih rangiranih iger v zadnjih 3 dneh. Če ste bili njegov nasprotnik na teh igrah, ste izgubili rejting (zaradi poraza ali remija) in vaš rejting ni bil začasen, prejmete povračilo za rejting. Nadomestilo se omeji na podlagi vašega najvišjega rejtinga in vašega napredka po igri. \n (Na primer, če se je vaš rejting po teh igrah močno povečal, morda ne boste dobili nobenega povračila ali samo delno povračilo.) Povračilo ne bo nikoli preseglo 150 točk.";i['ratingSystemUsedByLichess']="Ratingi se izračunajo po metodi rangiranja Glicko-2, ki jo je razvil Mark Glickman. To je zelo priljubljena metoda rangiranja, ki jo uporablja veliko število šahovskih organizacij (FIDE je izjemen primer, saj še vedno uporabljajo datiran sistem rangiranja Elo).\n\nV osnovi ratingi Glicko pri izračunu in predstavitvi vašega ratinga uporabljajo \\\"intervale zaupanja\\\". Ko prvič začnete uporabljati spletno mesto, se vaš rating začne pri 1500 ± 700. 1500 predstavlja vaš rating, 700 pa interval zaupanja.\n\nV bistvu je sistem 90% prepričan, da je vaš rating nekje med 800 in 2200. Je neverjetno negotova. Zaradi tega se bo, ko igralec šele začne, njegov rating zelo drastično spremenil, potencialno nekaj sto točk hkrati. Toda po nekaterih igrah z uveljavljenimi igralci se bo interval zaupanja zožil in količina pridobljenih / izgubljenih točk po vsaki tekmi se bo zmanjšala.\n\nDruga točka, ki jo je treba opozoriti, je, da se bo čas zaupanja povečeval. To vam omogoča, da hitreje pridobivate / izgubljate točke, da se ujemate s kakršnimi koli spremembami vaše spretnosti v tem času.";i['repeatedPositionsThatMatters']=s("Trikratno ponavljanje je ponovitev %1$s in ne premiki. Ni nujno, da se ponavljanje ponavlja.");i['secondRequirementToStopOldPlayersTrustingLeaderboards']="Druga zahteva je, da igralci, ki ne uporabljajo več svojih računov, prenehajo zasedati lestvico najboljših.";i['showYourTitle']=s("Če imate naslov OTB, se lahko prijavite, da se to prikaže na vašem računu, tako da izpolnite %1$s, vključno z jasno sliko identifikacijskega dokumenta / kartice in selfiejem, na katerem imate dokument / kartico.\n\nPreverjanje, da je naslovni igralec na Lichessu, omogoča dostop do iger na dogodkih Titled Arena.\n\nKončno je tu še častni naslov %2$s.");i['similarOpponents']="nasprotniki podobnih moči";i['superiorThanXsEqualYtimeControl']=s("&gt %1$ss = %2$s");i['threeFoldHasToBeClaimed']=s("Ponovitev mora zahtevati eden od igralcev. To lahko storite tako, da pritisnete gumb, ki je prikazan, ali tako, da pred zadnjo ponavljajočo se potezo ponudite remi, ne bo pomembno, ali bo nasprotnik zavrnil ponudbo remija, vseeno bo zahtevana trikratna ponovitev. Lahko tudi nastavite %1$s Lichess, da samodejno zahteva ponovitve za vas. Poleg tega petkratno ponavljanje vedno takoj konča igro.");i['threefoldRepetition']="Trikratna ponovitev";i['threefoldRepetitionExplanation']=s("Če se pozicija pojavi trikrat, lahko igralci zahtevajo remi za %1$s. Lichess izvaja uradna pravila FIDE, kot je opisano v členu 9.2 %2$s.");i['threefoldRepetitionLowerCase']="trikratna ponovitev";i['titlesAvailableOnLichess']="Kakšni nazivi so na Lichess?";i['uniqueTrophies']="Edinstvene trofeje";i['usernamesCannotBeChanged']="Ne, uporabniških imen ni mogoče spreminjati iz tehničnih in praktičnih razlogov. Uporabniška imena so materializirana na premnogih mestih: zbirke podatkov, izvozi, dnevniki in mislih ljudi. Velike začetnice lahko prilagodite enkrat.";i['usernamesNotOffensive']=s("Na splošno uporabniška imena ne smejo biti žaljiva, lažna predstavitev nekoga drugega ali oglaševanje. Več o %1$s lahko preberete.");i['verificationForm']="obrazec za preverjanje";i['viewSiteInformationPopUp']="Pokaži pojavno okno spletnega mesta";i['wayOfBerserkExplanation']=s("Da bi ga dobil, se je hiimgosu izzval, da se znori in zmaga v 100%% igrah %s.");i['weCannotDoThatEvenIfItIsServerSideButThatsRare']="Žal ne moremo vračati rejtinških točk za partije, izgubljene zaradi zakasnitve ali prekinitve povezave, ne glede na to, ali je bila težava na vašem ali na našem delu. Slednje je sicer zelo redko. Prav tako upoštevajte, da pri ponovnem zagonu Lichess-a, partijo vedno prekinemo, da preprečimo poraz zaradi izteka časa.";i['weRepeatedthreeTimesPosButNoDraw']="Trikrat smo ponovili položaj. Zakaj igra ni bila remizirana?";i['whatIsACPL']="Kakšna je povprečna izguba stotine kmeta (ACPL)?";i['whatIsProvisionalRating']="Zakaj je poleg ratinga vprašaj (?)?";i['whatUsernameCanIchoose']="Kakšno je lahko moje uporabniško ime?";i['whatVariantsCanIplay']="Katere različice lahko igram na Lichess?";i['whenAmIEligibleRatinRefund']="Kdaj sem upravičen do samodejnega povračila ratinga od goljufov?";i['whichRatingSystemUsedByLichess']="Kakšen rating sistem uporablja Lichess?";i['whyAreRatingHigher']="Zakaj so rejtingi višji v primerjavi z drugimi spletnimi mesti in organizacijami, kot so FIDE, USCF in ICC?";i['whyAreRatingHigherExplanation']="Najbolje je, da pri rejtingu ne razmišljate kot absolutni številki oz. ga primerjate z rejtingom pri drugih organizacijah. Različne organizacije imajo različne ravni igralcev, različne sisteme ocenjevanja (Elo, Glicko, Glicko-2 ali spremenjena različica prej omenjenih). Ti dejavniki lahko drastično vplivajo na absolutne številke (rejtinge).\n\nNajbolje, da na rejting gledate kot na \\\"relativno\\\" številko (ne kot \\\"absolutno\\\"): znotraj skupine igralcev vam bodo njihove relativne razlike v rejtingih pomagale oceniti, kdo in kako pogosto bo nekdo zmagal/remiziral/izgubil. \\\"Imam rejting X\\\" nič ne pomeni, razen če obstajajo drugi igralci, s katerimi se lahko ta rejting primerja.";i['whyIsLichessCalledLichess']="Zakaj se Lichess imenuje Lichess?";i['whyIsLilaCalledLila']=s("Podobno izvorna koda za Lichess, %1$s, pomeni li[chess in sca]la, saj je večina lichess napisana v %2$s, intuitivnem programskem jeziku.");i['whyLiveLightLibre']="V živo, ker se igre igrajo in gledajo v realnem času 24/7; lahek in svoboden za dejstvo, da je Lichess odprtokoden in neobremenjen z lastniškimi smeti, ki težijo druge spletne strani.";i['yesLichessInspiredOtherOpenSourceWebsites']=s("Da. Lichess je resnično navdihnil druga mesta z odprto kodo, ki uporabljajo našo %1$s, %2$s ali %3$s.");i['youCannotApply']="Če želite postati moderator, se ni mogoče prijaviti. Če vidimo nekoga, za katerega mislimo, da bi bil dober kot moderator, ga bomo kontaktirali neposredno.";i['youCanUseOpeningBookNoEngine']="Na Lichess, je glavna razlika v pravilih ta, da je dovoljena uporaba otvoritvenih knjižnic. Uporaba računalniških motorjev je prepovedana in ima za posledico zaznamek za pomoč z računalnikom. Čeprav ICCF dovoljuje uporabo motorjev v dopisnem šahu jih Lichess ne."})()