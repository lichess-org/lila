"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.voiceCommands)window.i18n.voiceCommands={};let i=window.i18n.voiceCommands;i['cancelTimerOrDenyARequest']="Cancelar temporizador ou negar solicitação";i['castle']="Roque (qualquer lado)";i['instructions1']=s("Usa o botão %1$s para alternar o reconhecimento de voz, o botão %2$s para abrir esta caixa de diálogo de ajuda e o menu %3$s para alterar as configurações de voz.");i['instructions2']="Mostramos setas para vários movimentos quando não temos certeza. Fala a cor ou o número de uma seta de movimento para selecioná-la.";i['instructions3']=s("Se uma seta exibir um radar abrangente, esse movimento será tocado quando o círculo estiver completo. Durante esse tempo, você só pode dizer %1$s para jogar o movimento imediatamente, %2$s para cancelar, ou falar a cor/número de uma seta diferente. Esse temporizador pode ser ajustado ou desligado nas configurações.");i['instructions4']=s("Ative %s em ambientes barulhentos. Segura shift enquanto falas comandos quando estiver ativado.");i['instructions5']="Usa o alfabeto fonético para melhorar o reconhecimento dos arquivos do tabuleiro.";i['instructions6']=s("%s explica detalhadamente as configurações do movimento de voz.");i['moveToE4OrSelectE4Piece']="Mover para e4 ou selecionar a peça de e4";i['phoneticAlphabetIsBest']="Alfabeto fonético é melhor";i['playPreferredMoveOrConfirmSomething']="Joga o movimento preferido ou confirma algo";i['selectOrCaptureABishop']="Seleciona ou captura um bispo";i['showPuzzleSolution']="Mostrar solução do problema";i['sleep']="Suspender (se houver palavra ativada)";i['takeRookWithQueen']="Pega a torre com a dama";i['thisBlogPost']="Este post no blog";i['turnOffVoiceRecognition']="Desativar reconhecimento de voz";i['voiceCommands']="Comandos de voz";i['watchTheVideoTutorial']="Ver o vídeo tutorial"})()