"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.swiss)window.i18n.swiss={};let i=window.i18n.swiss;i['comparison']="Vergelyking";i['durationUnknown']="Vooraf gekiesde maks rondtes, maar die duur is onbekend";i['dutchSystem']="Duitse stelsel";i['FIDEHandbook']="FIDE handboek";i['forbiddedUsers']="As die lys nie leeg is nie, dan sal die gebruikers wat afwesig van die lys is verbied word om aan te sluit. Een gebruiker per lyn.";i['forbiddenPairings']="Voorbode indelings";i['forbiddenPairingsHelp']="Gebruikersname van spelers wat nie teen mekaar mag speel nie (byvoorbeeld gesinslede). Twee gebruikersname, met \\'n spasie tussen in, per lyntjie.";i['identicalForbidden']="Verbode";i['identicalPairing']="Identiese paring";i['joinOrCreateTeam']="Sluit aan of maak \\'n span";i['lateJoin']="Laat aansluitings";i['lateJoinA']="Ja, totdat meer as halfde van die rondtes begin het; byvoorbeeld in \\'n 11-rondtes switserse toernooi kan spelers aansluit tot voordat rondte 6 begin en in \\'n 12-rondtes toernooi voor rondte 7 begin.\nLaat aansluiters kry \\'n enkel loslootjie, selfs as hulle verskeie rondtes gemis het.";i['lateJoinQ']="Kan spelers laat aansluit?";i['lateJoinUntil']="Ja, tot meer as halfde van die rondtes al begin het";i['moreRoundsThanPlayersA']="Wanneer al moontlike indelings gespeel is, sal die toernooi geëndig word en `n wenner verklaar word.";i['moreRoundsThanPlayersQ']="Wat gebeur as die toernooi meer rondes as spelers het?";i['nbRounds']=p({"one":"%s ronde","other":"%s rondes"});i['newSwiss']="Nuwe Switserse toernooi";i['nextRound']="Volgende ronde";i['nowPlaying']="Speel tans";i['numberOfByesA']="\\'n Speler kry \\'n loslootjie van een punt elke keer wat die paring sisteem nie \\'n parings maat vir hul kan find nie.\nAditioneel, \\'n enkel loslootjie van \\'n halwe punt word toegedien wanneer \\'n speler \\'n toernooi laat aansluit.";i['numberOfByesQ']="Hoeveel loslootjies kan \\'n speler kry?";i['numberOfGames']="Aantal spelle";i['numberOfGamesAsManyAsPossible']="Soveel as moontlik wat gespeel kan word binne die toernooi se duur";i['numberOfGamesPreDefined']="Kies vooraf, dieselfde vir alle spelers";i['numberOfRounds']="Aantal rondes";i['numberOfRoundsHelp']="\\'n Onewe aantal rondes voorsien optimale kleur balans.";i['oneRoundEveryXDays']=p({"one":"Een ronde per dag","other":"Een ronde elke %s dae"});i['ongoingGames']=p({"one":"Voordurende spel","other":"Voordurende spelle"});i['otherSystemsA']="Ons is nie van plan om op die oomblik nog toernooi sisteme by Lichess te voeg nie.";i['otherSystemsQ']="Wat van ander toernooie sisteme?";i['pairingsA']=s("Met die %1$s, geïmplementeer deur %2$s, ooreenstemmend met die %3$s.");i['pairingsQ']="Hoe word die indelings bepaal?";i['pairingSystem']="Paring sisteem";i['pairingSystemArena']="Enige beskikbare opponent met \\'n gelyksoortige rang";i['pairingSystemSwiss']="Beste parings gebaseer op punte en gelyk breuke";i['pairingWaitTime']="Paring wag tyd";i['pairingWaitTimeArena']="Vinnig: wag nie vir alle spelers nie";i['pairingWaitTimeSwiss']="Stadig: wag vir alle spelers";i['pause']="Pouse";i['pauseSwiss']="Ja, maar dit mag die aantal rondtes verminder";i['pointsCalculationA']="\\'n Wen is een punt werd, \\'n gelykop is \\'n halwe punt werd, en \\'n verloor is geen punte werd nie. \nWanneer \\'n speler nie kan gepaar word deur \\'n rondte nie, kry hulle \\'n loslootjie terwaarde van een punt.";i['pointsCalculationQ']="Hoe word punte toegeken?";i['possibleButNotConsecutive']="Moontlik, maar nie opeenvolgend nie";i['predefinedDuration']="Vooraf gestelde duur in minute";i['predefinedUsers']="Laat slegs vooraf-gestelde gebruikers om aan te sluit";i['restrictedToTeamsA']="Switserse toernooie is nie ontwerp vir aanlyn skaak nie. Dit verg stiptilikheid, toewyding en gedult van spelers.\nOns dink hierdie voorwaardes is meer waarskynlik binne \\'n span, as in werêlds-toernooie.";i['restrictedToTeamsQ']="Hoekom is dit beperk tot spanne?";i['roundInterval']="Interval tussen rondes";i['roundRobinA']="Ons wil graag dit byvoeg, maar ongelukkig werk rondomtalie toernooie nie aanlyn nie.\nDie rede hiervoor is dat, daar nie \\'n goeie manier is om situasies te hanteer waar mense die toernooi vroeg verlaat nie. Ons kan nie verwag dat alle spelers al hulle spelle sal speel nie, wat veroorsaak dat meeste rondomtalie toernooie foutief en onregverdig sal wees, wat die sisteem se bestaansrede omverwerp. Die naste wat jy aan \\'n rondomtalie toernooi aanlyn kan kry is deur \\'n Switserse toernooi met \\'n hoë aantal rondtes. Dan sal alle moontlike parings voor die einde van die toernooi bereik word.";i['roundRobinQ']="Wat van rondomtalie toernooie?";i['roundsAreStartedManually']="Rondtes word nie automaties begin nie";i['similarToOTB']="Eenders as oor die bord toernooie";i['sonnebornBergerScore']="Sonneborn–Berger telling";i['startingIn']="Begin in";i['startingSoon']="Begin binnekort";i['streaksAndBerserk']="Strepe en Berserk";i['swissDescription']=s("In \\'n switserse toernooi %1$s, elke deelnemer sal nie noodwendig teen alle ander deelnemers speel nie. Deelnemers speel elke rondte een-teen-een en word gepaar deur \\'n stelsel reëls geontwerp om te voorsien dat elke deelnemer teen opponente met \\'n gelyksoortige lopende tellings speel, maar nie teen dieselfde opponent meer as een keer nie. Die wenner is die deelnemer met die hoogste punte versameling gewerf in al die rondtes. Alle deelnemers speel in elke rondte tensy daar \\'n onewe aantal spelers is.");i['swissTournaments']="Switserse toernooie";i['swissVsArenaA']="In \\'n Switserse toernooi, speel alle deelnemers die selfde aantal spelle, en kan sleg eenkeer teen mekaar speel.\nDit kan \\'n goeie opsie wees vir klubs en amptelike toernooie.";i['swissVsArenaQ']="Wanneer om switserse toernooie in plaas van arenas te gebruik?";i['teamOnly']=s("Switserse toernooie kan slegs gemaak word deur span leiers, en kan slegs deur spanlede gespeel word. \n%1$s om in \\'n switserse toernooi te speel.");i['tiebreaksCalculationA']=s("Met die %s.\nTel die tellings van die spelers wat deur die speler gewen is op asook halfte van die spelers se tellings waarteen die speler gelykop gespeel het.");i['tiebreaksCalculationQ']="Hoe word gelykop breuke bepaal?";i['tournDuration']="Duur van die toernooi";i['tournStartDate']="Toernooi se begin datum";i['unlimitedAndFree']="Onbeperk en verniet";i['viewAllXRounds']=p({"one":"Beloer die ronde","other":"Beloer al %s rondes"});i['whatIfOneDoesntPlayA']="Hulle klok sal aantik en hulle sal die spel verloor op tyd.\nDie systeem sal hulle uit die toernooi ontrek, sodat hulle nie meer spelle verloor nie.\nHulle kan die toernooi enige tyd heraansluit.";i['whatIfOneDoesntPlayQ']="Wat gebeur as \\'n speler nie \\'n spel speel nie?";i['willSwissReplaceArenasA']="Nee. Hulle is komplementêre eienskappe.";i['willSwissReplaceArenasQ']="Gaan switserse toernooie, arena toernooie vervang?";i['xMinutesBetweenRounds']=p({"one":"%s minuut tussen rondes","other":"%s minute tussen rondes"});i['xRoundsSwiss']=p({"one":"%s rondte Switsers","other":"%s rondtes Switsers"});i['xSecondsBetweenRounds']=p({"one":"%s sekonde tussen ronde","other":"%s sekondes tussen rondes"})})()