"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.broadcast)window.i18n.broadcast={};let i=window.i18n.broadcast;i['aboutBroadcasts']="Zuzeneko emanaldiei buruz";i['addRound']="Gehitu txanda bat";i['ageThisYear']="Adina";i['broadcasts']="Emanaldiak";i['completed']="Amaitutako emanaldiak";i['completedHelp']="Txanda amaitu dela jatorrizko partidekin detektatzen du Lichessek. Erabili aukera hau jatorririk ez badago.";i['credits']="Jatorria zein den esaiguzu";i['currentGameUrl']="Uneko partidaren URL helbidea";i['definitivelyDeleteRound']="Betiko ezabatu txanda eta bere partida guztiak.";i['definitivelyDeleteTournament']="Txapelketa behin betiko ezabatu, bere txanda eta partida guztiak barne.";i['deleteAllGamesOfThisRound']="Ezabatu txanda honetako partida guztiak. Jatorria aktibo egon behar da berriz sortzeko.";i['deleteRound']="Ezabatu txanda hau";i['deleteTournament']="Ezabatu txapelketa hau";i['downloadAllRounds']="Deskargatu txanda guztiak";i['editRoundStudy']="Editatu txandako azterlana";i['federation']="Federazioa";i['fideFederations']="FIDE federazioak";i['fidePlayerNotFound']="FIDE jokalaria ez da aurkitu";i['fidePlayers']="FIDE jokalariak";i['fideProfile']="FIDE profila";i['fullDescription']="Ekitaldiaren deskribapen osoa";i['fullDescriptionHelp']=s("Emanaldiaren azalpen luzea, hautazkoa da. %1$s badago. Luzera %2$s karaktere edo laburragoa izan behar da.");i['howToUseLichessBroadcasts']="Nola erabili Lichessen Zuzenekoak.";i['liveBroadcasts']="Txapelketen zuzeneko emanaldiak";i['myBroadcasts']="Nire zuzenekoak";i['nbBroadcasts']=p({"one":"Zuzeneko %s","other":"%s zuzeneko"});i['newBroadcast']="Zuzeneko emanaldi berria";i['ongoing']="Orain martxan";i['periodInSeconds']="Aldia segundotan";i['periodInSecondsHelp']="Hautazkoa, zenbat itxaron eskaeren artean. Gutxienez 2 segundo, gehienez 60 segundo. Automatikora itzuliko da ikusle kopuruaren arabera.";i['recentTournaments']="Azken txapelketak";i['replacePlayerTags']="Hautazkoa: aldatu jokalarien izen, puntuazio eta tituluak";i['resetRound']="Berrezarri txanda hau";i['roundName']="Txandaren izena";i['roundNumber']="Txanda zenbaki";i['showScores']="Erakutsi jokalarien puntuazioak partiden emaitzen arabera";i['sourceGameIds']="Gehienez ere Lichesseko 64 partidren idak, espazioekin banatuta.";i['sourceSingleUrl']="PGNaren jatorrizko URLa";i['sourceUrlHelp']="Lichessek PGNaren eguneraketak jasoko dituen URLa. Interneteko helbide bat izan behar da.";i['startDateHelp']="Hautazkoa, ekitaldia noiz hasten den baldin badakizu";i['subscribedBroadcasts']="Harpidetutako emanaldiak";i['theNewRoundHelp']="Txanda berriak aurrekoak beste kide eta laguntzaile izango ditu.";i['top10Rating']="10 onenak";i['tournamentDescription']="Txapelketaren deskribapen laburra";i['tournamentName']="Txapelketaren izena";i['unrated']="Ez du sailkapenik";i['upcoming']="Hurrengo emanaldiak"})()