"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.challenge)window.i18n.challenge={};let i=window.i18n.challenge;i['cannotChallengeDueToProvisionalXRating']=s("Nu se poate provoca datorită scorului provizoriu la %s.");i['challengeAccepted']="Provocare acceptată!";i['challengeCanceled']="Provocare anulată.";i['challengeDeclined']="Provocare refuzată";i['challengesX']=s("Provocări: %1$s");i['challengeToPlay']="Provoacă la o partidă";i['declineCasual']="Te rog să îmi trimiți în loc o provocare neoficială.";i['declineGeneric']="Nu accept provocări la momentul actual.";i['declineLater']="Nu este timpul potrivit, te rog întreabă mai târziu.";i['declineNoBot']="Nu accept provocările roboților.";i['declineOnlyBot']="Accept doar provocări de la roboți.";i['declineRated']="Te rog să îmi trimiți în loc o provocare oficială.";i['declineStandard']="Nu accept provocări de variantă acum.";i['declineTimeControl']="Nu accept provocări cu acest timp.";i['declineTooFast']="Acest timp este prea rapid pentru mine, te rog să mă provoci cu un timp mai lent.";i['declineTooSlow']="Acest timp este prea lent pentru mine, te rog să mă provoci cu un timp mai rapid.";i['declineVariant']="Nu sunt dispus să joc această variantă acum.";i['inviteLichessUser']="Sau invitați un utilizator Lichess:";i['registerToSendChallenges']="Te rugăm să te înregistrezi pentru a putea trimite provocări.";i['xDoesNotAcceptChallenges']=s("%s nu acceptă provocări.");i['xOnlyAcceptsChallengesFromFriends']=s("%s acceptă provocări doar de la prieteni.");i['youCannotChallengeX']=s("Nu poți să îl provoci pe %s.");i['yourXRatingIsTooFarFromY']=s("Scorul tău pe %1$s este prea mare față de %2$s.")})()