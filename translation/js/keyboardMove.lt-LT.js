"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.keyboardMove)window.i18n.keyboardMove={};let i=window.i18n.keyboardMove;i['bothTheLetterOAndTheDigitZero']="Ir raidė \\\"o\\\" ir skaitmuo nulis \\\"0\\\" gali būti naudojami rokiruojantis";i['capitalizationOnlyMattersInAmbiguousSituations']="Didžiosios raidės svarbios tik tais atvejais, kai gali atsirasti dviprasmybių, pavyzdžiui kai galima judėti ir rikiu (B) ir b-stulpelio pėstininku";i['dropARookAtB4']="Pastatyti bokštą ties b4 (tik Crazyhouse variante)";i['ifItIsLegalToCastleBothWays']="Jeigu galima atlikti rokiruotę į abi puses, pasinaudodami enter galite rokiruotis į karaliaus pusę";i['ifTheAboveMoveNotationIsUnfamiliar']="Jei pateiktas užrašymo būdas nepažįstamas sužinokite daugiau čia:";i['includingAXToIndicateACapture']="Pridėkite \\\"x\\\" nurodydami, kad kirtimas nėra būtinas";i['keyboardInputCommands']="Įvedimo klaviatūra komandos";i['kingsideCastle']="Rokiruotė karaliaus pusėje";i['moveKnightToC3']="Perkelti žirgą į c3";i['movePieceFromE2ToE4']="Perkelti figūrą iš e2 į e4";i['offerOrAcceptDraw']="Pasiūlyti ar priimti lygiąsias";i['otherCommands']="Kitos komandos";i['performAMove']="Daryti ėjimą";i['promoteC8ToQueen']="Paaukštinti c8 į valdovę";i['queensideCastle']="Rokiruotė valdovės pusėje";i['readOutClocks']="Garsiai perskaityti laikrodžius";i['readOutOpponentName']="Perskaityti priešininko vardą";i['tips']="Patarimai";i['toPremoveSimplyTypeTheDesiredPremove']="Norėdami padaryti išankstinį ėjimą suveskite savo ėjimą prieš ateinant jūsų eilei"})()