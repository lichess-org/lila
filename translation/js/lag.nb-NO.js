"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.lag)window.i18n.lag={};let i=window.i18n.lag;i['andNowTheLongAnswerLagComposedOfTwoValues']="Og nå, det lange svaret! Spillforsinkelse er sammensatt av to uavhengige verdier (lavere er bedre):";i['isLichessLagging']="Lagger Lichess?";i['lagCompensation']="Kompensasjon for forsinkelse";i['lagCompensationExplanation']="Lichess kompenserer for nettverksforsinkelse. Dette omfatter vedvarende forsinkelser og tilfeldige topper. Det fins grenser og heuristikk basert på tidskontrollen og oppnådd kompensasjon slik at resultatet blir rimelig for begge spillerne. Dermed er det ikke et handikap å ha høyere nettverksforsinkelse enn motspilleren din!";i['lichessServerLatency']="Latens for Lichess-serveren";i['lichessServerLatencyExplanation']="Tiden det tar å behandle et trekk på serveren. Den er den samme for alle og avhenger bare av serverbelastningen. Jo flere spillere, dess høyere blir den, men Lichess-utviklerne gjør sitt ytterste for å holde den nede. Den overstiger sjelden 10 ms.";i['measurementInProgressThreeDot']="Målinger pågår...";i['networkBetweenLichessAndYou']="Nettverket mellom Lichess og deg";i['networkBetweenLichessAndYouExplanation']="Tiden det tar å sende et trekk fra datamaskinen din til Lichess-serveren og få svaret tilbake. Den er spesifikk for din avstand til Lichess (Frankrike) og for kvaliteten på din internettforbindelse. Lichess-utviklerne kan ikke fikse wifien din eller øke lysets hastighet.";i['noAndYourNetworkIsBad']="Nei. Og nettverket ditt er dårlig.";i['noAndYourNetworkIsGood']="Nei. Og nettverket ditt er bra.";i['yesItWillBeFixedSoon']="Ja. Det vil bli fikset snart!";i['youCanFindTheseValuesAtAnyTimeByClickingOnYourUsername']="Du kan finne begge disse verdiene når som helst ved å klikke på brukernavnet ditt øverst på siden."})()