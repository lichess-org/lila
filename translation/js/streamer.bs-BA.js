"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.streamer)window.i18n.streamer={};let i=window.i18n.streamer;i['allStreamers']="Svi emiteri";i['approved']="Vaš kanal za emitovanje je odobren.";i['becomeStreamer']="Postanite Lichess emiter";i['changePicture']="Promjenite / izbrišite Vašu sliku";i['currentlyStreaming']=s("Trenutno emituje: %s");i['downloadKit']="Preuzmite paket alata za emitere";i['doYouHaveStream']="Da li imate Twitch ili YouTube emiterski korisnički račun?";i['editPage']="Uredite stranicu emitera";i['headline']="Naslov";i['hereWeGo']="Idemo!";i['keepItShort']=p({"one":"Neka bude kratko: %s slovo maksimalno","few":"Neka bude kratko: %s slova maksimalno","other":"Neka bude kratko: %s slova maksimalno"});i['lastStream']=s("Poslednje emitovanje: %s");i['lichessStreamer']="Lichess emiter";i['lichessStreamers']="Lichess emiteri";i['live']="UŽIVO!";i['longDescription']="Dugi opis";i['maxSize']=s("Maksimalna veličina: %s");i['offline']="NIJE NA MREŽI";i['optionalOrEmpty']="Neobavezno. Ostavite prazno ako ga nemate";i['pendingReview']="Moderatori pregledaju Vaš kanal za emitovanje.";i['perk1']="Dobićete vatrenu ikonicu emitera na svom Lichess profilu.";i['perk2']="Probićete se na vrh liste emitera.";i['perk3']="Obavjestite svoje Lichess pratioce.";i['perk4']="Pokažite svoj kanal za emitovanje u svojim igrama, turnirima i studijama.";i['perks']="Prednosti emitovanja sa ključnom riječi";i['pleaseFillIn']="Molimo Vas da popunite Vaše informacije o emiteru i da stavite Vašu sliku.";i['requestReview']="zatražite pregled moderatora";i['rule1']="Uključite ključnu riječ \\\"lichess.org\\\" u naslov i koristite kategoriju \\\"Chess\\\", što znači šah, kada emitujete na Lichess-u.";i['rule2']="Uklonite ključnu riječ kada emitujete sadržaj koji nije vezan za Lichess.";i['rule3']="Lichess će automatski otkriti Vaše emitovanje i omogućiti sljedeće prednosti:";i['rule4']=s("Pročitajte naša %s kako biste osigurali korektnu igru za svakoga tokom Vašeg prijenosa uživo.");i['rules']="Pravila emitovanja";i['streamerLanguageSettings']="Stranica strimera na Lichessu cilja na Vašu publiku jezikom koji pruža Vaša platforma za striming. Postavite ispravan glavni jezik za Vaše šahovske striminge u aplikaciji ili usluzi koju koristite za emitiranje.";i['streamerName']="Ime Vašeg Lichess emiter kanala";i['streamingFairplayFAQ']="najčešća pitanja o korektnoj igri tokom prijenosa uživo";i['tellUsAboutTheStream']="Opišite Vaš kanal za emitovanje u jednoj rečenici";i['twitchUsername']="Vaše Twitch korisničko ime ili link";i['uploadPicture']="Učitajte sliku";i['visibility']="Vidljivo na stranici kanala za emitovanje";i['whenApproved']="Nakon odobrenja moderatora";i['whenReady']=s("Kada budete spremni da budete izlistani kao lichess emiter, %s");i['xIsStreaming']=s("%s emituje");i['xStreamerPicture']=s("%s slika emitera");i['yourPage']="Vaša emiterska stranica";i['youTubeChannelId']="ID Vašeg kanala na YouTubeu"})()