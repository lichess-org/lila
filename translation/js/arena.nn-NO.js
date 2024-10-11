"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.arena)window.i18n.arena={};let i=window.i18n.arena;i['allAveragesAreX']=s("Alle snittverdiar på denne sida er %s.");i['allowBerserk']="Tillat berserk";i['allowBerserkHelp']="Lat spelarane halvere tida si på klokka for eit ekstra poeng";i['allowChatHelp']="Lat spelarane diskutera i samtalerom";i['arena']="Arena";i['arenaStreaks']="Arena-vinstrekkjer";i['arenaStreaksHelp']="Etter to sigrar gjev seinara sigrar fire poeng i staden for to.";i['arenaTournaments']="Arena-turneringar";i['averagePerformance']="Gjennomsnittleg prestasjon";i['averageScore']="Gjennomsnittleg poengsum";i['berserk']="Arena Berserk";i['berserkAnswer']="Om ein spelar klikkar på Berserk-knappen når spelet startar, vert klokketida halvert, men ein eventuell siger gjev eitt ekstra turneringspoeng. For tidskontrollar med tidstillegg vil det å gå Berserk dessutan innebere at ein tapar tidstillegget. \n(Unntak for 1+2, det gir 1+0)\n\nDet er ikkje mogleg å gå Berserk i part med null i starttid (0+1, 0+2).\n\nBerserk gjev eitt ekstrapoeng berre om du spelar minst 7 trekk i partiet.";i['bestResults']="Beste resultat";i['created']="Oppretta";i['customStartDate']="Eigendefinert startdato";i['customStartDateHelp']="I din tidssone. Dette overstyrar innstillinga «Tid før turnering startar»";i['defender']="Forsvarar";i['drawingWithinNbMoves']=p({"one":"Dersom det blir remis innan første %s trekk vil ingen av spelarane få poeng.","other":"Ved remis innan dei første %s trekka vil ingen av spelarane få poeng."});i['drawStreakStandard']=s("Remisrekke: ein spelar med ei ubroten rekke parti som endar remis i ein arena får berre poeng for den første remisen eller ved remisar etter meir enn%s trekk. Ei remisrekke kan berre brytas av ein siger, ikkje av eit tap eller ein remis.");i['drawStreakVariants']="Antal trekk som ein må opp i for å få poeng i parti som endar i remis avheng av kva variant det er snakk om. Tabellen nedanfor syner grenseverdiane for kvar variant.";i['editTeamBattle']="Rediger lagkamp";i['editTournament']="Rediger turnering";i['history']="Arena-historikk";i['howAreScoresCalculated']="Korleis vert poenga rekna ut?";i['howAreScoresCalculatedAnswer']="Ein siger gjev to basispoeng, ein remis eitt poeng og eit tap null poeng. Vinn du to parti på rad vert det starta ein dobbelpoeng-serie, det kjem då fram ein flamme som ikon. Partia som følgjer er då verde doble poeng heilt til du mislukkast i å vinne eit parti. Dette vil seie at ein siger er verd fire poeng, ein remis to poeng og eit tap null poeng.\n\nTil dømes gjev to sigrar med ein remis etterpå seks poeng: 2 + 2 + (2 *1)";i['howDoesItEnd']="Korleis vert turneringa avgjort?";i['howDoesItEndAnswer']="Turneringa har ei nedteljingsklokke. Turneringsrangeringa vert fryst og vinnaren utropa når nedteljongsklokka er nede i null. Parti som framleis er i spel må spelast til endes, men dei reknast ikkje med i turneringa.";i['howDoesPairingWork']="Korleis vert turneringspara sett saman?";i['howDoesPairingWorkAnswer']="Når turneringa startar er spelarane sett saman på grunnlag av ratinga deira. Gå attende til turneringslobbyen straks du er ferdig med eit spel, og du vert då sett saman med ein spelar nær rangeringa di på turneringsstigen. Dette sikrar minimal ventetid, men du møter ikkje nødvendigvis alle dei andre spelarane i turneringa. Spel snøgt og gå attende til lobbyen for å spela fleire parti og vinna fleire poeng.";i['howIsTheWinnerDecided']="Korleis avgjer ein kven som vinn?";i['howIsTheWinnerDecidedAnswer']="Spelaren eller spelarane med flest poeng ved slutten av turneringsfristen vil verta utropt som vinnar(ar).";i['isItRated']="Er det rangert?";i['isNotRated']="Turneringa er *ikkje* rangert og vil *ikkje* påverke ratinga di.";i['isRated']="Denne turnering er rangert og vil påverke ratinga di.";i['medians']="medianar";i['minimumGameLength']="Minste partilengd";i['myTournaments']="Mine turneringar";i['newTeamBattle']="Ny lagkamp";i['noArenaStreaks']="Ingen arena-strøymingar";i['noBerserkAllowed']="Berserk er ikkje tillate";i['onlyTitled']="Berre spelarar med sjakktittel";i['onlyTitledHelp']="Berre spelarar med offisielle sjakktitlar kan delta i turneringa";i['otherRules']="Andre viktige reglar";i['pickYourTeam']="Vel ut laget ditt";i['pointsAvg']="Poengsnitt";i['pointsSum']="Poengsum";i['rankAvg']="Snittplassering";i['rankAvgHelp']="Plasseringssnittet er eit prosentverde av plasseringa di. Jo lågare, dess betre.\n\nTil dømes vil ein tredjeplass i ei turnering med 100 deltakarar gje plasseringssnittet 3%. Tiandeplass i ei turnering med 1000 deltakarar = 1%.";i['recentlyPlayed']="Nett spela";i['shareUrl']=s("Gje andre spelarar denne URL-en så dei kan bli med: %s");i['someRated']="Nokre av turneringane er rangerte og vil påverke ratingen din.";i['stats']="Statistikk";i['thereIsACountdown']="Der er ei nedteljing på det første trekket ditt. Du tapar partiet om du ikkje trekk i tide.";i['thisIsPrivate']="Dette er ein privat turnering";i['total']="Totalt";i['tournamentShields']="Turneringsskjold";i['tournamentStats']="Turneringsstatistikk";i['tournamentWinners']="Turneringsvinnarar";i['variant']="Variant";i['viewAllXTeams']=p({"one":"Vis laget","other":"Vis alle %s lag"});i['whichTeamWillYouRepresentInThisBattle']="Kva lag vil du spela på i denne kampen?";i['willBeNotified']="Du blir varsla når turneringa startar. Det er difor mogleg å bruke ventetida til å spela i ei anna fane.";i['youMustJoinOneOfTheseTeamsToParticipate']="For å delta må du bli med i eit av desse laga!"})()