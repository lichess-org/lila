"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.swiss)window.i18n.swiss={};let i=window.i18n.swiss;i['absences']="Poisjäännit";i['byes']="Huilivuorot";i['comparison']="Vertailu";i['durationUnknown']="Kierrosten maksimimäärä on rajattu, mutta kesto avoin";i['dutchSystem']="Hollantilainen järjestelmä";i['earlyDrawsAnswer']="Sveitsiläisen turnauksen peleissä pelaajat eivät voi sopia tasapeliä, ennen kuin 30 siirtoa on pelattu. Vaikka tällä tavoin ei voida estää ennalta järjestettyjä tasapelejä, ainakin spontaani tasapelin sopiminen on hankalampaa.";i['earlyDrawsQ']="Mitä tehdään nopeille tasapeleille?";i['FIDEHandbook']="FIDEn käsikirja";i['forbiddedUsers']="Jos lista ei ole tyhjä, turnaukseen ei voi osallistua kukaan muu kuin listalla luetellut pelaajat. Yksi käyttäjätunnus riviä kohden.";i['forbiddenPairings']="Kielletyt parit";i['forbiddenPairingsHelp']="Määritä pelaajat, joiden ei tule pelata toisiaan vastaan (esim. sisarukset). Kirjoita kullekin riville kaksi käyttäjätunnusta välilyönnillä erotettuna.";i['identicalForbidden']="Ei sallittu";i['identicalPairing']="Saman parin toistuminen";i['joinOrCreateTeam']="Liity joukkueeseen tai luo joukkue";i['lateJoin']="Myöhempi liittyminen";i['lateJoinA']="Voi siihen asti, kunnes yli puolet kierroksista on alkanut. Esimerkiksi 11 kierroksen sveitsiläiseen voi vielä liittyä ennen 6. kierroksen alkua ja 12-kierroksiseen ennen 7. kierroksen alkua.\nMyöhään liittyville merkitään yksi huilaus, vaikka heiltä olisi jäänyt väliin useita kierroksia.";i['lateJoinQ']="Voiko turnaukseen liittyä myöhässä?";i['lateJoinUntil']="Mahdollista, ennen kuin yli puolet kierroksista on alkanut";i['manualPairings']="Peliparien määritys käsin seuraavalla kierroksella";i['manualPairingsHelp']="Määritä seuraavan kierroksen kaikki peliparit käsin. Yksi pelipari riviä kohti. Esimerkki:\nPelaajaA PelaajaB\nPelaajaC PelaajaD\nVoit antaa (1 pisteen arvoisen) huilivuoron pelaajalle lisää seuraavanlaisen rivin:\nPelaajaE 1\nPuuttuvat pelaajat katsotaan poissaoleviksi, ja he saavat nolla pistettä.\nJätä tämä kenttä tyhjäksi, jos haluat Lichessin määrittävän peliparit automaattisesti.";i['moreRoundsThanPlayersA']="Kun kaikki mahdolliset parit ovat pelanneet keskinäisen pelin, turnaus päättyy ja voittaja julistetaan.";i['moreRoundsThanPlayersQ']="Mitä tapahtuu, jos turnauksessa on enemmän kierroksia kuin pelaajia?";i['mustHavePlayedTheirLastSwissGame']="Viimeisin sveitsiläisturnauspeli täytyy olla pelattu";i['mustHavePlayedTheirLastSwissGameHelp']="Anna vain niiden pelaajien liittyä, jotka pelasivat edellisen pelinsä sveitsiläisessä turnauksessa. Jos he eivät tulleet pelaamaan edeltävään sveitsiläiseen turnaukseen, he eivät myöskään pysty liittymään sinun turnaukseesi. Tällä tavoin sveitsiläisten turnausten pelikokemus on parempi niille pelaajille, jotka ilmoittauduttuaan myös tulevat pelaamaan.";i['nbRounds']=p({"one":"%s kierrosta","other":"%s kierrosta"});i['newSwiss']="Uusi sveitsiläinen turnaus";i['nextRound']="Seuraava kierros";i['nowPlaying']="Käynnissä";i['numberOfByesA']="Pelaaja saa yhden pisteen arvoisen huilauksen aina kun parinluontijärjestelmä ei löydä tälle paria.\nLisäksi pelaja saa puolen pisteen arvoisen huilauksen, jos hän liittyy turnaukseen myöhässä.";i['numberOfByesQ']="Kuinka monta kierrosta pelaaja voi huilata?";i['numberOfGames']="Pelien lukumäärä";i['numberOfGamesAsManyAsPossible']="Niin monta kuin peliä kuin rajattuna peliaikana ehtii";i['numberOfGamesPreDefined']="Ennalta määritelty, kaikille pelaajille sama";i['numberOfRounds']="Kierrosten määrä";i['numberOfRoundsHelp']="Parittomalla kierrosten määrällä värit jakautuvat tasapainoisimmin.";i['oneRoundEveryXDays']=p({"one":"Yksi kierros päivässä","other":"Yksi kierros %s päivän välein"});i['ongoingGames']=p({"one":"Meneillään oleva peli","other":"Meneillään olevaa peliä"});i['otherSystemsA']="Emme toistaiseksi aio lisätä Lichessiin enempää turnausjärjestelmiä.";i['otherSystemsQ']="Entä muut turnausjärjestelmät?";i['pairingsA']=s("Käytössä on %1$s, joka on toteuttu %2$sillä %3$sn mukaisesti.");i['pairingsQ']="Miten parit määriytyvät?";i['pairingSystem']="Parien muodostus";i['pairingSystemArena']="Kuka tahansa vastustaja, jonka sijoitus on lähellä omaasi";i['pairingSystemSwiss']="Pisteiden ja vertailupisteiden perusteella sopivin pari";i['pairingWaitTime']="Parin odotusaika";i['pairingWaitTimeArena']="Nopea: ei odota kaikkia pelaajia";i['pairingWaitTimeSwiss']="Hidas: odottaa kaikkia pelaajia";i['pause']="Tauon pitäminen";i['pauseSwiss']="Mahdollista, mutta se voi vähentää kierrosmäärää";i['playYourGames']="Pelaa enemmän pelejä";i['pointsCalculationA']="Voitto on yhden pisteen arvoinen, tasapelistä saa puoli pistettä ja tappiosta nolla pistettä.\nJos pelaajalle ei jollakin kierroksella löydy paria, hän saa yhden pisteen arvoisen huilausvuoron.";i['pointsCalculationQ']="Miten pisteet lasketaan?";i['possibleButNotConsecutive']="Mahdollista, mutta ei peräjälkeen";i['predefinedDuration']="Rajattu kesto minuuteissa";i['predefinedUsers']="Vain ennalta valitut pelaajat saavat osallistua";i['protectionAgainstNoShowA']="Voi syntyä ongelmia, jos sveitsiläiseen turnaukseen ilmoittautuneet pelaajat eivät pelaakaan turnauspelejään.\nOngelmien lievittämiseksi ne pelaajat, jotka jättävät tulematta peliin, eivät tietyn aikaa saa osallistua Lichessissä uusiin sveitsiläisiin turnauksiin.\nSveitsiläisen turnauksen luonut pelaaja voi kuitenkin sallia tällaisten pelaajien osallistumisen turnaukseen.";i['protectionAgainstNoShowQ']="Mitä tehdään, jos osallistuja jättää tulematta peliin?";i['restrictedToTeamsA']="Sveitsiläisiä turnauksia ei ole luotu nettishakkia varten. Ne vaativat pelaajilta täsmällisyyttä, omistautumista ja kärsivällisyyttä. Nämä ehdot täyttyvät mielestämme todennäköisemmin joukkueen sisäisissä kuin avoimissa turnauksissa.";i['restrictedToTeamsQ']="Miksi se on rajattu joukkueen sisäiseksi?";i['roundInterval']="Kierrosten välinen aika";i['roundRobinA']="Haluaisimme lisätä sen, mutta täyskierrosturnaus ei toimi verkossa.\nSiinä ei nimittäin voisi ratkaista reilulla tavalla turnauksen etuajassa jättävistä pelaajista syntyvää ongelmaa. Verkkotapahtumassa ei voi odottaa kaikkien pelaajien pelaavan kaikkia pelejään – niin ei vain tapahdu. Tämän seurauksena useimmat täyskierrosturnaukset olisivat puuttellisia ja epäreiluja, eikä niiden järjestämiselle siten ole perusteita.\nLähimmäksi täyskierrosturnausta pääsee pelaamalla sveitsiläisessä turnauksessa, jossa on hyvin monta kierrosta. Tällöin kaikki mahdolliset parit pääsevät pelaamaan keskenään ennen kuin turnaus loppuu.";i['roundRobinQ']="Entä täyskierrosturnaus?";i['roundsAreStartedManually']="Kierrokset aloitetaan käsin";i['similarToOTB']="Samanlainen kuin lähishakkiturnaukset";i['sonnebornBergerScore']="Sonneborn-Berger-pisteillä";i['startingIn']="Turnauksen alkuun on";i['startingSoon']="Alkaa pian";i['streaksAndBerserk']="Putket ja berserkki";i['swiss']="Sveitsiläinen";i['swissDescription']=s("Sveitsiläisessä turnauksessa %1$s pelaaja ei välttämättä pelaa kaikki muita osallistujia vastaan. Osallistujat pelaavat kullakin kierroksella pareina, jotka muodostetaan tiettyjä sääntöjä noudattaen. Säännöillä varmistetaan, että kaikki pelaavat sellaisia vastustajia vastaan, joilla on jotakuinkin sama pistemäärä, mutta eivät kuitenkaan samaa vastustajaa vastaan useammin kuin kerran. Voittaja on kaikilta kierroksilta korkeimmat yhteispisteet saanut pelaaja. Kaikki osallistujat pelaavat joka kierroksella, paitsi jos pelaajia on pariton määrä.");i['swissTournaments']="Sveitsiläiset turnaukset";i['swissVsArenaA']="Sveitsiläisessä turnauksessa kaikki osallistujat pelaavat saman määrän pelejä ja kohtaavat toisensa korkeintaan kerran.\nSe voi olla hyvä vaihtoehto seurakäyttöön ja virallisiin turnauksiin.";i['swissVsArenaQ']="Milloin kannattaa pelata sveitsiläisiä turnauksia areenaturnauksien sijaan?";i['teamOnly']=s("Sveitsiläisiä turnauksia voivat luoda vain joukkueenjohtajat, ja niissä voivat pelata vain joukkueen jäsenet.           \n%1$s pelataksesi sveitsiläisissä turnauksissa.");i['tieBreak']="Vertailu";i['tiebreaksCalculationA']=s("%s.\nNiissä on laskettu yhteen pelaajan voittamien vastustajien kaikki pisteet sekä puolet pelaajan kanssa tasapelin pelanneiden vastustajien pisteistä.");i['tiebreaksCalculationQ']="Miten paremmuus määriytyy tasatilanteessa?";i['tournDuration']="Turnauksen kesto";i['tournStartDate']="Turnauksen alkamisaika";i['unlimitedAndFree']="Rajoittamaton ja ilmainen";i['viewAllXRounds']=p({"one":"Näytä kierros","other":"Näytä kaikki %s kierrosta"});i['whatIfOneDoesntPlayA']="Aika kuluu pelaajan kellossa, lippu putoaa ja hän häviää pelin.\nSitten järjestelmä poistaa pelaajan turnauksesta, jotta tämä ei häviäisi lisää pelejä.\nPelaaja voi kuitenkin milloin tahansa liittyä turnaukseen uudestaan.";i['whatIfOneDoesntPlayQ']="Mitä tapahtuu, jos pelaaja jättää pelin pelaamatta?";i['willSwissReplaceArenasA']="Ei. Ne täydentävät toisiaan.";i['willSwissReplaceArenasQ']="Korvaako sveitsiläinen turnaus areenaturnaukset?";i['xMinutesBetweenRounds']=p({"one":"%s minuuttia kierrosten välillä","other":"%s minuuttia kierrosten välillä"});i['xRoundsSwiss']=p({"one":"%s kierros sveitsiläisellä järjestelmällä","other":"%s kierrosta sveitsiläisellä järjestelmällä"});i['xSecondsBetweenRounds']=p({"one":"%s sekuntia kierrosten välillä","other":"%s sekuntia kierrosten välillä"})})()