"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.perfStat)window.i18n.perfStat={};let i=window.i18n.perfStat;i['averageOpponent']="Prosječni protivnik";i['berserkedGames']="Berserk partije";i['bestRated']="Pobjede protiv protivnika visokog rejtinga";i['currentStreak']=s("Trenutni niz: %s");i['defeats']="Porazi";i['disconnections']="Prekinute partije";i['fromXToY']=s("od %1$s do %2$s");i['gamesInARow']="Broj odigranih partija u nizu";i['highestRating']=s("Najveći rejting: %s");i['lessThanOneHour']="Manje od jednog sata između partija";i['longestStreak']=s("Najduži niz: %s");i['losingStreak']="Gubitnički Niz";i['lowestRating']=s("Najniži rejting: %s");i['maxTimePlaying']="Maksimalno vrijeme provedeno igrajući";i['notEnoughGames']="Nedovoljno partija odigrano";i['notEnoughRatedGames']="Nije odigrano dovoljno partija za retjing bodove da bi se uspostavio pouzdani rejting.";i['now']="sada";i['perfStats']=s("%s statistika");i['progressOverLastXGames']=s("Napredak u poslednjih %s partija:");i['provisional']="privremen";i['ratedGames']="Partije za rejting bodove";i['ratingDeviation']=s("Rejting devijacija: %s.");i['ratingDeviationTooltip']=s("Niža vrijednost znači da je rejting stabilniji. Pri vrijednostima višim od %1$s za rejting se smatra da je privremen. Za uvrštavanje na rang-liste ova vrijednost trebala bi biti niža od %2$s (u običnom šahu) ili od %3$s (u drugim varijantama šaha).");i['timeSpentPlaying']="Vrijeme provedeno igrajući";i['totalGames']="Ukupno partija";i['tournamentGames']="Turnirske partije";i['victories']="Pobjede";i['viewTheGames']="Pregledajte partije";i['winningStreak']="Pobjednički Niz"})()