"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.search)window.i18n.search={};let i=window.i18n.search;i['advancedSearch']="Búsqueda avanzada";i['aiLevel']="Nivel I.A";i['analysis']="Análisis";i['ascending']="Ascendente";i['color']="Color";i['date']="Fecha";i['descending']="Descendente";i['evaluation']="Evaluación";i['from']="Desde";i['gamesFound']=p({"one":"%s partida encontrada","other":"%s partidas encontradas"});i['humanOrComputer']="Si el oponente era humano o un ordenador";i['include']="Incluir";i['loser']="Perdedor";i['maxNumber']="Número máximo";i['maxNumberExplanation']="El número máximo de partidas que se mostrarán en los resultados";i['nbTurns']="Cantidad de turnos";i['onlyAnalysed']="Solo partidas que tengan un análisis por ordenador disponible";i['opponentName']="Nombre del oponente";i['ratingExplanation']="La puntuación media de ambos jugadores";i['result']="Resultado";i['search']="Búsqueda";i['searchInXGames']=p({"one":"Busca en %s partida","other":"Busca en %s partidas"});i['sortBy']="Ordenar por";i['source']="Origen";i['to']="Hasta";i['winnerColor']="Color del ganador";i['xGamesFound']=p({"one":"Se ha encontrado %s partida","other":"Se han encontrado %s partidas"})})()