"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.swiss)window.i18n.swiss={};let i=window.i18n.swiss;i['comparison']="Vergelijking";i['durationUnknown']="Vooraf gedefinieerd maximum aantal rondes, maar duur onbekend";i['dutchSystem']="Systeem Keizer";i['earlyDrawsAnswer']="In Zwitserse partijen kunnen de spelers geen remise overeenkomen voordat er 30 zetten gespeeld zijn. Deze maatregel kan weliswaar geen vooraf afgesproken remises voorkomen, maar maakt het minder gemakkelijk om in de loop van het spel zelf tot remise over te gaan.";i['earlyDrawsQ']="Wat gebeurt er bij vroegtijdige remises?";i['FIDEHandbook']="FIDE-handboek";i['forbiddedUsers']="Als deze lijst niet leeg is, worden gebruikers die niet op deze lijst staan niet toegelaten. Eén gebruikersnaam per regel.";i['forbiddenPairings']="Verboden paringen";i['forbiddenPairingsHelp']="Gebruikersnamen van spelers waarvan je niet wilt dat ze tegen elkaar moeten spelen (bijvoorbeeld broers en zussen). Vul twee gebruikersnamen per regel in, gescheiden door een spatie.";i['identicalForbidden']="Niet toegestaan";i['identicalPairing']="Identieke paring";i['joinOrCreateTeam']="Word lid van een team of maak zelf een team aan";i['lateJoin']="Laat meedoen";i['lateJoinA']="Ja, tot meer dan de helft van de rondes is begonnen; in een Zwitsers toernooi van 11 rondes bijvoorbeeld, kunnen spelers meedoen voordat ronde 6 begint en in een toernooi van 12 rondes voordat ronde 7 begint.\nLate deelnemers krijgen een enkele bye, ook als ze meerdere rondes gemist hebben.";i['lateJoinQ']="Kunnen spelers nog meedoen als het toernooi al begonnen is?";i['lateJoinUntil']="Ja, tot meer dan de helft van de rondes is begonnen";i['moreRoundsThanPlayersA']="Als alle mogelijke paringen zijn afgewerkt, wordt het toernooi beëindigd en een winnaar uitgeroepen.";i['moreRoundsThanPlayersQ']="Wat gebeurt er als het toernooi meer rondes heeft dan spelers?";i['nbRounds']=p({"one":"%s ronde","other":"%s rondes"});i['newSwiss']="Nieuw Zwitsers toernooi";i['nextRound']="Volgende ronde";i['nowPlaying']="Nu bezig";i['numberOfByesA']="Een speler krijgt één punt voor elke keer dat er geen paring tot stand kan komen.\nDaarnaast wordt een enkele bye van een halve punt toegekend als een speler gaat deelnemen als het toernooi al begonnen is.";i['numberOfByesQ']="Hoeveel byes kan een speler krijgen?";i['numberOfGames']="Aantal partijen";i['numberOfGamesAsManyAsPossible']="Zoveel als er kunnen worden gespeeld binnen de toegewezen tijdsduur";i['numberOfGamesPreDefined']="Vooraf bepaald, hetzelfde voor alle spelers";i['numberOfRounds']="Aantal rondes";i['numberOfRoundsHelp']="Een oneven aantal rondes zorgt voor een optimale kleurenbalans.";i['oneRoundEveryXDays']=p({"one":"Eén ronde per dag","other":"Eén ronde elke %s dagen"});i['ongoingGames']=p({"one":"Lopende partij","other":"Lopende partijen"});i['otherSystemsA']="Momenteel zijn we niet van plan om meer toernooisystemen aan Lichess toe te voegen.";i['otherSystemsQ']="Hoe zit het met andere toernooisystemen?";i['pairingsA']=s("Met het %1$s, geïmplementeerd door %2$s, in overeenstemming met het %3$s.");i['pairingsQ']="Hoe komen paringen tot stand?";i['pairingSystem']="Paringsysteem";i['pairingSystemArena']="Elke beschikbare tegenstander met vergelijkbare rating";i['pairingSystemSwiss']="Beste paring gebaseerd op punten en tiebreaks";i['pairingWaitTime']="Wachttijd voor paring";i['pairingWaitTimeArena']="Snel: wacht niet op alle spelers";i['pairingWaitTimeSwiss']="Langzaam: wacht op alle spelers";i['pause']="Pauzeren";i['pauseSwiss']="Ja, maar kan het aantal rondes verminderen";i['playYourGames']="Speel jouw partijen";i['pointsCalculationA']="Een overwinning is één punt waard, remise een half punt, en een verlies nul punten.\nAls een speler niet kan worden gepaard tijdens een ronde, krijgt deze een bye met één punt.";i['pointsCalculationQ']="Hoe worden punten berekend?";i['possibleButNotConsecutive']="Mogelijk, maar niet opeenvolgend";i['predefinedDuration']="Vooraf gedefinieerde duur in minuten";i['predefinedUsers']="Sta enkel vooraf gedefinieerde gebruikers toe om mee te doen";i['protectionAgainstNoShowA']="Spelers die zich aanmelden voor een Zwitsers toernooi, maar vervolgens niet spelen kunnen een probleem zijn.\nOm dit probleem te verhelpen verbiedt Lichess spelers die niet komen opdagen bij een partij om een nieuw Zwitsers evenement te starten, voor een bepaalde periode. De maker van een Zwitser evenement kan er voor kiezen om deze spelers alsnog te laten spelen.";i['protectionAgainstNoShowQ']="Wat gebeurt er als iemand niet komt opdagen?";i['restrictedToTeamsA']="Zwitserse toernooien zijn niet ontworpen voor online schaken. Ze vereisen punctualiteit, toewijding en geduld van spelers.\nWij denken dat deze voorwaarden binnen een team waarschijnlijk eerder zullen worden vervuld dan in wereldwijde toernooien.";i['restrictedToTeamsQ']="Waarom is het beperkt tot teams?";i['roundInterval']="Interval tussen rondes";i['roundRobinA']="We zouden het graag toevoegen, maar helaas round-robin werkt niet online.\nDe reden is dat het geen eerlijke manier biedt om om te gaan met mensen die het toernooi vroegtijdig verlaten. We kunnen niet verwachten dat alle spelers al hun partijen in een online evenement ook zullen spelen. Dat gebeurt gewoonweg niet, en als gevolg daarvan zouden de meeste round-robintoernooien fouten bevatten en oneerlijk zijn, wat afbreuk doet aan hun bestaansrecht.\nWat het meest in de buurt komt van een online round-robin is een Zwitsers toernooi met een zeer groot aantal rondes. Dan worden alle mogelijke paringen gespeeld voordat het toernooi eindigt.";i['roundRobinQ']="Hoe zit het met Round Robin?";i['roundsAreStartedManually']="Rondes worden handmatig gestart";i['similarToOTB']="Vergelijkbaar met fysieke toernooien";i['sonnebornBergerScore']="Sonneborn-Bergerscore";i['startingIn']="Begint over";i['startingSoon']="Begint binnenkort";i['streaksAndBerserk']="Reeksen en Berserk";i['swiss']="Zwitsers";i['swissDescription']=s("In een Zwitsers toernooi %1$s speelt elke concurrent niet noodzakelijkerwijs tegen alle andere deelnemers. Deelnemers ontmoeten elkaar een-op-een in elke ronde en worden gepaard met een serie regels die ervoor zorgt dat elke deelnemer tegenstanders treft met een vergelijkbare score, maar niet dezelfde tegenstander meer dan één keer. De winnaar is de deelnemer die in alle rondes de meeste punten heeft verdiend. Alle deelnemers spelen in elke ronde, tenzij er een oneven aantal spelers is.");i['swissTournaments']="Zwitserse toernooien";i['swissVsArenaA']="In een Zwitsers toernooi spelen alle deelnemers hetzelfde aantal partijen en kunnen ze elkaar maar één keer treffen. Dit kan een goede optie zijn voor clubs en officiële toernooien.";i['swissVsArenaQ']="Wanneer gebruik je een Zwitsers toernooi in plaats van een arena-toernooi?";i['teamOnly']=s("Zwitserse toernooien kunnen alleen worden aangemaakt door teamleiders, en kunnen alleen worden gespeeld door teamleden.           \n%1$s om te beginnen met spelen in Zwitserse toernooien.");i['tiebreaksCalculationA']=s("Met de %s.\nVoeg de punten toe van elke tegenstander van wie de speler heeft gewonnen plus de helft van de punten van elke tegenstander met wie de speler remise speelde.");i['tiebreaksCalculationQ']="Hoe worden tiebreaks berekend?";i['tournDuration']="Duur van het toernooi";i['tournStartDate']="Begindatum toernooi";i['unlimitedAndFree']="Onbeperkt en gratis";i['viewAllXRounds']=p({"one":"Bekijk de ronde","other":"Bekijk alle %s rondes"});i['whatIfOneDoesntPlayA']="Hun klok loopt, ze gaan door de vlag en verliezen de partij.\nVervolgens wordt de speler uit het toernooi gehaald, zodat deze niet meer partijen verliest.\n Op elk moment kan de speler opnieuw deelnemen aan het toernooi.";i['whatIfOneDoesntPlayQ']="Wat gebeurt er als een speler geen partij speelt?";i['willSwissReplaceArenasA']="Nee. Het zijn aanvullende functies.";i['willSwissReplaceArenasQ']="Zullen Zwitserse toernooien de arena-toernooien vervangen?";i['xMinutesBetweenRounds']=p({"one":"%s minuut tussen rondes","other":"%s minuten tussen rondes"});i['xRoundsSwiss']=p({"one":"%s ronde Zwitsers","other":"%s ronden Zwitsers"});i['xSecondsBetweenRounds']=p({"one":"%s seconde tussen rondes","other":"%s seconden tussen rondes"})})()