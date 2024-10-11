"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.team)window.i18n.team={};let i=window.i18n.team;i['allTeams']="Alle Teams";i['battleOfNbTeams']=p({"one":"Kampf von %s Teams","other":"Kampf von %s Teams"});i['beingReviewed']="Deine Beitrittsanfrage wird von einem Teamleiter überprüft.";i['closeTeam']="Team auflösen";i['closeTeamDescription']="Löst das Team für immer auf.";i['completedTourns']="Beendete Turniere";i['declinedRequests']="Abgelehnte Anfragen";i['entryCode']="Teambeitrittscode";i['entryCodeDescriptionForLeader']="(Optional) Ein Passwort, das neue Teammitglieder kennen müssen, um dem Team beizutreten.";i['incorrectEntryCode']="Falscher Beitrittscode.";i['innerTeam']="Inneres Team";i['joinLichessVariantTeam']=s("Tritt dem offiziellen %s-Team für Neuigkeiten und Ereignisse bei");i['joinTeam']="Tritt dem Team bei";i['kickSomeone']="Entferne jemanden aus dem Team";i['leadersChat']="Teamleiter-Chat";i['leaderTeams']="Von dir geleitete Teams";i['listTheTeamsThatWillCompete']="Liste die Teams auf, die in diesem Kampf antreten werden.";i['manuallyReviewAdmissionRequests']="Beitrittsanfragen manuell überprüfen";i['manuallyReviewAdmissionRequestsHelp']="Wenn diese Option aktiviert ist, müssen Spieler eine Anfrage schreiben, um dem Team beizutreten, welche du ablehnen oder akzeptieren kannst.";i['messageAllMembers']="Benachrichtige alle Mitglieder";i['messageAllMembersLongDescription']="Sende eine private Nachricht an ALLE Mitglieder dieses Teams. Du kannst die Funktion benutzen, um Spieler zu einem Turnier oder einem Team-Kampf aufzurufen. Spieler, die diese Art von Nachrichten nicht empfangen wollen, könnten dein Team verlassen.";i['messageAllMembersOverview']="Sende eine private Nachricht an alle Mitglieder des Teams";i['myTeams']="Meine Teams";i['nbLeadersPerTeam']=p({"one":"%s Führende pro Team","other":"%s Führende pro Team"});i['nbMembers']=p({"one":"%s Mitglied","other":"%s Mitglieder"});i['newTeam']="Neues Team";i['noTeamFound']="Kein Team gefunden";i['numberOfLeadsPerTeam']="Anzahl der Führenden pro Team. Die Summe ihrer Punkte ist die Punktzahl des Teams.";i['numberOfLeadsPerTeamHelp']="Du solltest diesen Wert nach Beginn des Turniers wirklich nicht ändern!";i['oneTeamPerLine']="Ein Team pro Zeile. Verwende die Auto-Vervollständigung.";i['oneTeamPerLineHelp']="Du kannst diese Liste von einem Turnier kopieren und in eine anderes einfügen!\n\nDu kannst kein Team entfernen, wenn ein Spieler aus diesem Team bereits am Turnier teilnimmt.";i['onlyLeaderLeavesTeam']="Bevor du das Team verlässt, füge bitte einen neuen Teamleiter hinzu, oder schließe das Team.";i['quitTeam']="Team verlassen";i['requestDeclined']="Deine Beitrittsanfrage wurde von einem Teamleiter abgelehnt.";i['subToTeamMessages']="Team-Nachrichten abonnieren";i['swissTournamentOverview']="Ein Schweizer-System-Turnier, dem nur Mitglieder deines Teams beitreten können";i['team']="Team";i['teamAlreadyExists']="Dieses Team existiert bereits.";i['teamBattle']="Teamkampf";i['teamBattleOverview']="Ein Kampf mehrerer Teams, jeder Spieler erzielt Punkte für sein Team";i['teamLeaders']=p({"one":"Teamleiter","other":"Teamleiter"});i['teamPage']="Teamseite";i['teamRecentMembers']="Neueste Teammitglieder";i['teams']="Teams";i['teamsIlead']="Teams, die ich leite";i['teamTournament']="Team-Turnier";i['teamTournamentOverview']="Ein Arenaturnier, dem nur Mitglieder deines Teams beitreten können";i['thisTeamBattleIsOver']="Dieses Turnier ist vorbei und die Teams können nicht mehr aktualisiert werden.";i['upcomingTournaments']="Kommende Turniere";i['whoToKick']="Wen willst du aus dem Team werfen?";i['willBeReviewed']="Deine Beitrittsanfrage wird von einem Teamleiter überprüft werden.";i['xJoinRequests']=p({"one":"%s Beitrittsanfrage","other":"%s Beitrittsanfragen"});i['youWayWantToLinkOneOfTheseTournaments']="Möchtest du eines der kommenden Turniere verlinken?"})()