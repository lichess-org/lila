"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.keyboardMove)window.i18n.keyboardMove={};let i=window.i18n.keyboardMove;i['bothTheLetterOAndTheDigitZero']="Bei der Rochade kann ee souwuel de Buchstaw \\\"o\\\" wéi och d\\'Ziffer \\\"0\\\" benotzen";i['capitalizationOnlyMattersInAmbiguousSituations']="D\\'Grouss- a Klengschreiwung spillt just dann eng Roll, wann anerwäerts eng Verwiesslung vun engem Leefer an engem b-Bauer optriede kéint";i['dropARookAtB4']="Op b4 een Tuerm asetzen (nëmme bei der Crazyhous-Variant)";i['ifItIsLegalToCastleBothWays']="Falls béid Rochadë legal sinn, dréck d\\'Enter-Tast fir kuerz ze rochéieren";i['ifTheAboveMoveNotationIsUnfamiliar']="Falls dir d\\'Notation hei driwwer net geleefeg ass, gëss du hei méi gewuer:";i['includingAXToIndicateACapture']="Een \\\"x\\\" ze schreiwen fir unzeginn, datt eng Figur geholl gëtt, ass fakultativ";i['keyboardInputCommands']="Tastatureingabebefeeler";i['kingsideCastle']="Kuerz Rochade";i['moveKnightToC3']="E Sprénger op c3 réckelen";i['movePieceFromE2ToE4']="Eng Figur vun e2 op e4 réckelen";i['offerOrAcceptDraw']="Remis bidden oder akzeptéieren";i['otherCommands']="Aner Befeeler";i['performAMove']="Een Zuch maachen";i['promoteC8ToQueen']="Op c8 an eng Damm ëmwandelen";i['queensideCastle']="Laang Rochade";i['readOutClocks']="D\\'Auer virliesen";i['readOutOpponentName']="Den Numm vum Géigner virliesen";i['tips']="Tippen";i['toPremoveSimplyTypeTheDesiredPremove']="Falls du een Zuch am Viraus maache wëlls, tipp den Zuch einfach an, iers du dru bass"})()