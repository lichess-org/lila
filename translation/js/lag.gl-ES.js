"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.lag)window.i18n.lag={};let i=window.i18n.lag;i['andNowTheLongAnswerLagComposedOfTwoValues']="E agora, a resposta completa! A latencia do xogo depende de dous valores non relacionados (canto máis baixos, mellor):";i['isLichessLagging']="Lichess tarda en responder?";i['lagCompensation']="Compensación por retardo";i['lagCompensationExplanation']="Lichess compensa o retardo da rede. Isto inclúe o rertardo sostido ou os picos ocasionais. Hai límites e heurísticas baseadas no control de tempo e no retardo compensado até o momento, de modo que o resultado debería ser razoable para ambos os dous xogadores. Como resultado, ter un maior retardo na rede có opoñente non é unha desvantaxe!";i['lichessServerLatency']="Latencia do servidor de Lichess";i['lichessServerLatencyExplanation']="O tempo que se tarda en procesar un movemento no servidor. É o mesmo para todos os xogadores e só depende da carga do servidor. Cantos máis xogadores haxa nun momento, máis crece este tempo, pero os programadores de Lichess fan todo o posible para mantelo baixo. Rara vez excede os 10 ms.";i['measurementInProgressThreeDot']="Medindo latencias...";i['networkBetweenLichessAndYou']="Conexión entre ti e Lichess";i['networkBetweenLichessAndYouExplanation']="O tempo que se tarda en enviar un movemento desde a túa computadora ao servidor de Lichess e obter a resposta. Depende do lonxe que esteas de Lichess (Francia) e da calidade da túa conexión á internet. Os programadores de Lichess non poden arranxarche a Wifi ou facer que a luz vaia máis rápido.";i['noAndYourNetworkIsBad']="Non, pero a túa conexión non é boa.";i['noAndYourNetworkIsGood']="Non. E a túa rede está ben.";i['yesItWillBeFixedSoon']="Si. Arranxarémolo á maior brevidade!";i['youCanFindTheseValuesAtAnyTimeByClickingOnYourUsername']="Podes consultar ambos valores en calquera momento, facendo clic no teu nome de usuario na barra superior."})()