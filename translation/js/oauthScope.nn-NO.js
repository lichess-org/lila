"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.oauthScope)window.i18n.oauthScope={};let i=window.i18n.oauthScope;i['alreadyHavePlayedGames']="Du har allereie spelt parti!";i['apiAccessTokens']="API tilgjengenøklar";i['apiDocumentation']="API-dokumentasjon";i['apiDocumentationLinks']=s("Her er ein %1$s og %2$s.");i['attentionOfDevelopers']="Merknadar retta berre mot utviklarar:";i['authorizationCodeFlow']="autorisasjonskode prosess";i['boardPlay']="Spel parti med brett-api";i['botPlay']="Spel parti med bot-api";i['canMakeOauthRequests']=s("Du kan lage OAuth-førespurnader utan å gå gjennom %s.");i['carefullySelect']="Bruk godt omdøme når du velgjer kva andre kan gjera på dine vegne.";i['challengeBulk']="Opprett mange parti på ein gong for andre spelarar";i['challengeRead']="Les innkomne utfordringar";i['challengeWrite']="Skriv innkomne utfordringar";i['copyTokenNow']="Sørg for å kopiera eller skriva ned det nye tilgjengeteiknet med ein gong. Du vil ikkje få sjå det igjen!";i['created']=s("Oppretta %s");i['doNotShareIt']="Teiknet gjer tilgjenge til din konto. Del det ikkje med nokon!";i['emailRead']="Les e-postadresse";i['engineRead']="Sjå og bruk din eksterne sjakkmotor";i['engineWrite']="Opprett og oppdater ekstern motor";i['followRead']="Les følgde spelarar";i['followWrite']="Følg/ikkje følg andre spelarar";i['forExample']=s("Til dømes: %s");i['generatePersonalToken']="generer ein personlieg tilgjengenøkkel";i['givingPrefilledUrls']="Ved å gje desse ferdig utfylte nettadressene til brukarane hjelper du dei til å få rette tilgansnøklar.";i['guardTokensCarefully']="Pass godt på desse nøklane! Dei er som passord å rekne. Fordelen med å bruke nøklar framfor å sette passordet ditt inn i eit skript, er at nøklar kan tilbakekallas, og at du kan generere mange av dei.";i['insteadGenerateToken']=s("I staden kan du bruka %s direkte i API-forespurnader.");i['lastUsed']=s("Sist brukt: %s");i['msgWrite']="Send privat melding til andre spelarar";i['newAccessToken']="Nytt personleg API tilgjengeteikn (\\\"token\\\")";i['newToken']="Ny tilgjengenøkkel";i['personalAccessTokens']="Personlege API-tilgangsnøklar";i['personalTokenAppExample']="døme på ein personleg tilgjengenøkkel";i['possibleToPrefill']="Det er mogleg å fylle dette skjemaet ut på førehand ved å justere spørjingsparametrane til URL-en.";i['preferenceRead']="Lesepreferansar";i['preferenceWrite']="Skrivepreferansar";i['puzzleRead']="Les oppgåveaktivitet";i['racerWrite']="Opprett og delta i oppgåvekappløp";i['rememberTokenUse']="For at du skal hugsa kva dette tilgjengeteiknet gjeld";i['scopesCanBeFound']="Omfangskodane finn du i skjemaet si HTML-kode.";i['studyRead']="Les private studiar og sendingar";i['studyWrite']="Opprett, oppdater, slett studiar og sendingar";i['teamLead']="Administrer lag du leier: send personlege meldingar, fjern medlemar frå laget";i['teamRead']="Les privat laginformasjon";i['teamWrite']="Delta i, forlat og administrer lag";i['ticksTheScopes']=s("merkar av %1$s og %2$s omfanget og fastset nøkkelbeskrivinga.");i['tokenDescription']="Beskriving av tilgjengeteiknet";i['tokenGrantsPermission']="Eit tilgjengeteikn gjev andre rett til å bruka kontoen din.";i['tournamentWrite']="Opprett, oppdater og delta i turneringar";i['webLogin']="Opprett autentiserte nettstadsesjonar (gjev full tilgjenge!)";i['webMod']="Bruk moderatorverktøy (som er innanfor dine tilgangsrettar)";i['whatTheTokenCanDo']="Kva tilgjengeteiknet kan gjera på dine vegne:"})()