"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.perfStat)window.i18n.perfStat={};let i=window.i18n.perfStat;i['averageOpponent']="Aurkarien batazbestekoa";i['berserkedGames']="Berserk erabilitako partidak";i['bestRated']="Garaipen onenak";i['currentStreak']=s("Uneko segida: %s");i['defeats']="Porrotak";i['disconnections']="Deskonektatutakoak";i['fromXToY']=s("%1$s - %2$s");i['gamesInARow']="Jarraian jokatutako partida kopurua";i['highestRating']=s("Puntuazio altuena: %s");i['lessThanOneHour']="Partiden artean ordubete baino gutxiago";i['longestStreak']=s("Segida luzeena: %s");i['losingStreak']="Porroten segida";i['lowestRating']=s("Puntuazio baxuena: %s");i['maxTimePlaying']="Jokatzen emandako denbora gehiena";i['notEnoughGames']="Ez duzu partida nahiko jokatu";i['notEnoughRatedGames']="Ez duzu puntuaziorako balio duten behar adina partida jokatu.";i['now']="orain";i['perfStats']=s("%s estatistikak");i['progressOverLastXGames']=s("Azken %s partidetako aurrerapena:");i['provisional']="behin-behinekoa";i['ratedGames']="Puntuaziorako balio duten partidak";i['ratingDeviation']=s("Puntuazioaren desbideraketa: %s.");i['ratingDeviationTooltip']=s("Balio baxuagoak puntuazioa egonkorragoa dela esan nahi du. %1$stik gorako balioak puntuazioa behin-behinekoa dela esan nahi du. Sailkapenetan agertzeko, balio hori %2$s baino baxuagoa (xake estandarrean) eta %3$s baino baxuagoa (aldaeretan) izan behar da.");i['timeSpentPlaying']="Jokatzen emandako denbora";i['totalGames']="Partida kopurua";i['tournamentGames']="Txapelketetako partidak";i['victories']="Garaipenak";i['viewTheGames']="Partidak ikusi";i['winningStreak']="Garaipenen segida"})()