"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.voiceCommands)window.i18n.voiceCommands={};let i=window.i18n.voiceCommands;i['cancelTimerOrDenyARequest']="Nuligi tempumilon aŭ rifuzi peton";i['castle']="Aroku (ajnflanke)";i['instructions1']=s("Uzu la %1$s butonon por baskuligi voĉan rekonon, la %2$s butonon por malfermi ĉi tiun dialogon, kaj la %3$s menuon por ŝanĝi parolagordoj.");i['instructions2']="Ni montras sagojn por oblaj movoj, kiam ni ne estas certa. Parolu la koloron aŭ nombron de movsago por elkti ĝin.";i['instructions3']=s("Se sago montras svingantan radaron, tiu movo ludos, kiam la cirklon estas finiĝa. Dum tiam, vi povas nur %1$s por tuj ludi tiun movon, %2$s por nuligi, aŭ paroli la koloron/numbron de malsama sago. Tiu tempumilo povas esti ĝustigita aŭ malŝaltita en la agordoj.");i['instructions4']=s("Ebligu %s en brua ĉirkaŭaĵo. Premi majuskligon dum parolado de komandoj kiam tiu ĉi estas ebligita.");i['instructions5']="Uzu la fonetikan abocon por plibonigi rekonon de ŝaktabulaj dosieroj.";i['instructions6']=s("%s klarigas la voĉmovajn agordojn detale.");i['moveToE4OrSelectE4Piece']="Movu al e4 aŭ elektu la e4 pecon";i['phoneticAlphabetIsBest']="Fonetika aboco estas plejbona";i['playPreferredMoveOrConfirmSomething']="Ludi preferan movon aŭ konfirmi ion";i['selectOrCaptureABishop']="Elektu au kapu kurieron";i['showPuzzleSolution']="Montri puzlan solvon";i['sleep']="Dormi (se vekigan vorton ebligas)";i['takeRookWithQueen']="Kaptu la turon per damo";i['thisBlogPost']="Ĉi tiun blogan afiŝon";i['turnOffVoiceRecognition']="Malŝalti voĉrekonon";i['voiceCommands']="Voĉaj komandoj";i['watchTheVideoTutorial']="Spektu la videan lernilon"})()