"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.broadcast)window.i18n.broadcast={};let i=window.i18n.broadcast;i['addRound']="Lisa voor";i['broadcasts']="Otseülekanded";i['completed']="Lõppenud";i['credits']="Viita allikale";i['currentGameUrl']="Praeguse mängu URL";i['definitivelyDeleteRound']="Kustuta lõplikult voor ja selle mängud.";i['deleteAllGamesOfThisRound']="Kustuta kõik mängud sellest voorust. Allikas peab olema aktiveeritud nende taastamiseks.";i['deleteRound']="Kustuta see voor";i['downloadAllRounds']="Lae alla kõik voorud";i['fullDescription']="Sündmuse täielik kirjeldus";i['fullDescriptionHelp']=s("Valikuline otseülekande kirjeldus. %1$s on saadaval. Pikkus peab olema maksimaalselt %2$s tähemärki.");i['liveBroadcasts']="Otseülekanded turniirilt";i['newBroadcast']="Uus otseülekanne";i['ongoing']="Käimas";i['resetRound']="Lähtesta see voor";i['roundName']="Vooru nimi";i['roundNumber']="Vooru number";i['sourceUrlHelp']="URL, kust Lichess saab PGN-i värskenduse. See peab olema Internetist kättesaadav.";i['startDateHelp']="Valikuline, kui tead millal sündmus algab";i['tournamentDescription']="Lühike turniiri kirjeldus";i['tournamentName']="Turniiri nimi";i['upcoming']="Tulemas"})()