"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.voiceCommands)window.i18n.voiceCommands={};let i=window.i18n.voiceCommands;i['cancelTimerOrDenyARequest']="Atšaukti laikmatį arba atmesti prašymą";i['castle']="Rokiruotė (į bet kurią pusę)";i['moveToE4OrSelectE4Piece']="Eiti į e4 arba pasirinkti figūrą e4 langelyje";i['phoneticAlphabetIsBest']="Geriausia veikia fonetinė abėcėlė";i['playPreferredMoveOrConfirmSomething']="Žaisti norimą ėjimą arba ką nors patvirtinti";i['selectOrCaptureABishop']="Pasirinkti arba kirsti rikį";i['showPuzzleSolution']="Rodyti galvosūkio sprendimą";i['sleep']="Užmigdyti (jei nurodytas pažadinimo žodis)";i['takeRookWithQueen']="Valdove kirsti bokštą";i['thisBlogPost']="Šis tinklaraščio įrašas";i['turnOffVoiceRecognition']="Išjungti balso atpažinimą"})()