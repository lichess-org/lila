"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.coordinates)window.i18n.coordinates={};let i=window.i18n.coordinates;i['aCoordinateAppears']="Koordinata se pojavi na ploči a ti moraš kliknuti na odgovarajuće polje.";i['aSquareIsHighlightedExplanation']="Polje je istaknuto na ploči a ti moraš unjeti odgovarajuće koordinate (npr. \\\"e4\\\").";i['averageScoreAsBlackX']=s("Prosječan rezultat kao crni: %s");i['averageScoreAsWhiteX']=s("Prosječan rezultat kao bijeli: %s");i['coordinates']="Koordinate";i['coordinateTraining']="Trening koordinata";i['findSquare']="Pronađi polje";i['goAsLongAsYouWant']="Opustite se, nema vremenskog ograničenja!";i['knowingTheChessBoard']="Znanje šahovskih koordinata je jako važna šahovska vještina:";i['mostChessCourses']="Većina šahovskih tečajeva i vježbi opsežno koristi algebarsku notaciju.";i['nameSquare']="Imenuj polje";i['showCoordinates']="Prikaži koordinate";i['showPieces']="Prikaži figure";i['startTraining']="Započni trening";i['talkToYourChessFriends']="Jednostavnije je razgovarati sa svojim šahovskim prijateljima, jer oboje razumijete \\\"šahovski jezik\\\".";i['youCanAnalyseAGameMoreEffectively']="Možeš učinkovitije analizirati partiju ako ne trebaš tražiti koordinate pojedinih polja.";i['youHaveThirtySeconds']="Imaš 30 sekundi da ispravno obilježiš što više polja!"})()