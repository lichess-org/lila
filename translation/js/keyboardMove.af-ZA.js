"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.keyboardMove)window.i18n.keyboardMove={};let i=window.i18n.keyboardMove;i['bothTheLetterOAndTheDigitZero']="Beide die letter \\\"o\\\" en die syfer \\\"0\\\" kan gebruik word vir rokeer";i['capitalizationOnlyMattersInAmbiguousSituations']="Hoofletters is slegs belangrik indien daar dubbelsinnige situasies is met \\'n loper en \\'n b-pion";i['dropARookAtB4']="Laat vaar die toring op b4 (slegs Malhuis variant)";i['ifItIsLegalToCastleBothWays']="Indien dit wettig is om weerskante toe te rokeer, gebruik Enter vir die koningskant";i['ifTheAboveMoveNotationIsUnfamiliar']="Indien bogenoemde notasie vir jou onbekend is, leer meer hier:";i['includingAXToIndicateACapture']="Insluiting van \\\"x\\\" om \\'n vat-skuif aantedui is optioneel";i['keyboardInputCommands']="Sleutelbord-invoerbevele";i['kingsideCastle']="Rokeer aan die koningskant";i['moveKnightToC3']="Skuif ruiter na c3";i['movePieceFromE2ToE4']="Skuif stuk van e2 na e4";i['offerOrAcceptDraw']="Bied of aanvaar \\'n gelykop";i['otherCommands']="Ander bevele";i['performAMove']="Maak \\'n skuif";i['promoteC8ToQueen']="Promoveer c8 na dame";i['queensideCastle']="Rokeer aan die dameskant";i['readOutClocks']="Lees die horlosies hardop";i['readOutOpponentName']="Lees opponent se naam uit";i['tips']="Wenke";i['toPremoveSimplyTypeTheDesiredPremove']="Vir vooraf-skuiwe, tik bloot die skuif voordat dit jou beurt is"})()