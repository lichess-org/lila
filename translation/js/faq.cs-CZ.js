"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.faq)window.i18n.faq={};let i=window.i18n.faq;i['accounts']="Účty";i['acplExplanation']="V šachu se jako jednotka síly jednotlivých tahů ve srovnání s nejlepšími možnými tahy doporučenými počítačem používá tzv. centipawn (z angličtiny). Jeden centipawn je ekvivalentní jedné setině hodnoty pěšáka. To znamená, že 100 centipawnů odpovídá jednomu pěšákovi.\n\nNejlepší tah doporučený počítačem ztrácí 0 centipawnů (z definice). Slabší tah povede na zhoršení pozice, a právě toto zhoršení pozice se měří v centipawnech.\n\nTato hodnota se používá pro ověření kvality hry. Čím nižší má hráč pěšcovou ztrátu, tím silnější tahy hraje.\n\nPočítačová analýza na Lichessu využívá program Stockfish. Hráčovy tahy jsou tedy porovnávány vůči tahům, které by zahrál Stockfish.";i['adviceOnMitigatingAddiction']=s("Pravidelně dostáváme zprávy od uživatelů, kteří nás žádají o pomoc, abychom jim zabránili v příliš velkém hraní.\n\nZatímco Lichess nezakazuje ani neblokuje hráče kromě porušení smluvních podmínek, doporučujeme použít externí nástroje k omezení přílišného hraní. Některé běžné návrhy pro blokátory webových stránek zahrnují %1$s, %2$sa %3$s. Pokud chcete nadále používat web, ale necítit pokušení, můžete se také zajímat o %4$s, zde je jeden s %5$s.\n\nNěkteří hráči se mohou cítit, že se jejich chování mění v závislost. WHO totiž klasifikuje poruchy hry jako %6$s, přičemž základní funkce jsou 1) narušenou kontrolou hraní, 2) zvýšení priority v oblasti her 3) eskalace her navzdory negativním důsledkům. Pokud si myslíte, že vaše hraní šachu je podle tohoto vzoru, povzbuzujeme vás, abyste o tom hovořili s přítelem nebo s rodinným příslušníkem a/nebo s profesionálem.");i['aHourlyBulletTournament']="hodinový bullet turnaj";i['areThereWebsitesBasedOnLichess']="Existují nějaké webové stránky založené na Lichess?";i['asWellAsManyNMtitles']="mnoho titulů jednotlivých národních federací";i['basedOnGameDuration']=s("Časové kontroly Lichess jsou založeny na odhadu doby trvání partie = %1$s\nNapříklad odhadovaná délka partie 5+3 je 5 × 60 + 40 × 3 = 420 sekund.");i['beingAPatron']="patronství";i['beInTopTen']="být v top 10 v dané časové kontrole.";i['breakdownOfOurCosts']="rozložení našich nákladů";i['canIbecomeLM']="Mohu získat titul mistra Lichess (LM)?";i['canIChangeMyUsername']="Mohu si změnit své uživatelské jméno?";i['configure']="nakonfigurovat";i['connexionLostCanIGetMyRatingBack']="Kvůli pomalému připojení/ztrátě připojení jsem prohrál partii, mohu získat své ratingové body zpátky?";i['desktop']="počítač";i['discoveringEnPassant']="Proč může pěšák sebrat pěšáka, který už je za ním? (En passant aneb braní mimochodem)";i['displayPreferences']="předvolbách";i['durationFormula']="(počáteční čas) + 40 × (přídavek)";i['eightVariants']="8 variant šachu";i['enableAutoplayForSoundsA']="Většina prohlížečů může v zájmu ochrany uživatelů zabránit přehrávání zvuku na čerstvě načtené stránce. Představte si, že by vás každá webová stránka mohla okamžitě bombardovat zvukovou reklamou.\n\nČervená ikona ztlumení zvuku se zobrazí, když prohlížeč zabrání přehrávání zvuku na stránce lichess.org. Obvykle se toto omezení zruší, jakmile na něco kliknete. V některých mobilních prohlížečích se přetažení figurky dotykem nepočítá jako kliknutí. V takovém případě musíte na začátku každé hry klepnout na šachovnici, aby byl zvuk povolen.\n\nZobrazujeme červenou ikonu, abychom vás upozornili, když k tomu dojde. Často můžete výslovně povolit, aby lichess.org přehrával zvuky. Zde jsou pokyny, jak to udělat v posledních verzích některých populárních prohlížečů.";i['enableAutoplayForSoundsChrome']="1. Jděte na lichess.org\n2. Klikněte na ikonu zámku v adresním řádku\n3. Klikněte na možnost Nastavení webu\n4. Povolit zvuk";i['enableAutoplayForSoundsFirefox']="1. Jděte na lichess.org\n2. Stiskněte klávesu Ctrl-i v systému Linux/Windows nebo cmd-i v systému MacOS.\n3. Klikněte na kartu Oprávnění\n4. Povolit zvuk a video na lichess.org";i['enableAutoplayForSoundsMicrosoftEdge']="1. Klikněte na tři tečky v pravém horním rohu.\n2. Klikněte na možnost Nastavení\n3. Klikněte na položku Cookies a oprávnění webu\n4. Přejděte dolů a klikněte na možnost Automatické přehrávání médií\n5. Přidejte lichess.org do Povolit";i['enableAutoplayForSoundsQ']="Povolit automatické přehrávání zvuků?";i['enableAutoplayForSoundsSafari']="1. Jděte na lichess.org\n2. Klikněte na Safari na liště nabídek\n3. Klikněte na položku Nastavení pro lichess.org ...\n4. Povolit veškeré automatické přehrávání";i['enableDisableNotificationPopUps']="Povolit nebo zakázat oznámení?";i['enableZenMode']=s("V %1$s si zapněte zen-mód, či stiskněte %2$s během partie.");i['explainingEnPassant']=s("Tento tah je však platný a podle pravidel, a nazává se \\\"en passant\\\", neboli braní mimochodem. Můžete si přečíst tento článek na Wikipedii: %1$s.\n\nBraní mimochodem je také popsáno v článku 3.7 (d) %2$s:\n\\\"Pěšec ohrožující pole, které přešel soupeřův pěšec, jenž ze základního pole postoupil jedním tahem o dvě pole, může tohoto soupeřova pěšce brát, jako by tento pěšec postoupil pouze o jedno pole. Toto braní lze provést pouze jako bezprostřední odpověď a nazývá se „braní mimochodem“.\\\"\nPro procvičení můžete použít %3$s.");i['fairPlay']="Fair Play";i['fairPlayPage']="stránku fair play";i['faqAbbreviation']="Často kladené dotazy (FAQ)";i['fewerLobbyPools']="méně nabídek lobby";i['fideHandbook']="Příručka FIDE";i['fideHandbookX']=s("FIDE příručka %s");i['findMoreAndSeeHowHelp']=s("Více informací o %1$s (včetně %2$s). Pokud chcete pomoci Lichess dobrovolnictvím vašeho času a dovedností, existuje mnoho %3$s.");i['frequentlyAskedQuestions']="Často kladené otázky";i['gameplay']="Hraní hry";i['goldenZeeExplanation']="ZugAddict vysílal živý přenos poslední 2 hodiny, přičemž se snažil porazit AI level 8 v partii 1+0, ale neúspěšně. Thibault ho vyzval, že pokud AI porazí v průběhu jeho živého přenosu, dostane unikátní trofej. O hodinu později Stockfishe porazil, a trofej obdržel.";i['goodIntroduction']="dobré zahájení";i['guidelines']="pokyny";i['havePlayedARatedGameAtLeastOneWeekAgo']="odehráli jste hodnocenou hru během minulého týdne v dané časové kontrole,";i['havePlayedMoreThanThirtyGamesInThatRating']="odehrát alespoň 30 hodnocených partií v dané časové kontrole,";i['hearItPronouncedBySpecialist']="Poslechněte si, jak je vyslovováno profesionálem.";i['howBulletBlitzEtcDecided']="Podle čeho se rozhoduje, zda časomíra spadá pod bullet, blitz apod.?";i['howCanIBecomeModerator']="Jak se mohu stát moderátorem?";i['howCanIContributeToLichess']="Jak mohu pomoci Lichess?";i['howDoLeaderoardsWork']="Jak fungují žebříčky?";i['howToHideRatingWhilePlaying']="Jak skrýt rating během hraní partie?";i['howToThreeDots']="Jak...";i['inferiorThanXsEqualYtimeControl']=s("< %1$ss = %2$s");i['inOrderToAppearsYouMust']=s("Chcete-li se dostat na %1$s , musíte:");i['insufficientMaterial']="Prohrávání na čas, remízování a nedostatečný materiál";i['isCorrespondenceDifferent']="Je korespondenční hra jiná než normální šachy?";i['keyboardShortcuts']="Jaké klávesové zkratky jsou k dispozici?";i['keyboardShortcutsExplanation']="Některé stránky na Lichess mají klávesové zkratky, které můžete zkusit použít. Zkuste použít klávesu \\'?\\' při studii, analýze, úlohách nebo při hře pro zobrazení listu dostupných klávesových zkratek.";i['leavingGameWithoutResigningExplanation']="Hráčům, kteří často přerušují nebo ukončují hry, bude dočasně zakázáno hrát. Tato informace není veřejně uvedena na jejich profilu. Pokud v tomto chování pokračují, délka zákazu se prodlužuje a nakonec jim může být úplně uzavřen účet.";i['leechess']="ličes";i['lichessCanOptionnalySendPopUps']="Lichess vám také může posílat oznámení, například když jste na tahu či po obdržení soukromé zprávy. \nKlikněte na ikonu zámku vedle URL adresy ve Vašem prohlížeči, a poté vyberte zda zablokovat či povolit oznámení.";i['lichessCombinationLiveLightLibrePronounced']=s("Lichess je kombinace slov live/light/libre a chess z angličtiny. Vyslovuje se %1$s.");i['lichessFollowFIDErules']=s("Když hráči dojde čas, obvykle to znamená, že partii prohraje. Platí zde ovšem jedna výjimka, a to v případě, že je na šachovnici taková pozice, že by mu nebylo možné dát mat žádnou legální kombinací tahů. V takovém případě se bude jednat o remízu (viz %1$s).\n\nVe vzácných případech je obtížné to rozpoznat automaticky (např. u tzv. \\\"fortressů\\\", česky \\\"pevností\\\"). Pokud si nejsme jistí, vždy upřednostňujeme výhru hráče, který na čas neprohrál.\n\nMimochodem, mějte na paměti, že může být možné dát mat i jediným jezdcem nebo střelcem, pokud druhá strana má figuru, která by mohla králi zatarasit únikovou cestu.");i['lichessPoweredByDonationsAndVolunteers']="Lichess funguje díky darům od patronů a díky úsilí týmu dobrovolníků.";i['lichessRatings']="Ratingy";i['lichessRecognizeAllOTBtitles']=s("Lichess uznává všechny tituly FIDE pro hru naživo (nikoliv online), a také %1$s. Zde je seznam všech titulů FIDE:");i['lichessSupportChessAnd']=s("Lichess podporuje standardní šachy a %1$s.");i['lichessTraining']="tuto interaktivní lekci";i['lichessUserstyles']="Lichess uživatelské styly";i['lMtitleComesToYouDoNotRequestIt']="Tento čestný titul je neoficiální a existuje pouze na Lichess.\nUdělujeme jej pouze zřídka, a to hráčům, kteří se významně zasloužili o rozšiřování komunity Lichess. Titul Vám přidělíme my, požádat o něj nelze, ale můžete jej i odmítnout.";i['mentalHealthCondition']="samostatný mentální problém";i['notPlayedEnoughRatedGamesAgainstX']=s("Neodehráli jste dostatečné množství hodnocených partií proti %1$s v dané kategorii ratingu (blitz, rapid apod.)");i['notPlayedRecently']="Neodehráli jste dostatečné množství partií v nedávné minulosti. V závislosti na počtu partií, které jste dříve odehráli, může trvat přibližně rok, než se Vaše hodnocení opět stane provizorním.";i['notRepeatedMoves']="Neopakovali jsme tahy, i tak ale hra skončila remízou pro opakování tahů? Proč?";i['noUpperCaseDot']="Ne.";i['otherWaysToHelp']="další způsoby jak pomoci";i['ownerUniqueTrophies']=s("Tato trofej je jedinečná, a nikdo jiný než %1$s ji nikdy mít nebude.");i['pleaseReadFairPlayPage']=s("Pro více informací si prosím přečtěte %s");i['positions']="pozice";i['preventLeavingGameWithoutResigning']="Co děláte s hráči, kteří opouštějí rozehrané hry, aniž by se vzdali?";i['provisionalRatingExplanation']="Otazník znamená, že váš rating je provizorní. Důvody mohou být:";i['ratingDeviationLowerThanXinChessYinVariants']=s("mít odchylku hodnocení nižší než %1$s v standardním šachu a nižší než %2$s ve variantách,");i['ratingDeviationMorethanOneHundredTen']="Konkrétně to znamená, že odchylka Glicko-2 je větší než 110. Tato odchylka ukazuje, jak moc si je systém jistý ratingem daného hráče. Čím menší odchylka, tím stabilnější daný rating je.";i['ratingLeaderboards']="žebříček podle ratingu";i['ratingRefundExplanation']="Jednu minutu poté, co je hráč označen, vezmeme jejich posledních 40 hodnocených her za uplynulé 3 dny. Pokud jste proti němu v těchto hrách hráli, ztratili jste body (kvůli prohře nebo remíze) a vaše hodnocení nebylo prozatimní, budou vám body vráceny. Počet vrácených bodů je omezen v závislosti na vašem dosavadním nejlepším hodnocení a rovněž vývoji vašeho hodnocení po dané hře.\n (Například pokud se vaše hodnocení po těchto hrách mezitím výrazně zvyšovalo, je možné, že nedostanete nazpátek žádné body, a nebo jen jejich část.) Maximálně vám můžeme vrátit 150 bodů.";i['ratingSystemUsedByLichess']="Ratingy se počítají pomocí metody Glicko-2 vyvinuté Markem Glickmanem. Jedná se o velmi populární metodu, a je využívána značným množstvím šachových organizací (není mezi nimi FIDE, jež stále využívá již zastaralou metodu Elo). \n\nSystém Glicko používá při výpočtu vašeho ratingu tzv. „intervaly spolehlivosti“. Když se zaregistrujete, vaše hodnocení je rovno 1500 bodů s intervalem spolehlivosti 1000 bodů.\n\nTo znamená, že systém si je na 95 % jistý, že váš rating je někde mezi 500 a 2500. To je opravdu obrovské rozpětí, a tak je normální, že při prvních pár partiích rating hráče najednou poskočí o mnoho bodů, často i několik stovek. Po pár partiích proti hráčům s neprovizorním (bez otazníku) ratingem se interval spolehlivosti vašeho ratingu zúží, a počet bodů získaných/ztracený po každé partii se sníží.\n\nDále je také nutné poznamenat, že postupem času se interval spolehlivosti zvýší, což umožňuje získat/ztratit ratingové body rychleji tak, aby odpovídaly změnám Vašich šachových schopností.";i['repeatedPositionsThatMatters']=s("Trojnásobné opakování pozice se týká %1$s, nikoliv tahů. Pozice se nemusí opakovat ihned po sobě.");i['secondRequirementToStopOldPlayersTrustingLeaderboards']="Druhý požadavek je, aby hráči, kteří již své účty nepoužívají, přestali plnit žebříčky.";i['showYourTitle']=s("Jestliže máte šachový titul, který Lichess uznává, můžete vyplněním %1$s získat odznak titulovaného hráče, resp. Váš titul bude zobrazen před Vaším uživatelským jménem. \nJako ověřený titulovaný hráč se můžete účastnit tzv. Titled Arena turnaje na Lichess.\n\nNakonec existuje ještě čestný titul %2$s.");i['similarOpponents']="soupeřům podobné síly";i['stopMyselfFromPlaying']="Jak se donutit přestat hrát?";i['superiorThanXsEqualYtimeControl']=s("≥ %1$ss = %2$s");i['threeFoldHasToBeClaimed']=s("Trojí opakování pozice musí potvrdit jeden z hráčů, a to stisknutím tlačítka, které se v případě trojího opakování pozice objeví, či nabídnutím remízy před posledním ze tří opakování, přičemž i v případě, že soupeř remízu odmítne, partie skončí remízou. Lze také %1$s Lichess tak, aby za vás automaticky potvrzoval trojí opakování pozice. Pokud se poté jakákoliv pozice zopakuje pětkrát během partie, hra také ihned skončí remízou.");i['threefoldRepetition']="Trojí opakování pozice";i['threefoldRepetitionExplanation']=s("Pokud se jakákoliv pozice zopakuje třikrát, hráči mohou vynutit remízu podle pravidla %1$s. Lichess se řídí příslušným článek 9.2 pravidel FIDE, viz %2$s.");i['threefoldRepetitionLowerCase']="trojí opakování pozice";i['titlesAvailableOnLichess']="Jaké tituly jsou na Lichess?";i['uniqueTrophies']="Unikátní trofeje";i['usernamesCannotBeChanged']="Ne, uživatelská jména nelze měnit, a to z technických a praktických důvodů. Uživatelská jména jsou uložena na příliš mnoha místech: databázích, lozích a pamětech lidí. Velikost písmen lze upravit, ale pouze jednou.";i['usernamesNotOffensive']=s("Obecně platí, že uživatelská jména by neměla být: urážlivá, uživatel by se vydávat za někoho jiného, a neměla by sloužit jako reklama. Více si o tom můžete přečíst %1$s.");i['verificationForm']="ověřovacího formuláře";i['viewSiteInformationPopUp']="Zobrazit vyskakovací okno s informacemi o webu";i['watchIMRosenCheckmate']=s("Sledujte video mezinárodního mistra Erica Rosena: šach mat zbraním mimochodem %s.");i['wayOfBerserkExplanation']=s("Aby trofej získal, musel hiimgosu berserkovat a vyhrát všechny partie %s.");i['weCannotDoThatEvenIfItIsServerSideButThatsRare']="Bohužel nemůžeme vracet ratingové body za hry, které jste prohráli kvůli ztrátě připojení, bez ohledu na tom, zda byl problém na Vaší či naší straně. Tato druhá možnost je však velmi vzácná. Také si všimněte, že pokud se Lichess restartuje (plánovaně), automaticky přerušujeme všechny partie, aby nedošlo k nespravedlivé prohře na čas.";i['weRepeatedthreeTimesPosButNoDraw']="Třikrát jsme zopakovali stejnou pozici. Proč hra neskončila remízou?";i['whatIsACPL']="Co je to průměrná pěšcová ztráta (angl. \\\"average centipawn loss\\\")?";i['whatIsProvisionalRating']="Proč mám otazník (?) vedle svého hodnocení (ratingu)?";i['whatUsernameCanIchoose']="Jaké si mám zvolit uživatelské jméno?";i['whatVariantsCanIplay']="Jaké varianty mohu hrát na Lichess?";i['whenAmIEligibleRatinRefund']="Kdy mám nárok na automatické vrácení ratingu za zápasy s podvodníky?";i['whichRatingSystemUsedByLichess']="Jaký systém hodnocení Lichess využívá?";i['whyAreRatingHigher']="Proč jsou ratingy vyšší ve srovnání s jinými stránkami a organizacemi, jako je FIDE, USCF či ICC?";i['whyAreRatingHigherExplanation']="Nejlepší je nemyslet na hodnocení jako absolutní čísla, nebo je porovnávat s jinými organizacemi. Různé organizace mají různé úrovně hráčů, různé systémy hodnocení (Elo, Glicko, Glicko-2 nebo upravenou verzi výše uvedených). Tyto faktory můžou drasticky ovlivnit tato absolutní čísla (hodnocení).\n\nJe nejlepší myslet na hodnocení jako \\\"relativní\\\" údaje (na rozdíl od \\\"absolutních\\\"): V rámci skupiny hráčů, jejich relativní rozdíly v hodnocení Vám pomohou odhadnout kdo vyhraje/remízuje/prohraje a jak často. Říkat \\\"Mám X hodnocení\\\" nic neznamená, pokud neexistují další hráči, se kterými můžeme toto hodnocení porovnat.";i['whyIsLichessCalledLichess']="Proč se Lichess nazývá Lichess?";i['whyIsLilaCalledLila']=s("Podobně, zdrojový kód pro Lichess, %1$s, znamená li[chess in sca]la, protože většina Lichess je napsaná v programovacím jazyce %2$s, což je intuitivní programovací jazyk.");i['whyLiveLightLibre']="Živé, protože hry jsou hrány a sledovány v reálném čase 24/7; lehké a volné, protože Lichess má otevřený zdrojový kód a je nezatížené proprietárním nepořádkem, který zaplavuje ostatní weby.";i['yesLichessInspiredOtherOpenSourceWebsites']=s("Ano. Lichess vskutku inspiroval další otevřené stránky využívající náš %1$s, naše %2$s či naši %3$s.");i['youCannotApply']="Není možné žádat o funkci moderátora. Pokud uvidíme někoho, o němž si myslíme, že by byl dobrým moderátorem, přímo jej kontaktujeme.";i['youCanUseOpeningBookNoEngine']="Na Lichess je hlavní rozdílem v pravidlech pro korespondenční šachy to, že je povoleno použít knihu zahájení. Použití počítačových programů pro rady je stále zakázáno a bude mít za následek označení hry za hru s pomocí. Přestože ICCF povoluje použití počítačových programů v korespondenčním šachu, Lichess ne."})()