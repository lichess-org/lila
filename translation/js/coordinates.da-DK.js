"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.coordinates)window.i18n.coordinates={};let i=window.i18n.coordinates;i['aCoordinateAppears']="Et koordinat vises på brættet, og du skal klikke på det tilsvarende felt.";i['aSquareIsHighlightedExplanation']="Et felt fremhæves på brættet, og du skal indtaste dets koordinat (f.eks. \\\"e4\\\").";i['averageScoreAsBlackX']=s("Gennemsnitlig score som sort: %s");i['averageScoreAsWhiteX']=s("Gennemsnitlig score som hvid: %s");i['coordinates']="Koordinater";i['coordinateTraining']="Koordinattræning";i['findSquare']="Find felt";i['goAsLongAsYouWant']="Fortsæt så længe du ønsker, der er ingen tidsbegrænsning!";i['knowingTheChessBoard']="Det er vigtigt at kende koordinaterne på skakbrættet:";i['mostChessCourses']="De fleste skakkurser og øvelser bruger i vid udstrækning den algebraiske notation.";i['nameSquare']="Navngiv felt";i['showCoordinates']="Vis koordinater";i['showCoordsOnAllSquares']="Koordinater på hvert felt";i['showPieces']="Vis brikker";i['startTraining']="Start træning";i['talkToYourChessFriends']="Det gør det lettere at snakke med dine skakvenner, når I begge forstår \\\"skakkens sprog\\\".";i['youCanAnalyseAGameMoreEffectively']="Du kan analysere et parti mere effektivt, hvis du ikke skal lede efter feltnavne.";i['youHaveThirtySeconds']="Du har 30 sekunder til at kortlægge så mange felter som muligt!"})()