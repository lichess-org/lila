"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.streamer)window.i18n.streamer={};let i=window.i18n.streamer;i['allStreamers']="Alle strømmere";i['approved']="Strømmen din er godkjent.";i['becomeStreamer']="Bli en Lichess-strømmer";i['changePicture']="Endre/slette ditt strømmerbilde";i['currentlyStreaming']=s("Strømmer nå: %s");i['downloadKit']="Last ned strømmer-pakken";i['doYouHaveStream']="Har du en Twitch- eller YouTube-kanal?";i['editPage']="Rediger strømmer-siden";i['headline']="Overskrift";i['hereWeGo']="Nå kjører vi!";i['keepItShort']=p({"one":"Hold deg kort; maks %s tegn","other":"Hold deg kort; maks %s tegn"});i['lastStream']=s("Strømmet sist %s");i['lichessStreamer']="Lichess-strømmer";i['lichessStreamers']="Lichess-strømmere";i['live']="DIREKTE!";i['longDescription']="Lang beskrivelse";i['maxSize']=s("Maks størrelse: %s");i['offline']="AVKOBLET";i['optionalOrEmpty']="Valgfritt. Kan være tom";i['pendingReview']="Strømmen din blir nå vurdert av moderatorne.";i['perk1']="Få et spesielt strømmer-ikon på Lichess-profilen din.";i['perk2']="Bli flyttet til toppen av listen over strømmere.";i['perk3']="Send en melding til følgerne dine på Lichess.";i['perk4']="Vis strømmen din i partier, turneringer og studier på Lichess.";i['perks']="Fordeler ved å inkludere \\\"lichess.org\\\" i tittelen på strømmen";i['pleaseFillIn']="Vennligst fyll inn informasjon om strømmen din og last opp et bilde.";i['requestReview']="be om en vurdering fra en moderator";i['rule1']="Du må inkludere nøkkelordet «lichess.org» i tittelen på strømmen og bruke kategorien «Chess» når du strømmer på Lichess.";i['rule2']="Fjern \\\"lichess.org\\\" fra tittelen på strømmen når du strømmer noe annet enn Lichess.";i['rule3']="Lichess oppdager strømmen din automatisk og gir deg følgende fordeler:";i['rule4']=s("Les vår %s for å sikre like forhold for alle mens du strømmer.");i['rules']="Strømmeregler";i['streamerLanguageSettings']="Strømmesiden til Lichess retter seg mot publikummet ditt med språket strømmeplattformen din leverer. Angi riktig språk for sjakkstrømmene dine i appen eller tjenesten du bruker for overføringer.";i['streamerName']="Ditt strømmernavn på Lichess";i['streamingFairplayFAQ']="FAQ for sportslig opptreden for strømmere";i['tellUsAboutTheStream']="I én setning, fortell oss om strømmen din";i['twitchUsername']="Ditt Twitch-brukernavn eller URL";i['uploadPicture']="Last opp et bilde";i['visibility']="Synlig på strømmersiden";i['whenApproved']="Når godkjent av moderatorne";i['whenReady']=s("Når du er klar til å bli listet som en Lichess-strømmer, %s");i['xIsStreaming']=s("%s strømmer nå");i['xStreamerPicture']=s("%s strømmerbilde");i['yourPage']="Din strømmer-side";i['youTubeChannelId']="ID til YouTube-kanalen din"})()