"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.coordinates)window.i18n.coordinates={};let i=window.i18n.coordinates;i['aCoordinateAppears']="Es erscheint eine Koordinate auf dem Brett und du musst auf das entsprechende Feld klicken.";i['aSquareIsHighlightedExplanation']="Ein Feld wird auf dem Brett hervorgehoben und du musst dessen Koordinate eingeben (z. B. „e4“).";i['averageScoreAsBlackX']=s("Durchschnittliche Punktzahl als Schwarz: %s");i['averageScoreAsWhiteX']=s("Durchschnittliche Punktzahl als Weiß: %s");i['coordinates']="Koordinaten";i['coordinateTraining']="Koordinatentraining";i['findSquare']="Feld finden";i['goAsLongAsYouWant']="Spiele so lange du willst, es gibt keine Zeitbegrenzung!";i['knowingTheChessBoard']="Die Koordinaten des Schachbretts zu kennen ist eine wichtige Fähigkeit:";i['mostChessCourses']="Die meisten Schach-Kurse und Übungen verwenden die algebraische Notation.";i['nameSquare']="Feld benennen";i['showCoordinates']="Koordinaten anzeigen";i['showCoordsOnAllSquares']="Koordinaten auf jedem Feld";i['showPieces']="Figuren anzeigen";i['startTraining']="Training beginnen";i['talkToYourChessFriends']="Es macht es einfacher dich mit deinen Schachfreunden zu unterhalten, da ihr beide die \\\"Schachsprache\\\" versteht.";i['youCanAnalyseAGameMoreEffectively']="Du kannst ein Spiel effektiver analysieren, wenn du nicht immer nach den Feldbezeichnungen suchen musst.";i['youHaveThirtySeconds']="Du hast 30 Sekunden, um so viele Felder wie möglich korrekt zu benennen!"})()