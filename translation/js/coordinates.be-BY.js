"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.coordinates)window.i18n.coordinates={};let i=window.i18n.coordinates;i['aCoordinateAppears']="На дошцы з\\'яўляецца каардыната, і вы павінны націснуць на адпаведную клетку.";i['aSquareIsHighlightedExplanation']="На дошцы падсвечваецца клетка, і вы павінны ўвесці яе каардынату (напрыклад, \\\"e4\\\").";i['averageScoreAsBlackX']=s("Сярэдні вынік за чорных: %s");i['averageScoreAsWhiteX']=s("Сярэдні вынік за белых: %s");i['coordinates']="Каардынаты";i['coordinateTraining']="Трэніроўка каардынат";i['findSquare']="Знайдзіце клетку";i['goAsLongAsYouWant']="Спрабуйце так доўга, як хочаце, час неабмежаваны!";i['knowingTheChessBoard']="Ведаць каардынаты шахматнай дошкі - вельмі важны шахматны навык:";i['mostChessCourses']="Большасць шахматных задач та заданняў выкарыстоўваць шахматнаю натацыю.";i['nameSquare']="Назавіце клетку";i['showCoordinates']="Паказваць каардынаты";i['showPieces']="Паказваць фігуры";i['startTraining']="Пачаць трэніроўку";i['talkToYourChessFriends']="Гэта спрошчвае гутарку з сябрамі-шахматыстамі, бо вы абодва разумееце \\\"шахматную мову\\\".";i['youCanAnalyseAGameMoreEffectively']="Вы можаце больш эфектыўна аналізаваць гульні, калі вы ведаеце дзе кожная клетка знаходзіцца.";i['youHaveThirtySeconds']="У вас ёсць 30 секунд, каб правільна суаднесці як мага больш клетак!"})()