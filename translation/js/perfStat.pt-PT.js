"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.perfStat)window.i18n.perfStat={};let i=window.i18n.perfStat;i['averageOpponent']="Pontuação média dos adversários";i['berserkedGames']="Partidas no modo frenético";i['bestRated']="Melhores vitórias a pontuar";i['currentStreak']=s("Sequência atual: %s");i['defeats']="Derrotas";i['disconnections']="Desconexões";i['fromXToY']=s("de %1$s a %2$s");i['gamesInARow']="Partidas jogadas de seguida";i['highestRating']=s("Pontuação mais alta: %s");i['lessThanOneHour']="Menos de uma hora entre partidas";i['longestStreak']=s("Sequência mais longa: %s");i['losingStreak']="Derrotas consecutivas";i['lowestRating']=s("Pontuação mais baixa: %s");i['maxTimePlaying']="Tempo máximo passado a jogar";i['notEnoughGames']="Não foram jogadas partidas suficientes";i['notEnoughRatedGames']="Não foi jogado um número suficiente de partidas a pontuar para estabelecer uma pontuação de confiança.";i['now']="agora";i['perfStats']=s("Estatísticas de %s");i['progressOverLastXGames']=s("Progresso nas últimas %s partidas:");i['provisional']="provisório";i['ratedGames']="Total de partidas a pontuar";i['ratingDeviation']=s("Desvio da pontuação: %s.");i['ratingDeviationTooltip']=s("Um valor inferior significa que a classificação é mais estável. Acima de %1$s, a classificação é considerada provisória. Para ser incluído nas classificações, esse valor deve estar abaixo de %2$s (xadrez padrão) ou %3$s (variantes).");i['timeSpentPlaying']="Tempo passado a jogar";i['totalGames']="Total de partidas";i['tournamentGames']="Partidas em torneios";i['victories']="Vitórias";i['viewTheGames']="Ver as partidas";i['winningStreak']="Vitórias consecutivas"})()