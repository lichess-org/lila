"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.streamer)window.i18n.streamer={};let i=window.i18n.streamer;i['allStreamers']="Tutti gli streamer";i['approved']="Il tuo stream è approvato.";i['becomeStreamer']="Diventa uno streamer su Lichess";i['changePicture']="Cambia/elimina la tua immagine";i['currentlyStreaming']=s("In streaming ora: %s");i['downloadKit']="Scarica lo streamer kit";i['doYouHaveStream']="Hai un canale Twitch o YouTube?";i['editPage']="Modifica la pagina streamer";i['headline']="Intestazione";i['hereWeGo']="Eccoci qui!";i['keepItShort']=p({"one":"Mantienilo corto: max caratteri %s","other":"Mantienilo corto: %s caratteri al massimo"});i['lastStream']=s("Ultimo stream %s");i['lichessStreamer']="Lichess streamer";i['lichessStreamers']="Lichess streamer";i['live']="DIRETTA!";i['longDescription']="Descrizione estesa";i['maxSize']=s("Dimensione massima: %s");i['offline']="OFFLINE";i['optionalOrEmpty']="Opzionale. Lasciare vuoto se nessuno";i['pendingReview']="Il tuo stream è in fase di revisione da parte dei moderatori.";i['perk1']="Ricevi un\\'icona di streamer sul tuo profilo Lichess.";i['perk2']="Vieni messo in primo piano nella lista degli streamer.";i['perk3']="Informa i tuoi follower su Lichess.";i['perk4']="Mostra il tuo stream nei tuoi giochi, tornei e studi.";i['perks']="Vantaggi di fare streaming con la parola chiave";i['pleaseFillIn']="Per favore compila le tue informazioni da streamer e carica un\\'immagine.";i['requestReview']="richiedi la revisione di un moderatore";i['rule1']="Includi la parola chiave \\\"lichess.org\\\" nel titolo del tuo stream quando fai streaming su Lichess.";i['rule2']="Rimuovi la parola chiave quando fai streaming di materiale non-Lichess.";i['rule3']="Lichess rileverà automaticamente il tuo stream e riceverai i seguenti vantaggi:";i['rule4']=s("Leggi la nostra %s per assicurare a tutti il fair play durante la tua diretta.");i['rules']="Regole di streaming";i['streamerLanguageSettings']="La pagina dello streamer di Lichess si rivolge al pubblico con la lingua fornita dalla piattaforma di streaming. Imposta correttamente la lingua predefinita per i tuoi stream di scacchi nell\\'app o nel servizio che usi per trasmettere.";i['streamerName']="Il tuo nome da streamer su Lichess";i['streamingFairplayFAQ']="FAQ di Fairplay in streaming Fairplay";i['tellUsAboutTheStream']="Parlaci del tuo stream in una frase";i['twitchUsername']="Il tuo nome utente Twitch o URL";i['uploadPicture']="Carica una immagine";i['visibility']="Visibile sulla pagina degli streamer";i['whenApproved']="Quando approvato dai moderatori";i['whenReady']=s("Quando sei pronto per essere inserito nell\\'elenco dei Lichess streamer, %s");i['xIsStreaming']=s("%s è in diretta streaming");i['xStreamerPicture']=s("Immagine da streamer di %s");i['yourPage']="La tua pagina streamer";i['youTubeChannelId']="ID del tuo canale YouTube"})()