"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.streamer)window.i18n.streamer={};let i=window.i18n.streamer;i['allStreamers']="Alle streamers";i['approved']="Je stream is goedgekeurd.";i['becomeStreamer']="Word een Lichess streamer";i['changePicture']="Wijzig/verwijder je foto";i['currentlyStreaming']=s("Streamt nu: %s");i['downloadKit']="Download streamer kit";i['doYouHaveStream']="Heb je een Twitch- of YouTube-kanaal?";i['editPage']="Wijzig streamer pagina";i['headline']="Titel";i['hereWeGo']="Daar gaan we!";i['keepItShort']=p({"one":"Houd het kort: maximaal %s tekens","other":"Houd het kort: maximaal %s tekens"});i['lastStream']=s("Vorige stream: %s");i['lichessStreamer']="Lichess streamer";i['lichessStreamers']="Lichess streamers";i['live']="LIVE!";i['longDescription']="Uitgebreide omschrijving";i['maxSize']=s("Maximale grootte: %s");i['offline']="OFFLINE";i['optionalOrEmpty']="Optioneel. Laat leeg indien geen";i['pendingReview']="Je stream wordt beoordeeld door de toezichthouders.";i['perk1']="Krijg een brandende streamer pictogram op je Lichess profiel.";i['perk2']="Word aan de bovenkant van de streamers lijst toegevoegd.";i['perk3']="Waarschuw Lichess volgers.";i['perk4']="Toon je stream in je partijen, toernooien en studies.";i['perks']="Voordelen van het streamen met een trefwoord";i['pleaseFillIn']="Vul je streamer informatie in en upload een foto.";i['requestReview']="verzoek een beoordeling van een toezichthouder";i['rule1']="Voeg het trefwoord \\\"lichess.org\\\" toe aan uw stream titel wanneer je streamt op Lichess.";i['rule2']="Verwijder het trefwoord wanneer u niet-Lichess dingen streamt.";i['rule3']="Lichess detecteert je stream automatisch en schakelt de volgende voordelen in:";i['rule4']=s("Lees onze %s om te zorgen dat fair play voor iedereen geldt tijdens uw stream.");i['rules']="Streaming regels";i['streamerLanguageSettings']="De Lichess streamerpagina toont je publiek de taal die wordt verstrekt door je streamingplatform. Stel de juiste standaardtaal in voor je schaakstreams in de app of service die je gebruikt om te streamen.";i['streamerName']="Je streamer naam op Lichess";i['streamingFairplayFAQ']="streamen Fairplay FAQ";i['tellUsAboutTheStream']="Vertel ons in één zin over je stream";i['twitchUsername']="Je Twitch gebruikersnaam of website";i['uploadPicture']="Upload een foto";i['visibility']="Zichtbaar op de streamers pagina";i['whenApproved']="Zodra goedgekeurd door toezichthouders";i['whenReady']=s("Wanneer u klaar bent om als Lichess stream vermeld te worden, %s");i['xIsStreaming']=s("%s streamt nu");i['xStreamerPicture']=s("%s streamer afbeelding");i['yourPage']="Mijn streamer pagina";i['youTubeChannelId']="Jouw YouTubekanaal-ID"})()