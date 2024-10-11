"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.swiss)window.i18n.swiss={};let i=window.i18n.swiss;i['absences']="Abwesend";i['byes']="Freilose";i['comparison']="Vergleich";i['durationUnknown']="Vordefinierte maximale Rundenanzahl, aber unbekannte Dauer";i['dutchSystem']="niederländischen System";i['earlyDrawsAnswer']="In Partien von Turnieren nach Schweizer System können Spieler nicht remisieren, bevor 30 Züge gespielt wurden. Obwohl diese Maßnahme vorab vereinbarte Remis nicht verhindern kann, macht sie es trotzdem schwieriger, in der laufenden Partie zu einer Übereinkunft zu gelangen.";i['earlyDrawsQ']="Was passiert bei frühzeitigen Remis?";i['FIDEHandbook']="FIDE-Handbuch";i['forbiddedUsers']="Falls diese Liste nicht leer ist, können nicht aufgeführte Benutzer nicht teilnehmen. Ein Benutzername pro Zeile.";i['forbiddenPairings']="Verbotene Paarungen";i['forbiddenPairingsHelp']="Benutzernamen von Spielern, die nicht gegeneinander spielen dürfen (zum Beispiel Geschwister). Zwei Benutzernamen pro Zeile, getrennt durch ein Leerzeichen.";i['identicalForbidden']="Verboten";i['identicalPairing']="Gleiche Paarung mehrmals";i['joinOrCreateTeam']="Erstelle ein Team oder tritt einem anderen Team bei";i['lateJoin']="Später Beitritt";i['lateJoinA']="Ja, bis mehr als die Hälfte der Runden begonnen wurden; so können Spieler zum Beispiel an einem elfründigen Turnier nach Schweizer System vor dem Beginn der Runde 6 und in einem zwölfründigen vor Beginn der Runde 7 beitreten. Späteinsteiger erhalten einen halben Punkt gutgeschrieben, auch wenn sie mehrere Runden verpasst haben.";i['lateJoinQ']="Können Spieler im Nachhinein beitreten?";i['lateJoinUntil']="Erlaubt, bis mehr als die Hälfte der Runden gestartet wurden";i['manualPairings']="Manuelle Paarungen in der nächsten Runde";i['manualPairingsHelp']="Gib alle Paarungen der nächsten Runde manuell ein. Ein Spielerpaar pro Zeile. Zum Beispiel:\nSpieler A Spieler B\nSpieler C Spieler D\nUm einem Spieler ein Freilos (1 Punkt) statt einer Paarung zu geben, füge eine Zeile wie folgt ein:\nSpieler E 1\nNicht gelistete Spieler werden als abwesend betrachtet und erhalten null Punkte.\nLass dieses Feld leer, um von Lichess automatisch Paare erstellen zu lassen.";i['moreRoundsThanPlayersA']="Wenn alle möglichen Paarungen gespielt wurden, wird das Turnier beendet und ein Sieger erklärt.";i['moreRoundsThanPlayersQ']="Was passiert, wenn das Turnier mehr Runden als Spieler hat?";i['mustHavePlayedTheirLastSwissGame']="Sie müssen ihre letzte Partie nach Schweizer System gespielt haben";i['mustHavePlayedTheirLastSwissGameHelp']="Lass Spieler nur dann teilnehmen, wenn sie ihr letztes Spiel nach Schweizer System gespielt haben. Wenn sie bei einem vor kurzem stattgefundenen Turnier nach Schweizer System nicht erschienen sind, können sie an deinem Turnier nicht teilnehmen. Das führt zu mehr Erfahrung mit dem Schweizer System für Spieler, die tatsächlich erscheinen.";i['nbRounds']=p({"one":"%s Runde","other":"%s Runden"});i['newSwiss']="Neues Turnier nach Schweizer System";i['nextRound']="Nächste Runde";i['nowPlaying']="Läuft gerade";i['numberOfByesA']="Ein Spieler bekommt jedes Mal, wenn das Paarungssystem keine Paarung für ihn findet, einen Punkt.\nZusätzlich wird ein halber Punkt vergeben, wenn ein Spieler einem Turnier zu spät beitritt.";i['numberOfByesQ']="Wie viele kampflose Punkte kann ein Spieler bekommen?";i['numberOfGames']="Anzahl der Partien";i['numberOfGamesAsManyAsPossible']="So viele wie in der zugewiesenen Dauer gespielt werden können";i['numberOfGamesPreDefined']="Im Voraus entschieden, für alle Spieler gleich";i['numberOfRounds']="Anzahl der Runden";i['numberOfRoundsHelp']="Eine ungerade Anzahl der Runden ermöglicht eine optimale Farbverteilung.";i['oneRoundEveryXDays']=p({"one":"Eine Runde pro Tag","other":"Eine Runde alle %s Tage"});i['ongoingGames']=p({"one":"Laufende Partie","other":"Laufende Partien"});i['otherSystemsA']="Wir planen derzeit keine weiteren Turniersysteme zu Lichess hinzuzufügen.";i['otherSystemsQ']="Was ist mit anderen Turniersystemen?";i['pairingsA']=s("Mit dem %1$s, implementiert von %2$s, in Übereinstimmung mit dem %3$s.");i['pairingsQ']="Wie werden Paarungen entschieden?";i['pairingSystem']="Paarungssystem";i['pairingSystemArena']="Jeder verfügbare Gegner mit ähnlichem Rang";i['pairingSystemSwiss']="Beste Paarung, basierend auf Punkten und Feinwertungen";i['pairingWaitTime']="Wartezeit für die Paarung";i['pairingWaitTimeArena']="Schnell: Wartet nicht auf alle Spieler";i['pairingWaitTimeSwiss']="Langsam: Wartet auf alle Spieler";i['pause']="Pause";i['pauseSwiss']="Ja, aber die Anzahl der Runden könnte verringert werden";i['playYourGames']="Spiele deine Partien";i['pointsCalculationA']="Ein Sieg ist einen Punkt wert, ein Remis einen halben Punkt, und eine Niederlage null Punkte. Wenn ein Spieler während einer Runde keinem anderen Spieler zugeteilt werden kann, erhält er einen Punkt.";i['pointsCalculationQ']="Wie werden Punkte berechnet?";i['possibleButNotConsecutive']="Möglich, aber nicht hintereinander";i['predefinedDuration']="Vordefinierte Dauer in Minuten";i['predefinedUsers']="Nur vordefinierten Benutzern erlauben beizutreten";i['protectionAgainstNoShowA']="Spieler, die sich für ein Turnier nach dem Schweizer System anmelden, aber ihre Partien nicht spielen, können ein Problem darstellen.\nUm diesem Problem entgegenzuwirken, verhindert Lichess, dass Spieler, die es versäumt haben, eine Partie zu spielen, sich für eine bestimmte Zeit für ein neues Turnier nach dem Schweizer System anmelden können.\nDer Initiator eines Turniers nach dem Schweizer System kann jedoch entscheiden, ihnen trotzdem die Teilnahme am Turnier zu ermöglichen.";i['protectionAgainstNoShowQ']="Was wird bei Nichterscheinen unternommen?";i['restrictedToTeamsA']="Turniere nach Schweizer System sind nicht für Online-Schach konzipiert. Sie erfordern von den Spielern Pünktlichkeit, Durchhaltevermögen und Geduld.\nWir glauben, dass diese Bedingungen eher innerhalb eines Teams als in öffentlichen Turnieren erfüllt werden.";i['restrictedToTeamsQ']="Warum ist es auf Teams beschränkt?";i['roundInterval']="Zeitabstand zwischen den Runden";i['roundRobinA']="Wir möchten es hinzufügen, aber leider funktionieren Rundenturniere online nicht.\nDer Grund dafür ist, dass es keine faire Möglichkeit gibt, mit Leuten umzugehen, die das Turnier vorzeitig verlassen. Wir können nicht erwarten, dass alle Spieler in einem Online-Event alle ihre Spiele spielen. Das wird einfach nicht passieren, und dadurch wären die meisten Rundenturniere fehlerhaft und unfair, was ihren eigentlichen Sinn zunichte macht.\nAm nähesten an ein online Rundenturnier kommt, ein Turnier nach Schweizer System und einer sehr hohen Anzahl an Runden zu spielen. Dadurch werden alle möglichen Paarungen gespielt, bevor das Turnier endet.";i['roundRobinQ']="Was ist mit Rundenturnieren?";i['roundsAreStartedManually']="Runden werden manuell gestartet";i['similarToOTB']="Ähnlich wie OTB Turniere";i['sonnebornBergerScore']="Sonneborn-Berger-Wertung";i['startingIn']="Beginnt in";i['startingSoon']="Beginnt in Kürze";i['streaksAndBerserk']="Siegesserien und Berserken";i['swiss']="Schweizer System";i['swissDescription']=s("In einem Turnier nach Schweizer System %1$s spielt jeder Spieler nicht notwendigerweise gegen alle anderen Teilnehmer. Die Teilnehmer treffen in jeder Runde eins gegen eins aufeinander und werden mithilfe einer Reihe von Regeln einander zugelost, um sicherzustellen, dass jeder Wettbewerber gegen Gegner mit einem ähnlichen Punktestand spielt, aber nicht gegen den gleichen Gegner mehr als einmal. Der Gewinner ist der Teilnehmer mit den höchsten Gesamtpunkten, die aus allen Runden addiert werden. Alle Spieler spielen in jeder Runde, es sei denn, es gibt eine ungerade Anzahl an Teilnehmern.");i['swissTournaments']="Turniere nach Schweizer System";i['swissVsArenaA']="Bei einem Turnier nach Schweizer System spielen alle Teilnehmer die gleiche Anzahl von Spielen und können nur einmal gegeneinander spielen.\nEs kann eine gute Option für Clubs und offizielle Turniere sein.";i['swissVsArenaQ']="Wann sollten Turniere nach Schweizer System anstelle von Arena-Turnieren verwendet werden?";i['teamOnly']=s("Turniere nach Schweizer System können nur von Teamleitern erstellt werden und können nur von Teammitgliedern gespielt werden.           \n%1$s um an Turnieren nach Schweizer System teilzunehmen.");i['tieBreak']="Tiebreak";i['tiebreaksCalculationA']=s("Mit dem %s.\nAddiere die Punkte aller Gegner, die der Spieler besiegt, und die Hälfte der Punkte aller Gegner, gegen die der Spieler remisiert.");i['tiebreaksCalculationQ']="Wie werden Tie-Breaks berechnet?";i['tournDuration']="Dauer des Turniers";i['tournStartDate']="Turnier-Startzeit";i['unlimitedAndFree']="Unbegrenzt und kostenlos";i['viewAllXRounds']=p({"one":"Die Runde ansehen","other":"Alle %s Runden ansehen"});i['whatIfOneDoesntPlayA']="Die Zeit wird ablaufen und er wird die Partie verlieren. Anschließend entfernt das System den Spieler vom Turnier, so dass dieser nicht noch mehr Partien verliert.\n Er kann jederzeit wieder am Turnier teilnehmen.";i['whatIfOneDoesntPlayQ']="Was passiert, wenn ein Spieler nicht zur Partie antritt?";i['willSwissReplaceArenasA']="Nein. Sie sind komplementäre Funktionen.";i['willSwissReplaceArenasQ']="Werden nach Schweizer System-Turniere Arena-Turniere ersetzen?";i['xMinutesBetweenRounds']=p({"one":"%s Minute zwischen den Runden","other":"%s Minuten zwischen den Runden"});i['xRoundsSwiss']=p({"one":"%s Runde Schweizer System","other":"%s Runden Schweizer System"});i['xSecondsBetweenRounds']=p({"one":"%s Sekunde zwischen den Runden","other":"%s Sekunden zwischen den Runden"})})()