"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.contact)window.i18n.contact={};let i=window.i18n.contact;i['accountSupport']="Ezhomm em eus da gaout sikour e-keñver ma c\\'hont";i['authorizationToUse']="Aotre d\\'ober gant Lichess";i['buyingLichess']="Prenañ Lichess";i['calledEnPassant']="\\\"En ur dremen\\\" a vez graet deus-se hag ur reolenn diazez eus ar c\\'hoari an hini eo.";i['cantChangeMore']="Ne c\\'haller ket cheñch ouzhpenn ment al lizherennoù. Evit abegoù teknikel an hini eo.";i['cantClearHistory']="Ne c\\'haller ket diverkañ roll-istor ho krogadoù, pe hini ho poelladennoù pe ho renkadur.";i['changeUsernameCase']="Kit war ar bajenn-mañ da cheñch ment lizherennoù hoc\\'h anv-implijer";i['closeYourAccount']="Gallout a rit serriñ ho kont war ar bajenn-mañ";i['contact']="Darempred";i['contactLichess']="Mont e darempred gant Lichess";i['creditAppreciated']="Kontant e vefemp ma venegfec\\'h Lichess daoust ma n\\'oc\\'h ket rediet hen ober.";i['doNotAskByEmail']="Arabat goulenn ganeoc\\'h serriñ ur gont dre bostel, ne raimp ket.";i['doNotAskByEmailToReopen']="Arabat goulenn ganeoc\\'h digeriñ en-dro ur gont dre bostel, ne raimp ket.";i['doNotDeny']="Arabat nac\\'hañ bezañ truchet. Ma fell deoc\\'h bezañ aotreet da grouiñ ur gont nevez eo gwelloc\\'h deoc\\'h anzav ar pezh az peus graet ha kompren pegen fall e oa.";i['errorPage']="Pajenn fazi";i['explainYourRequest']="Displegit fraezh ha sklaer ho koulenn. Menegit hoc\\'h anv-impljer Lichess hag ar pep retañ evit ma c\\'hallfemp ho sikour.";i['forgotPassword']="Disoñjet em eus ma ger-tremen";i['forgotUsername']="Disoñjet em eus ma anv-implijer";i['iCantLogIn']="Ne c\\'hallan ket kevreañ";i['illegalPawnCapture']="Paket eo bet ur pezh gwerin en un doare direizh";i['noConfirmationEmail']="N\\'em eus ket resevet postel ebet evit kadarnat";i['noneOfTheAbove']="Hini ebet eus ar re meneget uheloc\\'h";i['noRatingPoints']="Poent renkadur ebet zo bet roet";i['orCloseAccount']="Padal e c\\'hallit serriñ ar gont-mañ ha digeriñ unan all.";i['ratedGame']="Bezit sur ho peus kemeret perzh en ur c\\'hrogad renket rak ar c\\'hrogadoù a vignoniezh ne cheñchont ket renkadur ar c\\'hoarierien.";i['reopenOnThisPage']="Gallout a rit digeriñ ho kont en-dro war ar bajenn-mañ. Ne vo ket tu d\\'ober se nemet ur wech.";i['reportBugInDiscord']="War Lichess Discord server";i['sendAppealTo']=s("Galv a c\\'hallit ober: %s.");i['sendEmailAt']=s("Kasit ur postel deomp da %s.");i['videosAndBooks']="Gallout a rit ober gant Lichess en ho videoioù ha moullañ tapadennoù-skramm en ho levrioù.";i['visitThisPage']="Kit war ar pajenn-mañ da zirouestlañ ho kudenn";i['wantChangeUsername']="Fellout a ra din kemm ma anv arveriad";i['wantClearHistory']="C\\'hoant am eus da ziverkañ roll-istor ma c\\'hrogadoù";i['wantCloseAccount']="C\\'hoant am eus da serriñ ma c\\'hont";i['wantReopen']="Plijout a rafe din digeriñ ma c\\'hont en-dro";i['welcomeToUse']="Aotreañ a reomp ac\\'hanoc\\'h d\\'ober gant Lichess war bep tachenn, zoken war hini ar c\\'henwerzh.";i['whatCanWeHelpYouWith']="Penaos e c\\'hellomp ho sikour?";i['youCanLoginWithEmail']="Gallout a rit kevreañ gant ar chomlec\\'h postel arveret ganeoc\\'h evit lakaat hoc\\'h anv"})()