"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.broadcast)window.i18n.broadcast={};let i=window.i18n.broadcast;i['addRound']="Айналым қосу";i['broadcasts']="Көрсетілімдер";i['completed']="Аяқталған";i['credits']="Қайнар көзіне сілтеңіз";i['currentGameUrl']="Қазіргі ойын сілтемесі";i['definitivelyDeleteRound']="Айналым мен оның ойындарын толығымен жою.";i['definitivelyDeleteTournament']="Жарысты айналым мен ойындарымен бірге толығымен жою.";i['deleteAllGamesOfThisRound']="Айналымның бүкіл ойындарын жою. Оларды қайта құру үшін қайнар көзі белсенді болуы керек.";i['deleteRound']="Бұл айналымды жою";i['deleteTournament']="Бұл жарысты жою";i['downloadAllRounds']="Барлық айналымдарды жүктеп алу";i['editRoundStudy']="Айналымның зертханасын өзгерту";i['fullDescription']="Оқиғаның толық сипаттамасы";i['fullDescriptionHelp']=s("Көрсетілімнің қосымша үлкен сипаттамасы. %1$s қолданысқа ашық. Ұзындығы %2$s таңбадан кем болуы керек.");i['liveBroadcasts']="Жарыстың тікелей көрсетілімдері";i['myBroadcasts']="Менің көрсетілімдерім";i['nbBroadcasts']=p({"one":"%s көрсетілім","other":"%s көрсетілім"});i['newBroadcast']="Жаңа тікелей көрсетілім";i['ongoing']="Болып жатқан";i['resetRound']="Бұл айналымды жаңарту";i['roundName']="Айналым атауы";i['roundNumber']="Раунд нөмірі";i['sourceUrlHelp']="PGN жаңартуларын алу үшін Личес тексеретін сілтеме. Ол интернетте баршалыққа ашық болуы керек.";i['startDateHelp']="Міндетті емес, егер күнін біліп тұрсаңыз";i['tournamentDescription']="Жарыстың қысқа сипаттамасы";i['tournamentName']="Жарыс атауы";i['upcoming']="Келе жатқан"})()