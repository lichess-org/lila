"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.oauthScope)window.i18n.oauthScope={};let i=window.i18n.oauthScope;i['alreadyHavePlayedGames']="Du hast bereits Partien gespielt!";i['apiAccessTokens']="API-Zugangs-Schlüssel";i['apiDocumentation']="API-Dokumentation";i['apiDocumentationLinks']=s("Hier ist ein %1$s und die %2$s.");i['attentionOfDevelopers']="Hinweis nur für Entwickler:";i['authorizationCodeFlow']="Autorisierungs-Code-Prozess";i['boardPlay']="Partien mit der Board-API spielen";i['botPlay']="Partien mit der Bot-API spielen";i['canMakeOauthRequests']=s("Du kannst OAuth-Anfragen erstellen, ohne den %s zu durchlaufen.");i['carefullySelect']="Entscheide sorgfältig, was in deinem Namen erlaubt sein soll.";i['challengeBulk']="Erstelle viele Partien gleichzeitig für andere Spieler";i['challengeRead']="Eingehende Herausforderungen lesen";i['challengeWrite']="Herausforderungen senden, akzeptieren und ablehnen";i['copyTokenNow']="Stelle sicher, dass du deinen persönlichen Zugangsschlüssel jetzt kopierst. Du wirst ihn nicht erneut sehen können!";i['created']=s("Erstellt: %s");i['doNotShareIt']="Der Zugangsschlüssel wird Zugriff auf dein Konto ermöglichen. Teile ihn NIEMALS!";i['emailRead']="E-Mail-Adresse lesen";i['engineRead']="Deine externen Engines anzeigen und benutzen";i['engineWrite']="Externe Engines erstellen und aktualisieren";i['followRead']="Gefolgte Spieler lesen";i['followWrite']="Anderen Spielern folgen und entfolgen";i['forExample']=s("Zum Beispiel: %s");i['generatePersonalToken']="generiere einen persönlichen Zugangs-Schlüssel";i['givingPrefilledUrls']="Die Bereitstellung von im vorab ausgefüllter URLs wird deinen Nutzern helfen, die passenden Zugangs-Schlüssel zu erhalten.";i['guardTokensCarefully']="Hüte diese Schlüssel gewissenhaft! Sie sind wie Passwörter. Der Vorteil bei der Verwendung von Schlüsseln anstelle von Passwörtern in Skripten ist, dass Schlüssel widerrufen werden können und du viele von ihnen generieren kannst.";i['insteadGenerateToken']=s("Stattdessen %s, den du direkt in deinen API-Anfragen benutzen kannst.");i['lastUsed']=s("Zuletzt benutzt: %s");i['msgWrite']="Anderen Spielern private Nachrichten senden";i['newAccessToken']="Neuer persönlicher API-Zugangsschlüssel";i['newToken']="Neuer Zugangs-Schlüssel";i['personalAccessTokens']="Persönlicher API-Zugangsschlüssel";i['personalTokenAppExample']="Beispiel für eine Anwendung mit persönlichem Zugangs-Schlüssel";i['possibleToPrefill']="Es ist möglich, dieses Formular im vorab auszufüllen, indem du die Abfrageparameter der URL bearbeitest.";i['preferenceRead']="Einstellungen lesen";i['preferenceWrite']="Einstellungen ändern";i['puzzleRead']="Aufgaben-Aktivität lesen";i['racerWrite']="Erstelle und trete Aufgaben-Rennen bei";i['rememberTokenUse']="Damit du dich daran erinnerst, wofür der Zugangsschlüssel gedacht ist";i['scopesCanBeFound']="Die Bereichs-Codes können im HTML-Code des Formulars gefunden werden.";i['studyRead']="Private Studien und Übertragungen lesen";i['studyWrite']="Erstelle, aktualisiere und lösche Studien und Übertragungen";i['teamLead']="Verwalte von dir geleitete Teams: Sende PMs, entferne Mitglieder";i['teamRead']="Private Team-Informationen lesen";i['teamWrite']="Teams beitreten und verlassen";i['ticksTheScopes']=s("%1$s und %2$s wählt die Bereiche aus und setzt die Schlüsselbeschreibung fest.");i['tokenDescription']="Zugangsschlüssel-Beschreibung";i['tokenGrantsPermission']="Ein Zugangsschlüssel erlaubt anderen Leuten Zugang zu deinem Konto.";i['tournamentWrite']="Erstelle, aktualisiere und trete Turnieren bei";i['webLogin']="Authentifizierte Website-Sitzungen erstellen (gewährt vollen Zugriff!)";i['webMod']="Moderator-Werkzeuge verwenden (innerhalb der Grenzen deiner Berechtigung)";i['whatTheTokenCanDo']="Was der Zugangsschlüssel in deinem Namen tun kann:"})()