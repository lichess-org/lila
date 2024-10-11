"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.swiss)window.i18n.swiss={};let i=window.i18n.swiss;i['comparison']="Салыстырма";i['durationUnknown']="Айналымдар саны белгіленген, бірақ ұзақтығы беймәлім";i['dutchSystem']="Голандиялық жүйе";i['earlyDrawsAnswer']="Швейцарлық ойында ойыншы 30 қадам жасамай тепе-теңдік сұрауға жол жоқ. Бұл шектеу жоспарлы тепе-теңдікке тосқауыл болмаса да, тепе-теңдіктер санын азайтуда септігі бар.";i['earlyDrawsQ']="Ерте тепе-теңдік не болды?";i['FIDEHandbook']="FIDE нұсқаулығы";i['forbiddedUsers']="Егер де осы тізімде біреу көрсетілсе, тізімде жоқ ойыншылардың қосылуға руқсаты болмайды. Әр жолға – бір ойыншының аты.";i['forbiddenPairings']="Тыйым салынған жұптар";i['forbiddenPairingsHelp']="Бір-бірімен ойнамайтын ойыншылар аттары (туыстар, мысалы). Бір жолға бір жұптың аттары, бос орынмен ажыратылған.";i['identicalForbidden']="Рұқсат емес";i['identicalPairing']="Бірдеу жұптау";i['joinOrCreateTeam']="Топты жасау не қосылу";i['lateJoin']="Кешіге қосылу";i['lateJoinA']="Айналым санының жартысы өтпесе, Иә. Мысалы, 11-айналымды жарыста 6-ыншы айналымға дейін немесе 12-айналымды жарыста 7-інші айналымға дейін қосылуына болады.";i['lateJoinQ']="Ойыншылар кешіге қосыла алады ма?";i['lateJoinUntil']="Рұқсат, бірақ айналымдардың жартысы өтпегенше";i['moreRoundsThanPlayersA']="Барлық мүмкін болған жұптар ойындарын аяқтаған соң, жарыс та аяқталады, жеңімпаз жария болады.";i['moreRoundsThanPlayersQ']="Егер жарыста айналым саны ойыншылар санынан артық болса, не болады?";i['nbRounds']=p({"one":"%s айналым","other":"%s айналым"});i['newSwiss']="Жаңа Швейцарлық жарыс";i['nextRound']="Келесі айналым";i['nowPlaying']="Қазір ойында";i['numberOfByesA']="Жүйе ойыншыны жұптай алмағанда, ойыншы бір ұпай алады.\nСонымен қатар, ойыншы жарысқа кешігіп қосылса, жарты \\\"ақтаушы\\\" ұпай алады.";i['numberOfByesQ']="Ойыншы қанша \\\"ақтаушы\\\" ұпай алуы мүмкін?";i['numberOfGames']="Ойын саны";i['numberOfGamesAsManyAsPossible']="Ұзақтығымен шектелген барынша көп";i['numberOfGamesPreDefined']="Алдын-ала келісіп белгіленген, барлық ойыншыларға бірдей";i['numberOfRounds']="Айналым саны";i['numberOfRoundsHelp']="Жұп сан болса, түстер теңдігі сақталады.";i['oneRoundEveryXDays']=p({"one":"Күніне бір айналым","other":"%s күнде бір айналым"});i['ongoingGames']=p({"one":"Болып жатқан ойын","other":"Болып жатқан ойын"});i['otherSystemsA']="Әзірге басқа жарыс тәртіптерін Лическе қосу ойымызда жоқ.";i['otherSystemsQ']="Басқа да жарыс тәртіптері туралы не дейсіз?";i['pairingsA']=s("%1$s-де %3$s-на сәйкес %2$s арқылы шешіледі.");i['pairingsQ']="Жұптасу мәселесі қалай шешіледі?";i['pairingSystem']="Жұптау жүйесі";i['pairingSystemArena']="Дәрежесі шамалас кез-келген қарсылас";i['pairingSystemSwiss']="Ұпай мен шешуші ойынға негізделген ең тиімді жұптау";i['pairingWaitTime']="Жұптау уақыты";i['pairingWaitTimeArena']="Тез: барлық ойыншыны күтпейді";i['pairingWaitTimeSwiss']="Баяу: барлық ойыншыны күтеді";i['pause']="Үзіліс";i['pauseSwiss']="Рұқсат, бірақ айналым саны азаяды";i['playYourGames']="Тағы ойын бастау";i['pointsCalculationA']="Жеңіс – 1, тепе-теңдік – 0,5, жеңіліс – 0 ұпай.\nЕгер ойыншы бір айналымда жұптаспай қалса, ол бір \\\"ақтаушы\\\" ұпай алады.";i['pointsCalculationQ']="Ұпайды қалай есептейді?";i['possibleButNotConsecutive']="Рұқсат, бірақ қатарынан емес";i['predefinedDuration']="Ұзақтығы белгіленген (минутпен)";i['predefinedUsers']="Тек алдын-ала бекітілген ойыншылар ғана қосыла алады";i['protectionAgainstNoShowA']="Швейцарлық ойынға кірген, бірақ қатыспағандар қиындық тудыруы мүмкін.\nМәселені шешу үшін Личес оларға белгіленген бір уақыт аралығында жаңа ойындарға қатысуда тыйым орнатты.\nДегенмен, Швейцарлық ойын құрастырушысы оларға рұқсат беруге қауһарлы.";i['protectionAgainstNoShowQ']="Қатыспағандарға қандай шара?";i['restrictedToTeamsA']="Швейцарлық жарыс онлайн шахмат ретінде жасалмаған. Ол ойыншыдан ұқыптылық, сабырлық талап етеді.\nӘлемдік жарыстан гөрі бір топтың ішінде осы талаптарды орындау ыңғайлы деп ойлаймыз.";i['restrictedToTeamsQ']="Топтарға неге шектеу қойылды?";i['roundInterval']="Айналымдар аралығы";i['roundRobinA']="Біз оны қосуға даяр едік, бірақ, өкінішке орай, Round Robin онлайн жұмыс істемейді.\nНегізгі қиындық мынада: жарыстан ерте шыққан қатысушыларды әділ түрде қадағалау мүмкін емес. Онлайн шарада барлық ойыншылар өзінің барлық ойындарын ойнайтынына сену қиын. Шындығында, олай болмайды, сондықтан бүкіл онлайн түріндегі Round Robin жарысы – жарымшыл, әділсіз. Олай болса, осындай жарысты бастаудың да мәні жоқ.\nБірақ Швейцарлық жарысты барынша Round Robin жарысына ұқсатуға болады. Ол үшін айналым саны өте көп болуы керек. Сонда барлық мүмкін жұптасудан кейін жарыс аяқталады.";i['roundRobinQ']="Round Robin туралы не дейсіз?";i['roundsAreStartedManually']="Айналымды өзіңіз бастайсыз";i['similarToOTB']="Шынайы жарыстарға ұқсас";i['sonnebornBergerScore']="Sonneborn–Berger әдісімен";i['startingIn']="Басталуға қалған уақыт";i['startingSoon']="Басталуға әзір";i['streaksAndBerserk']="Тізбектер мен Берсерк";i['swissDescription']=s("Швейцарлық жарыста %1$s әр қатысушы қалғандардың барлығымен ойнап шығатынына кепіл жоқ. Әр ойында қатысушылар жекпе-жек ойнайды, оларды жұптау үшін ережелер жинағы жасалды. Ережеге сәйкес ойыншылардың дәрежелері шамалас, әрі алдында жұпталмаған болуы керек, Жеңімпаз болып бүкіл ойындарда қорытынды ұпайы көп ойыншы аталады. Егер ойыншылар саны тақ болмаса, барлық қатысушылар саны бірдей ойын ойнайды.");i['swissTournaments']="Швейцарлық жарыстар";i['swissVsArenaA']="Швейцарлық жарыста әр қатысушы басқасымен тек бір рет кездесіп, барлығы саны бірдей ойын ойнайды. Бұл клубтар мен ресми жарыстарға оңтайлы.";i['swissVsArenaQ']="Қай жағдайда Алаң орнына Швейцарлық жарысты ойнаймыз?";i['teamOnly']=s("Швейцарлық жарысты жасай алатын – тек топ жетекшісі, жарысқа қатыса алатындар – тек  топ мүшелері.           Швейцарлық жарыста ойнау үшін %1$s.");i['tiebreaksCalculationA']=s("%s.\nҚарсылас арасынан жеңілгендердің толық ұпайлары мен тепе-теңдікке жеткендердің жарты ұпайлары қосылады.");i['tiebreaksCalculationQ']="Тай-брейкті қалай есептейді?";i['tournDuration']="Жарыс ұзақтығы";i['tournStartDate']="Жарыстың басталу уақыты";i['unlimitedAndFree']="Шектеусіз әрі тегін";i['viewAllXRounds']=p({"one":"Айналымды қарау","other":"Барлық %s айналымды қарау"});i['whatIfOneDoesntPlayA']="Уақыт бітеді де, ойыншы жеңіледі.\nКейін жүйе оны жарыстан шығарады, бірақ ойыншы кез-келген уақытта қайта қосылуға қабілетті.";i['whatIfOneDoesntPlayQ']="Егер ойыншы ойнамай отырса, не болады?";i['willSwissReplaceArenasA']="Жоқ, олар әр-түрлі жарыстар.";i['willSwissReplaceArenasQ']="Швейцарлық жарыс Алаң жарысын алмастыра ма?";i['xMinutesBetweenRounds']=p({"one":"Айналым арасында %s минут","other":"Айналым арасында %s минут"});i['xRoundsSwiss']=p({"one":"Швейцарлық ойынның %s кезеңі","other":"Швейцарлық ойынның %s кезеңі"});i['xSecondsBetweenRounds']=p({"one":"Айналым арасында %s секунд","other":"Айналым арасында %s секунд"})})()