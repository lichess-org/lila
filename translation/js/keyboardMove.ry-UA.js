"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.keyboardMove)window.i18n.keyboardMove={};let i=window.i18n.keyboardMove;i['bothTheLetterOAndTheDigitZero']="Ай букву \\\"о\\\" ай число \\\"0\\\" мож хосновати при рокірованьови";i['capitalizationOnlyMattersInAmbiguousSituations']="Реґістру ся дає важность лем бы роздïлити пішака (b) тай слона (B)";i['dropARookAtB4']="Веричи туров на b4 (Лем для Crazyhouse)";i['ifItIsLegalToCastleBothWays']="Коли мож ся рокіровати у два бокы, нажміт Enter, обы ся рокіровати у ближньый бук";i['ifTheAboveMoveNotationIsUnfamiliar']="Кідь не розумієте шахматну нотацію, туй ся можете дознати булше:";i['includingAXToIndicateACapture']="Указовати \\\"х\\\" коли берете фіґуру не мусай, просто мож указати позіцію котру занимат фіґура";i['keyboardInputCommands']="Команды вводу из клавешници";i['kingsideCastle']="Курта рокіровка";i['moveKnightToC3']="Покласти коня на с3";i['movePieceFromE2ToE4']="Покласти фіґуру из е2 на е4";i['offerOrAcceptDraw']="Понукнути вадь прияти ремізу";i['otherCommands']="Другі команды";i['performAMove']="Зробити ход";i['promoteC8ToQueen']="Провести с8 у даму";i['queensideCastle']="Довга рокіровка";i['readOutClocks']="Проговорити зостаток часу";i['readOutOpponentName']="Прочитати имня противника";i['tips']="Тіпы";i['toPremoveSimplyTypeTheDesiredPremove']="Обы зробити передход просто уведіт свуй ход, до того як маєте ходити"})()