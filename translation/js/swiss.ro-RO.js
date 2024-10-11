"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.swiss)window.i18n.swiss={};let i=window.i18n.swiss;i['absences']="Absențe";i['byes']="Byes";i['comparison']="Comparaţie";i['durationUnknown']="Număr maxim de runde predefinit, dar durata nu este cunoscută";i['dutchSystem']="sistemul olandez";i['earlyDrawsAnswer']="În sistemul elvețian, jucătorii nu pot face remiză înainte de a juca 30 de mutări. Deși această măsură nu poate preveni remizele prestabilite, este puțin mai dificil să se convină asupra unei remize în timpul partidei.";i['earlyDrawsQ']="Ce se întâmplă cu remizele timpurii?";i['FIDEHandbook']="manualul FIDE";i['forbiddedUsers']="Dacă această listă nu este goală, atunci utilizatorii care nu apar în această listă nu vor putea să se alăture. Un nume de utilizator pe linie.";i['forbiddenPairings']="Asocieri interzise";i['forbiddenPairingsHelp']="Numele de utilizator ale jucătorilor care nu trebuie să se joace împreună (frați, de exemplu). Două nume de utilizator pe linie, separate de un spațiu.";i['identicalForbidden']="Interzis";i['identicalPairing']="Asociere repetata (daca jucati cu aceeasi persoana de mai multe ori)";i['joinOrCreateTeam']="Alătură-te sau creează o echipă";i['lateJoin']="Aderare tardivă";i['lateJoinA']="Da, până la începerea a mai mult de jumătate din runde; De exemplu, într-un turneu elvețian cu 11 runde jucătorii se pot alătura înainte ca runda 6 să înceapă, iar într-un turneu cu 12 runde jucătorii se pot alătura înainte de a începe runda 7.\nCei care s-au alăturat cu întârziere primesc o singură pauză, chiar dacă au ratat mai multe runde.";i['lateJoinQ']="Pot jucătorii să se alăture cu întârziere?";i['lateJoinUntil']="Da, însă doar înainte să înceapă mai mult de jumătate din numărul rundelor";i['manualPairings']="Asocieri manuale în runda următoare";i['manualPairingsHelp']="Specificați manual toate perechile următoarei runde. Asocierea unui jucător pe linie. Exemplu:\nPlayerA PlayerB\nPlayerC PlayerD\nPentru a da o adiere (1 punct) unui jucător în loc de o pereche, adaugă o linie ca aceasta:\nJucător 1\nJucătorii lipsă vor fi considerați absenți și vor primi zero puncte.\nLăsați acest câmp gol pentru a lăsa lichess să creeze perechi automat.";i['moreRoundsThanPlayersA']="După ce vor fi jucate toate perechile posibile, turneul va fi încheiat şi va fi declarat un câştigător.";i['moreRoundsThanPlayersQ']="Ce se întâmplă dacă turneul are mai multe runde decât numărul jucătorilor?";i['mustHavePlayedTheirLastSwissGame']="Trebuie să fi jucat ultimul lor joc de elvețian";i['mustHavePlayedTheirLastSwissGameHelp']="Lăsați jucătorii să se alăture dacă au jucat ultimul lor joc de elvețian. Dacă nu au reușit să apară într-un eveniment recent de tip swiss nu vor putea intra pe al tău. Asta are ca rezultat o experiență mai bună pentru jucătorii care apar de fapt.";i['nbRounds']=p({"one":"%s rundă","few":"%s runde","other":"%s de runde"});i['newSwiss']="Turneu Elvețian nou";i['nextRound']="Runda următoare";i['nowPlaying']="Acum se joacă";i['numberOfByesA']="Un jucător primește un \\\"bye\\\" de un punct de fiecare dată când sistemul de asociere nu ii găsește un adversar.\nÎn plus, un singur \\\"bye\\\" de jumătate de punct este atribuit atunci când un jucător se alătură unui turneu mai târziu.";i['numberOfByesQ']="Câte \\\"bye\\\" poate primi un jucător?";i['numberOfGames']="Număr de partide";i['numberOfGamesAsManyAsPossible']="Cât de multe pot fi jucate pe durata atribuită";i['numberOfGamesPreDefined']="Decisă în avans, la fel pentru toți jucătorii";i['numberOfRounds']="Număr de runde";i['numberOfRoundsHelp']="Un număr impar de runde permite un echilibru optim de culori.";i['oneRoundEveryXDays']=p({"one":"O rundă pe zi","few":"O rundă la fiecare %s zile","other":"O rundă la fiecare %s de zile"});i['ongoingGames']=p({"one":"Joc în desfășurare","few":"Jocuri în desfășurare","other":"Jocuri în desfășurare"});i['otherSystemsA']="Nu intenţionăm să adăugăm mai multe tipuri de turnee la Lichess în acest moment.";i['otherSystemsQ']="Cum rămâne cu alte sisteme de turnee?";i['pairingsA']=s("Cu %1$s, implementat de %2$s, în conformitate cu %3$s.");i['pairingsQ']="Cum sunt decise perechile?";i['pairingSystem']="Sistem de asociere";i['pairingSystemArena']="Orice oponent disponibil cu rang similar";i['pairingSystemSwiss']="Cea mai buna asociere bazata pe numarul de puncte si a criteriului de departajare";i['pairingWaitTime']="Timp de aşteptare pentru asociere";i['pairingWaitTimeArena']="Rapid: nu așteaptă toți jucătorii";i['pairingWaitTimeSwiss']="Lent: așteaptă toți jucătorii";i['pause']="Pauză";i['pauseSwiss']="Da, dar ar putea reduce numărul de runde";i['playYourGames']="Joacă jocurile tale";i['pointsCalculationA']="O victorie valorează un punct, o remiză este o jumătate de punct, iar o pierdere zero puncte.\nAtunci când un jucător nu poate fi împerecheat în timpul unei runde, acesta primește o compensație în valoare de un punct.";i['pointsCalculationQ']="Cum sunt calculate punctele?";i['possibleButNotConsecutive']="Posibil, dar nu consecutiv";i['predefinedDuration']="Durată predefinită în minute";i['predefinedUsers']="Permite doar utilizatorilor predefiniți să se alăture";i['protectionAgainstNoShowA']="Jucătorii care se înscriu pentru evenimente elvețiene dar nu își joacă jocurile pot fi problematici.\nPentru a atenua această problemă, Lichess îi împiedică pe jucătorii care nu au reușit să joace un joc să se alăture unui nou eveniment elvețian pentru o anumită perioadă de timp.\nCreatorul unui eveniment elvețian poate decide oricum să se alăture evenimentului.";i['protectionAgainstNoShowQ']="Ce se face cu privire la ne-prezentări?";i['restrictedToTeamsA']="Turneurile elvețiene nu au fost proiectate pentru șah online. Ele necesită punctualitate, dedicare și răbdare din partea jucătorilor.\nConsiderăm că este mai probabil ca aceste condiții să fie îndeplinite în cadrul unei echipe decât în turneele globale.";i['restrictedToTeamsQ']="De ce este limitat la echipe?";i['roundInterval']="Interval între runde";i['roundRobinA']="Am dori să o adăugăm, dar din păcate Round Robin nu funcționează bine online.\nMotivul este că nu are un mod corect de a trata persoanele care părăsesc turneul mai devreme. Nu ne putem aștepta ca toți jucătorii să își joace toate jocurile într-un eveniment online. Pur și simplu, acest lucru nu se va întâmpla şi, ca urmare, majoritatea turneelor de tip Round Robin ar fi nedrepte, ceea ce ar fi contrar scopului pentru care a fost creat turneul.\nCel mai apropiat mod de a ajunge la Round Robin online este să joci un turneu elvețian cu un număr foarte mare de runde. Astfel vor fi jucate toate perechile posibile înainte de terminarea turneului.";i['roundRobinQ']="Ce se întâmplă cu Round Robin?";i['roundsAreStartedManually']="Rundele sunt pornite manual";i['similarToOTB']="Similar turneelor oficiale";i['sonnebornBergerScore']="scorul Sonneborn–Berger";i['startingIn']="Începe în";i['startingSoon']="Începe în curând";i['streaksAndBerserk']="Serii de victorii si Berserk";i['swiss']="Elvețian";i['swissDescription']=s("Într-un turneu elvețian %1$s, fiecare concurent nu joacă neapărat cu toți ceilalți inscrisi. Concurenții se întâlnesc unu la unu în fiecare rundă și sunt asociați utilizând un set de reguli concepute pentru a se asigura că fiecare concurent joacă adversari cu un scor similar, dar nu cu același adversar de mai multe ori. Câștigătorul este concurentul cu cele mai multe puncte câștigate cumulat în toate rundele. Toți concurenții joacă în fiecare rundă, cu excepția cazului în care există un număr impar de jucători.”");i['swissTournaments']="Turnee Swiss";i['swissVsArenaA']="Într-un turneu elvetian toţi participanţii joacă acelaşi număr de partide şi doi adversari se intalnesc o singură dată.\nPoate fi o opțiune bună pentru cluburi și turnee oficiale.";i['swissVsArenaQ']="Când folosim turnee elvetiene în loc de arene?";i['teamOnly']=s("Turneurile elvețiene pot fi create doar de liderii de echipe și pot fi jucate doar de membrii unei echipe.           \n%1$s pentru a începe să joci într-un turneu elvețian.");i['tieBreak']="Tie Break";i['tiebreaksCalculationA']=s("Cu %s.\nAdaugă scorurile fiecărui adversar pe care jucătorul îl învinge și jumătate din scorul fiecărui adversar cu care jucătorul face remiză.");i['tiebreaksCalculationQ']="Cum se calculează jocurile decisive?";i['tournDuration']="Durata turneului";i['tournStartDate']="Data de începere a turneului";i['unlimitedAndFree']="Nelimitat și gratuit";i['viewAllXRounds']=p({"one":"Vezi runda","few":"Vezi toate cele %s runde","other":"Vezi toate cele %s de runde"});i['whatIfOneDoesntPlayA']="Ceasul va porni si va pierde jocul din cauza timpului.\nApoi sistemul va retrage jucătorul din turneu, astfel încât să nu piardă mai multe partide.\n Se pot alătura din nou turneului în orice moment.";i['whatIfOneDoesntPlayQ']="Ce se întâmplă dacă un jucător nu joacă un joc?";i['willSwissReplaceArenasA']="Nu. Sunt turnee cu caracteristici complementare.";i['willSwissReplaceArenasQ']="Turneele elvețiene vor inlocui turneele de tip arenă?";i['xMinutesBetweenRounds']=p({"one":"%s minut între runde","few":"%s minute între runde","other":"%s de minute între runde"});i['xRoundsSwiss']=p({"one":"Sistem elvețian %s rundă","few":"Sistem elvețian %s runde","other":"Sistem elvețian %s de runde"});i['xSecondsBetweenRounds']=p({"one":"%s secundă între runde","few":"%s secunde între runde","other":"%s de secunde între runde"})})()