"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.coordinates)window.i18n.coordinates={};let i=window.i18n.coordinates;i['aCoordinateAppears']="Aparece uma coordenada no tabuleiro e deve clicar no quadrado correspondente.";i['aSquareIsHighlightedExplanation']="É destacado um quadrado no tabuleiro e deve introduzir a sua coordenada (ex: \\\"e4\\\").";i['averageScoreAsBlackX']=s("Pontuação média com as pretas: %s");i['averageScoreAsWhiteX']=s("Pontuação média com as brancas: %s");i['coordinates']="Coordenadas";i['coordinateTraining']="Treino de coordenadas";i['findSquare']="Encontrar quadrado";i['goAsLongAsYouWant']="Pode continuar enquanto quiser, não há limite de tempo!";i['knowingTheChessBoard']="Saber as coordenadas do tabuleiro é uma habilidade muito importante no xadrez:";i['mostChessCourses']="A maioria dos cursos e exercícios usam extensamente a notação algébrica.";i['nameSquare']="Designar quadrado";i['showCoordinates']="Mostrar coordenadas";i['showCoordsOnAllSquares']="Coordenadas em cada quadrado";i['showPieces']="Mostrar as peças";i['startTraining']="Começar o treino";i['talkToYourChessFriends']="Torna-se mais fácil falar de xadrez com os teus amigos, já que ambos entendem o \\\"idioma do xadrez\\\".";i['youCanAnalyseAGameMoreEffectively']="Podes analisar um jogo com maior eficiência se conseguires reconhecer as coordenadas rapidamente.";i['youHaveThirtySeconds']="Tem 30 segundos para mapear corretamente o máximo de quadrados possível!"})()