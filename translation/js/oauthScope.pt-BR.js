"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.oauthScope)window.i18n.oauthScope={};let i=window.i18n.oauthScope;i['alreadyHavePlayedGames']="Você já jogou!";i['apiAccessTokens']="Chaves de acesso à API";i['apiDocumentation']="documentação da API";i['apiDocumentationLinks']=s("Aqui está um %1$s e a %2$s.");i['attentionOfDevelopers']="Nota reservada aos desenvolvedores:";i['authorizationCodeFlow']="fluxo de código de autorização";i['boardPlay']="Jogar com a API de tabuleiro";i['botPlay']="Jogar com a API de bots";i['canMakeOauthRequests']=s("Você pode fazer solicitações OAuth sem passar pelo %s.");i['carefullySelect']="Escolha com cuidado o que podem fazer em seu nome.";i['challengeBulk']="Permitir criar vários jogos de uma vez para outros jogadores";i['challengeRead']="Acesso aos desafios recebidos";i['challengeWrite']="Permitir enviar, aceitar e rejeitar desafios";i['copyTokenNow']="Copie e cole sua nova chave de acesso pessoal em um local seguro. Você não poderá mais vê-la!";i['created']=s("Criado %s");i['doNotShareIt']="A chave dará acesso à sua conta. NÃO a compartilhe com ninguém!";i['emailRead']="Acesso ao endereço de e-mail";i['engineRead']="Permitir ver e usar seus softwares de assistência externos";i['engineWrite']="Permitir criar e atualizar softwares de assistência externos";i['followRead']="Acesso à lista de usuários seguidos";i['followWrite']="Permitir (parar de) seguir outros usuários";i['forExample']=s("Por exemplo: %s");i['generatePersonalToken']="crie uma chave de acesso pessoal";i['givingPrefilledUrls']="As URLs pré-preenchidas ajudam seus usuários a obterem os escopos de chave certos.";i['guardTokensCarefully']="Guarde as chaves com cuidado! Elas são como senhas. A vantagem de usar chaves em vez de colocar sua senha num script é que as chaves podem ser revogadas, e você pode criar várias.";i['insteadGenerateToken']=s("Em vez disso, %s para usar diretamente nos pedidos da API.");i['lastUsed']=s("Último uso %s");i['msgWrite']="Permitir enviar mensagens privadas a outros usuários";i['newAccessToken']="Nova chave pessoal de acesso à API";i['newToken']="Nova chave de acesso";i['personalAccessTokens']="Chaves de acesso pessoal à API";i['personalTokenAppExample']="exemplo de chave pessoal num aplicativo";i['possibleToPrefill']="É possível pré-preencher o formulário ajustando os parâmetros de consulta da URL.";i['preferenceRead']="Acesso a preferências";i['preferenceWrite']="Modificar preferências";i['puzzleRead']="Acesso à atividade de quebra-cabeças";i['racerWrite']="Permitir criar e entrar em corridas";i['rememberTokenUse']="Lembrará para você qual a função da chave";i['scopesCanBeFound']="Os códigos de escopo estão no código HTML do formulário.";i['studyRead']="Acesso a estudos privados e transmissões";i['studyWrite']="Permitir criar, atualizar e deletar estudos e transmissões";i['teamLead']="Administre suas equipes: envie mensagens privadas, expulse membros";i['teamRead']="Acesso a informações privadas da equipe";i['teamWrite']="Permitir entrar e sair de equipes";i['ticksTheScopes']=s("marca os escopos de %1$s e %2$s, e define a descrição da chave.");i['tokenDescription']="Descrição da chave";i['tokenGrantsPermission']="Uma chave permite que outras pessoas usem sua conta.";i['tournamentWrite']="Permitir criar, atualizar e entrar em torneios";i['webLogin']="Permitir iniciar sessões autenticadas (garante acesso completo!)";i['webMod']="Permitir usar ferramentas de moderador (com restrições)";i['whatTheTokenCanDo']="O que a chave pode fazer em seu nome:"})()