"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.coach)window.i18n.coach={};let i=window.i18n.coach;i['aboutMe']="O mne";i['accepting']="Prijíma študentov";i['areYouCoach']=s("Ste skvelým šachovým trénerom s %s?");i['availability']="Dostupnosť";i['bestSkills']="Najlepšie schopnosti";i['confirmTitle']="Potvrďte svoj titul tu a my preveríme Vašu žiadosť.";i['hourlyRate']="Hodinová sadzba";i['languages']="Jazyky";i['lichessCoach']="Lichess tréner";i['lichessCoaches']="Lichess tréneri";i['location']="Poloha";i['nmOrFideTitle']="NM alebo FIDE titul";i['notAccepting']="Momentálne neprijíma študentov";i['otherExperiences']="Iné skúsenosti";i['playingExperience']="Hráčske skúsenosti";i['publicStudies']="Verejné štúdie";i['rating']="Rating";i['sendApplication']=s("Pošlite nám email na %s a preskúmame Vašu žiadosť.");i['sendPM']="Poslať súkromnú správu";i['teachingExperience']="Skúsenosti s učením";i['teachingMethod']="Metodológia výučby";i['viewXProfile']=s("Prezrieť si %s Lichess profil");i['xCoachesStudents']=s("%s trénuje šachových študentov");i['youtubeVideos']="YouTube videá"})()