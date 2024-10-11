"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.swiss)window.i18n.swiss={};let i=window.i18n.swiss;i['absences']="Nieobecności";i['byes']="Punkty bez gry";i['comparison']="Porównanie";i['durationUnknown']="Ustalona maksymalna liczba rund, czas trwania nieokreślony";i['dutchSystem']="systemu holenderskiego";i['earlyDrawsAnswer']="W turniejach szwajcarskich, gracze nie mogą proponować remisu przed 30 posunięciem. Mimo że środek ten nie zapobiega remisom na żądanie, to przynajmniej utrudnia takie praktyki.";i['earlyDrawsQ']="Co się stanie gdy zbyt wcześnie nastąpi remis?";i['FIDEHandbook']="przepisami gry w szachy FIDE";i['forbiddedUsers']="Jeśli ta lista nie jest pusta, nazwy użytkowników nieobecne na niej nie będą mogły dołączyć do turnieju. Jedna nazwa użytkownika na linię.";i['forbiddenPairings']="Zabronione pary";i['forbiddenPairingsHelp']="Nazwy graczy, którzy nie mogą grać ze sobą (na przykład rodzeństwo). Dwie nazwy graczy w linii, oddzielone spacją.";i['identicalForbidden']="Niemożliwe";i['identicalPairing']="Wiele partii z tym samym graczem";i['joinOrCreateTeam']="Dołącz lub utwórz klub";i['lateJoin']="Dołączenie po starcie";i['lateJoinA']="Tak, dopóki nie rozpocznie się ponad połowa rund. Na przykład w 11-rundowym turnieju uczestnicy mogą dołączyć przed rozpoczęciem się 6 rundy, a w 12-rundowym turnieju przed rozpoczęciem się rundy 7.\nUczestnicy dołączający po rozpoczęciu turnieju otrzymują jeden wolny los (punkt bez gry), nawet jeśli przegapili już kilka rund.";i['lateJoinQ']="Czy można dołączyć do turnieju po jego rozpoczęciu?";i['lateJoinUntil']="Tak, zanim nie rozpocznie się więcej niż połowa rund";i['manualPairings']="Ręczne parowanie w następnej rundzie";i['manualPairingsHelp']="Określ ręcznie wszystkie pary następnej rundy. Jedna para graczy na linię. Przykład:\nGraczA GraczB\nGraczC GraczD\nAby przyznać graczowi 1 punkt zamiast pary, dodaj linię taką jak ta:\nGraczE 1\nBrakujących graczy uważamy za nieobecnych i nie dostaną oni punktów.\nPozostaw to pole puste, aby umożliwić automatyczne parowania.";i['moreRoundsThanPlayersA']="Po rozegraniu wszystkich możliwych par, turniej zakończy się i zostanie ogłoszony zwycięzca.";i['moreRoundsThanPlayersQ']="Co się stanie, jeśli turniej ma więcej rund niż graczy?";i['mustHavePlayedTheirLastSwissGame']="Wymagane rozegranie swojej poprzedniej partii w turnieju szwajcarskim";i['mustHavePlayedTheirLastSwissGameHelp']="Pozwól dołączyć graczom tylko wtedy, gdy rozegrali swoją ostatnią partię w turnieju szwajcarskim. Jeśli w ostatnim turnieju szwajcarskim opuszczali partie, nie będą mogli dołączyć do Twojego turnieju. Dzięki temu do turnieju dołączą gracze, którzy stawiają się na wszystkie swoje partie.";i['nbRounds']=p({"one":"%s runda","few":"%s rundy","many":"%s rund","other":"%s rund"});i['newSwiss']="Nowy turniej systemem szwajcarskim";i['nextRound']="Następna runda";i['nowPlaying']="W toku";i['numberOfByesA']="Uczestnik turnieju dostaje wolny los (punkt bez gry) za każdym razem, gdy system parowania nie może znaleźć dla niego przeciwnika.\nDodatkowo, wolny los przyznawany jest uczestnikowi, który dołącza do turnieju po jego rozpoczęciu.";i['numberOfByesQ']="Ile wolnych losów (punktów bez gry) może otrzymać uczestnik?";i['numberOfGames']="Liczba partii";i['numberOfGamesAsManyAsPossible']="Tak dużo, ile można rozegrać w ustalonym czasie trwania turnieju";i['numberOfGamesPreDefined']="Ustalana na początku turnieju, taka sama dla wszystkich uczestników";i['numberOfRounds']="Liczba rund";i['numberOfRoundsHelp']="Nieparzysta liczba rund pozwala na optymalną równowagę kolorów.";i['oneRoundEveryXDays']=p({"one":"Jedna runda dziennie","few":"Jedna runda co %s dni","many":"Jedna runda co %s dni","other":"Jedna runda co %s dni"});i['ongoingGames']=p({"one":"Trwająca partia","few":"Trwające partie","many":"Trwające partie","other":"Trwające partie"});i['otherSystemsA']="W tej chwili nie planujemy dodawać innych systemów turniejowych na Lichess.";i['otherSystemsQ']="Co z innymi systemami turniejowymi?";i['pairingsA']=s("Pary ustalane są według %1$s, zaimplementowanego przez %2$s, zgodnie z %3$s.");i['pairingsQ']="Jak ustalane są pary?";i['pairingSystem']="System parowania";i['pairingSystemArena']="Dowolny dostępny przeciwnik o podobnym rankingu";i['pairingSystemSwiss']="Najlepsze dostępne parowanie w oparciu o punkty i punkty pomocnicze";i['pairingWaitTime']="Czas oczekiwania na parowanie";i['pairingWaitTimeArena']="Szybki: nie czekaj na wszystkich graczy";i['pairingWaitTimeSwiss']="Wolne: czekaj na wszystkich graczy";i['pause']="Wstrzymywanie";i['pauseSwiss']="Tak, ale może to zmniejszyć liczbę rund";i['playYourGames']="Rozegraj swoje partie";i['pointsCalculationA']="Zwycięstwo warte jest jeden punkt, remis to pół punktu, a przegrana to brak punktów.\nGdy uczestnik nie może być sparowany podczas rundy, otrzymuje wtedy tzw. wolny los warty jeden punkt.";i['pointsCalculationQ']="Jak oblicza się punkty?";i['possibleButNotConsecutive']="Możliwe, ale nie pod rząd";i['predefinedDuration']="Ustalony czas trwania w minutach";i['predefinedUsers']="Zezwalaj na dołączenie do turnieju tylko wstępnie zdefiniowanym użytkownikom";i['protectionAgainstNoShowA']="Gracze, którzy zapisali się do turnieju w systemie szwajcarskim, ale nie rozgrywają swoich partii, stanowią problem.\nW celu minimalizacji skali takich sytuacji Lichess uniemożliwia przez pewien czas zapisy na turnieje w systemie szwajcarskim graczom, którzy nie podeszli do partii. \nTwórca turnieju może jednak zezwolić im na dołączenie.";i['protectionAgainstNoShowQ']="Co się dzieje z graczami, którzy nie przystępują do partii?";i['restrictedToTeamsA']="System szwajcarski nie został opracowany dla rozgrywek online. Wymaga od uczestników punktualności i cierpliwości.\nUważamy, że warunki te są bardziej prawdopodobne do spełnienia w klubie niż w turniejach otwartych.";i['restrictedToTeamsQ']="Dlaczego tylko dla klubów?";i['roundInterval']="Interwał między rundami";i['roundRobinA']="Chcielibyśmy go dodać, ale niestety system kołowy nie sprawdza się w turniejach online.\nProblemem jest to, że nie ma sprawiedliwego sposobu radzenia sobie z uczestnikami, którzy wcześnie porzucają turniej. Trudno oczekiwać, że wszyscy uczestnicy zagrają wszystkie swoje partie. To po prostu się nie zdarza, a w rezultacie większość turniejów byłaby wadliwa i niesprawiedliwa, co podważa ich sens istnienia.\nNajbardziej zbliżoną formą do systemu kołowego jest właśnie system szwajcarski z bardzo dużą liczbą rund. W takim turnieju zagrają ze sobą wszystkie możliwe pary.";i['roundRobinQ']="Co z systemem kołowym?";i['roundsAreStartedManually']="Rundy są uruchamiane ręcznie";i['similarToOTB']="Podobieństwo do turniejów przy szachownicy";i['sonnebornBergerScore']="system Sonneborn–Berger";i['startingIn']="Rozpoczęcie za";i['startingSoon']="Rozpoczną się wkrótce";i['streaksAndBerserk']="Serie zwycięstw i berserk";i['swiss']="Szwajcarski";i['swissDescription']=s("W systemie szwajcarskim %1$s, uczestnik niekoniecznie rozegra partie ze wszystkimi innymi uczestnikami turnieju. Uczestnicy parowani są ze sobą przy użyciu zestawu zasad mających na celu zapewnienie, że każdy zagra z przeciwnikiem mającym podobny wynik, jednak nie więcej niż raz z tym samym przeciwnikiem. Zwycięzcą turnieju jest uczestnik z najwyższą łączną liczbą punktów uzyskaną we wszystkich rundach. Wszyscy uczestnicy turnieju grają w każdej rundzie, chyba że istnieje nieparzysta liczba uczestników.");i['swissTournaments']="System szwajcarski";i['swissVsArenaA']="W systemie szwajcarskim wszyscy uczestnicy grają tę samą liczbę partii i mogą grać ze sobą tylko raz.\nMoże to być dobra opcja dla klubów i oficjalnych turniejów.";i['swissVsArenaQ']="Kiedy używać systemu szwajcarskiego zamiast aren?";i['teamOnly']=s("Turnieje systemem szwajcarskim mogą być tworzone wyłącznie przez liderów klubów i mogą być grane tylko przez członków tego klubu.\n%1$s, aby zacząć grać systemem szwajcarskim.");i['tieBreak']="Tie break";i['tiebreaksCalculationA']=s("Stosujemy %s.\nLiczba punktów pomocniczych danego uczestnika składa się z sumy punktów końcowych tych uczestników, z którymi dany uczestnik wygrał oraz połowy punktów końcowych, z którymi dany uczestnik zremisował.");i['tiebreaksCalculationQ']="Jak wyznaczany jest zwycięzca, gdy wielu uczestników uzyskało ten sam wynik?";i['tournDuration']="Czas trwania turnieju";i['tournStartDate']="Data rozpoczęcia turnieju";i['unlimitedAndFree']="Bez ograniczeń i opłat";i['viewAllXRounds']=p({"one":"Zobacz rundę","few":"Zobacz wszystkie %s rundy","many":"Zobacz wszystkie %s rund","other":"Zobacz wszystkie %s rund"});i['whatIfOneDoesntPlayA']="Jego czas na zegarze będzie upływał, aż skończy się i wtedy przegra partię.\nNastępnie system wycofa takiego uczestnika z turnieju, więc nie przegra on więcej partii.\nUczestnik ten może w każdej chwili ponownie dołączyć do turnieju.";i['whatIfOneDoesntPlayQ']="Co się stanie, jeśli uczestnik nie rozpocznie partii?";i['willSwissReplaceArenasA']="Nie. To dodatkowy system turniejowy na Lichess.";i['willSwissReplaceArenasQ']="Czy system szwajcarski zastąpi areny?";i['xMinutesBetweenRounds']=p({"one":"%s minunta między rundami","few":"%s minuty między rundami","many":"%s minunt między rundami","other":"%s minut między rundami"});i['xRoundsSwiss']=p({"one":"%s runda","few":"%s rundy","many":"%s rund","other":"%s rund"});i['xSecondsBetweenRounds']=p({"one":"%s sekunda między rundami","few":"%s sekundy między rundami","many":"%s sekund między rundami","other":"%s sekund między rundami"})})()