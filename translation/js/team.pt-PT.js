"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.team)window.i18n.team={};let i=window.i18n.team;i['allTeams']="Todas as equipas";i['battleOfNbTeams']=p({"one":"Batalha de %s equipa","other":"Batalha de %s equipas"});i['beingReviewed']="O teu pedido de adesão está a ser revisto pelo líder da equipa.";i['closeTeam']="Encerrar equipa";i['closeTeamDescription']="Encerrar a equipa para sempre.";i['completedTourns']="Torneios concluídos";i['declinedRequests']="Pedidos Recusados";i['entryCode']="Código da equipa";i['entryCodeDescriptionForLeader']="(Opcional) Um código de entrada que os novos membros devem saber para se juntarem a esta equipa.";i['incorrectEntryCode']="Código incorreto.";i['innerTeam']="Equipa interna";i['joinLichessVariantTeam']=s("Junte-se à equipa oficial do %s para ficar a par de novidades e eventos");i['joinTeam']="Juntar-se à equipa";i['kickSomeone']="Expulsar alguém da equipa";i['leadersChat']="Chat dos líderes";i['leaderTeams']="Equipas do líder";i['listTheTeamsThatWillCompete']="Liste as equipas que irão competir nesta batalha.";i['manuallyReviewAdmissionRequests']="Rever pedidos de admissão manualmente";i['manuallyReviewAdmissionRequestsHelp']="Se selecionado, os jogadores vão precisar de escrever um pedido de adesão para se juntarem à equipa, que tu podes aceitar ou recusar.";i['messageAllMembers']="Enviar uma mensagem a todos os membros";i['messageAllMembersLongDescription']="Enviar uma mensagem privada para todos os membros da equipa.\nPodes usar isto para chamar jogadores para participar num torneio ou batalha de equipas.\nOs jogadores que não gostam de receber as tuas mensagens podem sair da equipa.";i['messageAllMembersOverview']="Enviar uma mensagem privada para todos os membros da equipa";i['myTeams']="As minhas equipas";i['nbLeadersPerTeam']=p({"one":"Um líder por equipa","other":"%s líderes por equipa"});i['nbMembers']=p({"one":"%s membro","other":"%s membros"});i['newTeam']="Nova equipa";i['noTeamFound']="Nenhuma equipa encontrada";i['numberOfLeadsPerTeam']="Número de líderes por equipa. A soma da pontuação deles é a pontuação da equipa.";i['numberOfLeadsPerTeamHelp']="Realmente não deverias mudar esse valor após o torneio ter começado!";i['oneTeamPerLine']="Uma equipa por linha. Usa o preenchimento automático.";i['oneTeamPerLineHelp']="Podes copiar e colar esta lista de um torneio para outro!\n\nNão podes remover uma equipa se um jogador já tiver entrado no torneio com ela.";i['onlyLeaderLeavesTeam']="Por favor, adicione um novo líder de equipa antes de sair ou feche a equipa.";i['quitTeam']="Abandonar equipa";i['requestDeclined']="O seu pedido de adesão foi recusado por um líder de equipa.";i['subToTeamMessages']="Subscrever mensagens de equipa";i['swissTournamentOverview']="Um torneio suiço em que só podem entrar membros da tua equipa";i['team']="Equipa";i['teamAlreadyExists']="Esta equipa já existe.";i['teamBattle']="Batalha de Equipas";i['teamBattleOverview']="Uma batalha de várias equipas, cada jogador ganha pontos para a sua equipa";i['teamLeaders']=p({"one":"Líder da equipa","other":"Líderes da equipa"});i['teamPage']="Página da Equipa";i['teamRecentMembers']="Novos membros";i['teams']="Equipas";i['teamsIlead']="Equipas que eu lidero";i['teamTournament']="Torneio de equipa";i['teamTournamentOverview']="Um torneio em arena em que só podem entrar membros da tua equipa";i['thisTeamBattleIsOver']="Este torneio acabou, e as equipas não podem mais ser atualizadas.";i['upcomingTournaments']="Próximos torneios";i['whoToKick']="Quem é que queres expulsar da equipa?";i['willBeReviewed']="O teu pedido de adesão será revisto pelo líder da equipa.";i['xJoinRequests']=p({"one":"%s pedido de adesão","other":"%s pedidos de adesão"});i['youWayWantToLinkOneOfTheseTournaments']="Talvez queiras enviar um link de algum destes futuros torneios."})()