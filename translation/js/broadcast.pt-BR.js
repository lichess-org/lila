"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.broadcast)window.i18n.broadcast={};let i=window.i18n.broadcast;i['aboutBroadcasts']="Sobre as transmissões";i['addRound']="Adicionar uma rodada";i['ageThisYear']="Idade atual";i['broadcastCalendar']="Calendário das transmissões";i['broadcasts']="Transmissões";i['completed']="Concluído";i['completedHelp']="O Lichess detecta o fim da rodada baseado nos jogos fonte. Use essa opção se não houver fonte.";i['credits']="Crédito a fonte";i['currentGameUrl']="URL da partida atual";i['definitivelyDeleteRound']="Deletar permanentemente todas as partidas desta rodada.";i['definitivelyDeleteTournament']="Excluir permanentemente todo o torneio, incluindo todas as rodadas e jogos.";i['deleteAllGamesOfThisRound']="Deletar todas as partidas desta rodada. A fonte deverá estar ativa para criá-las novamente.";i['deleteRound']="Excluir esta rodada";i['deleteTournament']="Excluir este torneio";i['downloadAllRounds']="Baixar todas as rodadas";i['editRoundStudy']="Editar estudo da rodada";i['federation']="Federação";i['fideFederations']="Federações FIDE";i['fidePlayerNotFound']="Jogador não encontrando na FIDE";i['fidePlayers']="Jogadores FIDE";i['fideProfile']="Perfil FIDE";i['fullDescription']="Descrição completa do evento";i['fullDescriptionHelp']=s("Descrição longa e opcional da transmissão. %1$s está disponível. O tamanho deve ser menor que %2$s caracteres.");i['howToUseLichessBroadcasts']="Como usar as transmissões do Lichess.";i['liveBroadcasts']="Transmissões ao vivo do torneio";i['myBroadcasts']="Minhas transmissões";i['nbBroadcasts']=p({"one":"%s transmissão","other":"%s transmissões"});i['newBroadcast']="Nova transmissão ao vivo";i['ongoing']="Em andamento";i['periodInSeconds']="Período em segundos";i['periodInSecondsHelp']="Opcional: tempo entre as solicitações. Mín. 2s, máx. 60s. Por padrão, é calculado com base no número de espectadores.";i['recentTournaments']="Torneios recentes";i['replacePlayerTags']="Opcional: substituir nomes de jogador, ratings e títulos";i['resetRound']="Reiniciar esta rodada";i['roundName']="Nome da rodada";i['roundNumber']="Número da rodada";i['showScores']="Mostrar pontuações dos jogadores com base nos resultados das partidas";i['sourceGameIds']="Até 64 IDs de partidas do Lichess, separados por espaços.";i['sourceSingleUrl']="URL de origem de PGN";i['sourceUrlHelp']="URL que Lichess irá verificar para obter atualizações PGN. Deve ser acessível ao público a partir da Internet.";i['startDateHelp']="Opcional, se você sabe quando o evento começa";i['startDateTimeZone']=s("Data de início no horário local do torneio: %s");i['subscribedBroadcasts']="Transmissões em que você se inscreveu";i['theNewRoundHelp']="A nova rodada terá os mesmos membros e colaboradores que a anterior.";i['top10Rating']="Classificação top 10";i['tournamentDescription']="Descrição curta do torneio";i['tournamentName']="Nome do torneio";i['unrated']="Sem rating";i['upcoming']="Próximos"})()