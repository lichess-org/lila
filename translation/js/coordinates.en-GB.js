"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.coordinates)window.i18n.coordinates={};let i=window.i18n.coordinates;i['aCoordinateAppears']="A coordinate appears on the board and you must click on the corresponding square.";i['aSquareIsHighlightedExplanation']="A square is highlighted on the board and you must enter its coordinate (e.g. \"e4\").";i['averageScoreAsBlackX']=s("Average score as black: %s");i['averageScoreAsWhiteX']=s("Average score as white: %s");i['coordinates']="Coordinates";i['coordinateTraining']="Coordinate training";i['findSquare']="Find square";i['goAsLongAsYouWant']="Go as long as you want, there is no time limit!";i['knowingTheChessBoard']="Knowing the chessboard coordinates is a very important skill for several reasons:";i['mostChessCourses']="Most chess courses and exercises use the algebraic notation extensively.";i['nameSquare']="Name square";i['showCoordinates']="Show coordinates";i['showCoordsOnAllSquares']="Coordinates on every square";i['showPieces']="Show pieces";i['startTraining']="Start training";i['talkToYourChessFriends']="It makes it easier to talk to your chess friends, since you both understand the 'language of chess'.";i['youCanAnalyseAGameMoreEffectively']="You can analyse a game more effectively if you can quickly recognise coordinates.";i['youHaveThirtySeconds']="You have 30 seconds to correctly map as many squares as possible!"})()