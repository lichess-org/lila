"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.coach)window.i18n.coach={};let i=window.i18n.coach;i['aboutMe']="Über mich";i['accepting']="Nimmt neue Schüler an";i['areYouCoach']=s("Bist du ein toller Schachtrainer mit einem %s?");i['availability']="Verfügbarkeit";i['bestSkills']="Größte Talente";i['confirmTitle']="Bestätige hier deinen Titel und wir werden deine Bewerbung überprüfen.";i['hourlyRate']="Stundensatz";i['languages']="Sprachen";i['lichessCoach']="Lichess Trainer";i['lichessCoaches']="Lichess Trainer";i['location']="Ort";i['nmOrFideTitle']="NM oder FIDE-Titel";i['notAccepting']="Nimmt im Moment keine neuen Schüler an";i['otherExperiences']="Andere Erfahrungen";i['playingExperience']="Spielerfahrung";i['publicStudies']="Öffentliche Studien";i['rating']="Wertungszahl";i['sendApplication']=s("Sende uns eine Mail an %s und wir werden deine Bewerbung überprüfen.");i['sendPM']="Sende eine private Nachricht";i['teachingExperience']="Lehrerfahrung";i['teachingMethod']="Lehrmethode";i['viewXProfile']=s("Lichess-Profil von %s");i['xCoachesStudents']=s("%s trainiert Schachschüler");i['youtubeVideos']="Youtube-Videos"})()