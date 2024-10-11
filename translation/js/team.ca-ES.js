"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.team)window.i18n.team={};let i=window.i18n.team;i['allTeams']="Tots els equips";i['battleOfNbTeams']=p({"one":"Batalla d\\'%s equip","other":"Batalla de %s equips"});i['beingReviewed']="La teva petició està sent revisada pel líder de l\\'equip.";i['closeTeam']="Eliminar equip";i['closeTeamDescription']="Elimina l\\'equip definitivament.";i['completedTourns']="Tornejos finalitzats";i['declinedRequests']="Sol·licituds declinades";i['entryCode']="Clau d\\'entrada a l\\'equip";i['entryCodeDescriptionForLeader']="(Opcional) Una contrasenya que els nous membres han de saber per unir-se a l\\'equip.";i['incorrectEntryCode']="Codi d\\'entrada incorrecte.";i['innerTeam']="Intern de l\\'equip";i['joinLichessVariantTeam']=s("Uniu-vos a l\\'equip oficial %s per estar al cas de les notícies i esdeveniments");i['joinTeam']="Uneix-te a l\\'equip";i['kickSomeone']="Expulsar algú de l\\'equip";i['leadersChat']="Xat de capitans";i['leaderTeams']="Equips que lideres";i['listTheTeamsThatWillCompete']="Enumera els equips que competiran en aquesta batalla.";i['manuallyReviewAdmissionRequests']="Comprovar manualment peticions d\\'admissió";i['manuallyReviewAdmissionRequestsHelp']="Si està marcat, els jugadors necessitaran fer una petició per unir-se a l\\'equip, la qual pots acceptar o rebutjar.";i['messageAllMembers']="Missatge a tots els membres";i['messageAllMembersLongDescription']="Envia un missatge privat a TOTS els membres de l\\'equip.\nPots fer-ho servir per avisar als jugadors per entrar a un torneig o batalla d\\'equips.\nEls jugadors que no els agrada rebre missatges podrien abandonar l\\'equip.";i['messageAllMembersOverview']="Enviar missatge privat a tots els membres de l\\'equip";i['myTeams']="Els meus equips";i['nbLeadersPerTeam']=p({"one":"Un líder per equip","other":"%s Líders per equip"});i['nbMembers']=p({"one":"%s membre","other":"%s membres"});i['newTeam']="Nou equip";i['noTeamFound']="No s\\'ha trobat cap equip";i['numberOfLeadsPerTeam']="Nombre dels líders de l\\'equip. La suma de la seva puntuació és la puntuació de l\\'equip.";i['numberOfLeadsPerTeamHelp']="No hauríeu de canviar aquest valor una vegada hagi començat el torneig!";i['oneTeamPerLine']="Un equip per línia. Utilitzeu l\\'autocompletat.";i['oneTeamPerLineHelp']="Podeu copiar i pegar aquesta llista d\\'un torneig a un altre!\n\nNo podeu eliminar un equip si un dels seus jugadors s\\'ha unit al torneig.";i['onlyLeaderLeavesTeam']="Si us plau, afegiu un líder o tanqueu l\\'equip abans de sortir-ne.";i['quitTeam']="Abandona l\\'equip";i['requestDeclined']="La teva sol·licitud ha estat declinada pel líder de l\\'equip.";i['subToTeamMessages']="Subscriu-te als missatges de l\\'equip";i['swissTournamentOverview']="Torneig Suís on només membres del teu equip es poden inscriure";i['team']="Equip";i['teamAlreadyExists']="Aquest equip ja existeix.";i['teamBattle']="Batalla d\\'equips";i['teamBattleOverview']="Batalla de diversos equips, cada jugador puntua pel seu equip";i['teamLeaders']=p({"one":"Líder de l\\'equip","other":"Líders de l\\'equip"});i['teamPage']="Pàgina de l\\'equip";i['teamRecentMembers']="Nous membres de l\\'equip";i['teams']="Equips";i['teamsIlead']="Equips que lidero";i['teamTournament']="Torneig d\\'equip";i['teamTournamentOverview']="Torneig Arena on només membres del teu equip poden inscriure\\'s";i['thisTeamBattleIsOver']="El torneig ha finalitzat i ja no es pot editar els equips.";i['upcomingTournaments']="Propers tornejos";i['whoToKick']="Qui voleu expulsar de l\\'equip?";i['willBeReviewed']="La teva petició serà revisada pel capità de l\\'equip.";i['xJoinRequests']=p({"one":"%s solicitud d\\'adhesió","other":"%s solicituds d\\'adhesió"});i['youWayWantToLinkOneOfTheseTournaments']="Potser vols enllaçar un d\\'aquests propers torneigs?"})()