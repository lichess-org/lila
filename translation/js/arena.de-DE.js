"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.arena)window.i18n.arena={};let i=window.i18n.arena;i['allAveragesAreX']=s("Alle Durchschnittswerte auf dieser Seite sind %s.");i['allowBerserk']="Berserk erlauben";i['allowBerserkHelp']="Lasse Spieler ihre Bedenkzeit halbieren, um einen zusätzlichen Punkt zu erzielen";i['allowChatHelp']="Lasse Spieler in einem Chatraum diskutieren";i['arena']="Arena";i['arenaStreaks']="Arena-Siegesserien";i['arenaStreaksHelp']="Nach 2 Siegen gewähren aufeinander folgende Siege 4 Punkte anstelle von 2.";i['arenaTournaments']="Arena-Turniere";i['averagePerformance']="Durchschnittliche Leistung";i['averageScore']="Durchschnittliche Punktzahl";i['berserk']="Arena Berserk";i['berserkAnswer']="Wenn ein Spieler den \\\"Berserk\\\"-Knopf zu Beginn der Partie drückt, verliert er die Hälfte seiner Zeit auf der Uhr, aber ein Sieg wird mit einem zusätzlichen Turnierpunkt honoriert.\n\nIn einem Zeitmodus mit Inkrement entfernt \\\"Berserk\\\" das Inkrement. (1+2 ist eine Ausnahme, es wird zu 1+0.)\n\nFür Partien ohne Grundbedenkzeit (0+1, 0+2) ist \\\"Berserk\\\" nicht verfügbar.\n\nDer zusätzliche Punkt für \\\"Berserk\\\" wird nur vergeben, wenn du mindestens sieben Züge in der Partie gemacht hast.";i['bestResults']="Beste Ergebnisse";i['created']="Erstellt";i['customStartDate']="Benutzerdefiniertes Startdatum";i['customStartDateHelp']="In deiner eigenen, lokalen Zeitzone. Dies wird die Einstellung \\\"Zeit bevor das Turnier beginnt\\\" überschreiben";i['defender']="Verteidiger";i['drawingWithinNbMoves']=p({"one":"Remis innerhalb des ersten Zugs wird keinem Spieler Punkte einbringen.","other":"Bei Remis innerhalb der ersten %s Züge werden keinem Spieler Punkte gut geschrieben."});i['drawStreakStandard']=s("Remis-Serien: Wenn ein Spieler in mehreren aufeinanderfolgenden Arena-Partien Remis erzielt, wird nur für das erste Remis und in Standard-Partien für Remis, in denen mehr als %s Züge gespielt wurden, ein Punkt vergeben. Eine Remis-Serie kann nur durch einen Sieg gebrochen werden, nicht durch eine verlorene Partie oder ein Unentschieden.");i['drawStreakVariants']="Die minimale Partielänge, um für remisierte Partien Punkte zu erhalten, unterscheidet sich je nach gespielter Variante. Die Tabelle unten listet die Grenzen für jede Variante auf.";i['editTeamBattle']="Teamkampf bearbeiten";i['editTournament']="Turnier bearbeiten";i['history']="Arena-Historie";i['howAreScoresCalculated']="Wie werden die Punkte berechnet?";i['howAreScoresCalculatedAnswer']="Grundsätzlich gibt es für einen Sieg 2 Punkte, 1 Punkt für Remis und 0 Punkte für eine Niederlage.\nGewinnst du zwei Partien in Folge, beginnst du eine Serie mit verdoppelter Punktzahl (dargestellt durch ein Flammensymbol). Für die nachfolgenden Partien erhältst du solange die doppelte Punktzahl, bis du eine Partie nicht gewinnst.\nDas bedeutet, dass du für einen Sieg vier, für ein Remis zwei und für eine Niederlage keine Punkte bekommst.\n\nBeispiel: Zwei Siege, gefolgt von einem Remis sind 6 Punkte wert: 2 + 2 + (2 * 1)";i['howDoesItEnd']="Wann endet das Turnier?";i['howDoesItEndAnswer']="Während des Turniers wird die Zeit heruntergezählt. Wenn sie null erreicht, wird die Rangliste eingefroren und der Gewinner bekanntgegeben. Laufende Partien müssen beendet werden, zählen aber nicht mehr für das Turnier.";i['howDoesPairingWork']="Wie werden die Turnierpaarungen ermittelt?";i['howDoesPairingWorkAnswer']="Zu Beginn des Turniers werden die Spieler auf Grundlage der Wertungszahl zugeteilt.\nSobald du ein Spiel beendet hast, kannst du zur Turnierübersicht zurückkehren. Dir wird dann ein Spieler mit ähnlichem Rang zugeteilt. Dadurch wird eine kurze Wartezeit erreicht, allerdings kann es sein, dass du nicht gegen alle anderen Spieler in diesem Turnier spielen wirst.\nSpiele schnell und kehre zur Turnierübersicht zurück, um mehr Spiele zu spielen und eine höhere Punktzahl zu erzielen.";i['howIsTheWinnerDecided']="Wie wird der Gewinner ermittelt?";i['howIsTheWinnerDecidedAnswer']="Der oder die Spieler mit den meisten Punkten am Turnierende werden als Gewinner bekanntgegeben.\n\nWenn zwei oder mehr Spieler die gleiche Punktzahl besitzen, entscheidet die Turnierleistung als Feinwertung.";i['isItRated']="Sind die Partien gewertet?";i['isNotRated']="Dieses Turnier ist *nicht* gewertet und wird deine Wertungszahl *nicht* beeinflussen.";i['isRated']="Dieses Turnier ist gewertet und wird deine Wertungszahl beeinflussen.";i['medians']="Medianwerte";i['minimumGameLength']="Minimale Partielänge";i['myTournaments']="Meine Turniere";i['newTeamBattle']="Neuer Teamkampf";i['noArenaStreaks']="Keine Arena-Siegesserien";i['noBerserkAllowed']="Kein Berserken erlaubt";i['onlyTitled']="Nur Spieler mit Titel";i['onlyTitledHelp']="Für die Teilnahme am Turnier ist ein offizieller Titel erforderlich";i['otherRules']="Andere wichtige Regeln";i['pickYourTeam']="Wähle dein Team";i['pointsAvg']="Punkte im Schnitt";i['pointsSum']="Punkte insgesamt";i['rankAvg']="Platzierungs-Durchschnitt";i['rankAvgHelp']="Der Platzierungs-Durchschnitt ist ein Prozentwert deiner Platzierungen. Niedriger ist besser.\n\nEin Beispiel: Wenn man in einem Turnier von 100 Spielern Platz 3 erreicht hat = Top 3%. In einem Turnier mit 1000 Spielern den Rang 10 zu erreichen = Top 1%.";i['recentlyPlayed']="Kürzlich gespielt";i['shareUrl']=s("Teile diese URL, um andere einzuladen: %s");i['someRated']="Manche Turniere sind gewertet und werden deine Wertungszahl beeinflussen.";i['stats']="Statistiken";i['thereIsACountdown']="Es gibt einen Countdown für deinen ersten Zug. Ziehst du nicht innerhalb dieser Zeit, wird der Sieg deinem Gegner zugesprochen.";i['thisIsPrivate']="Dies ist ein privates Turnier";i['total']="Gesamt";i['tournamentShields']="Turnier-Schilde";i['tournamentStats']="Turnierstatistiken";i['tournamentWinners']="Turniersieger";i['variant']="Variante";i['viewAllXTeams']=p({"one":"Das Team ansehen","other":"Alle %s Teams ansehen"});i['whichTeamWillYouRepresentInThisBattle']="Welches Team wirst du in diesem Kampf repräsentieren?";i['willBeNotified']="Du wirst benachrichtigt, wenn das Turnier beginnt. Es ist somit möglich, während der Wartezeit in einem anderen Tab zu spielen.";i['youMustJoinOneOfTheseTeamsToParticipate']="Du musst dich einem dieser Teams anschließen, um teilnehmen zu können!"})()