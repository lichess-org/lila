"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.streamer)window.i18n.streamer={};let i=window.i18n.streamer;i['allStreamers']="Alle streamere";i['approved']="Din stream er godkendt.";i['becomeStreamer']="Bliv en Lichess-streamer";i['changePicture']="Ændr/slet dit billede";i['currentlyStreaming']=s("Streamer i øjeblikket: %s");i['downloadKit']="Download streamer-kit";i['doYouHaveStream']="Har du en Twitch- eller YouTube-stream?";i['editPage']="Rediger streamer-side";i['headline']="Overskrift";i['hereWeGo']="Så er det nu!";i['keepItShort']=p({"one":"Hold det kort: maksimalt %s tegn","other":"Hold det kort: maksimalt %s tegn"});i['lastStream']=s("Sidste stream %s");i['lichessStreamer']="Lichess-streamer";i['lichessStreamers']="Lichess-streamere";i['live']="LIVE!";i['longDescription']="Lang beskrivelse";i['maxSize']=s("Maks størrelse: %s");i['offline']="OFFLINE";i['optionalOrEmpty']="Valgfrit. Efterlad tomt hvis intet";i['pendingReview']="Din stream bliver gennemgået af moderatorer.";i['perk1']="Få et flammende streamer-ikon på din Lichess-profil.";i['perk2']="Stryg til tops på streamer-listen.";i['perk3']="Giv dine Lichess-følgere besked.";i['perk4']="Vis din stream i dine partier, turneringer og studier.";i['perks']="Fordele ved streaming med nøgleordet";i['pleaseFillIn']="Udfyld venligst din streamer-information og upload et billede.";i['requestReview']="anmod om en moderator-gennemgang";i['rule1']="Inkluder nøgleordet \\\"lichess.org\\\" i den streamtitel, når du streamer på Lichess.";i['rule2']="Fjern nøgleordet, når du streamer ikke-Lichess ting.";i['rule3']="Lichess vil automatisk detektere din stream og aktivere følgende frynsegode:";i['rule4']=s("Læs vores %s for at sikre fair play for alle under din stream.");i['rules']="Streamingsregler";i['streamerLanguageSettings']="Lichess-streamersiden er målrettet dit publikum med det sprog, som din streamingplatform leverer. Indstil det korrekte standardsprog for dine skakstreams i den app eller tjeneste, du bruger til at sende.";i['streamerName']="Dit streamernavn på Lichess";i['streamingFairplayFAQ']="streaming Fairplay FAQ";i['tellUsAboutTheStream']="Beskriv din stream for os i én sætning";i['twitchUsername']="Dit Twitch-brugernavn eller URL";i['uploadPicture']="Upload et billede";i['visibility']="Synlig på streamere-siden";i['whenApproved']="Når godkendt af moderatorer";i['whenReady']=s("Når du er klar til at blive opført som en lichess-streamer, %s");i['xIsStreaming']=s("%s streamer");i['xStreamerPicture']=s("%s streamer-billede");i['yourPage']="Din streamer-side";i['youTubeChannelId']="Dit YouTube-kanal-id"})()