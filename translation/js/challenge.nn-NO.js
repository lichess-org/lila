"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.challenge)window.i18n.challenge={};let i=window.i18n.challenge;i['cannotChallengeDueToProvisionalXRating']=s("Kan ikkje utfordre grunna mellombels %s rating.");i['challengeAccepted']="Utfordring akseptert!";i['challengeCanceled']="Utfordring avbroten.";i['challengeDeclined']="Utfordring avvist";i['challengesX']=s("Utfordringar: %1$s");i['challengeToPlay']="Utfordra til eit parti";i['declineCasual']="Send meg heller ei utfordring til eit uformelt parti.";i['declineGeneric']="Eg tek for tida ikkje mot utfordringar.";i['declineLater']="Det passar ikkje nett no, men spør meg gjerne seinare.";i['declineNoBot']="Eg tek ikkje utfordringar frå bottar.";i['declineOnlyBot']="Eg tek berre utfordringar frå bottar.";i['declineRated']="Utfordra meg heller til eit rangert parti.";i['declineStandard']="Eg tek ikkje utfordringar til variantar nett no.";i['declineTimeControl']="Jeg tek ikkje mot utfordringar med denne tidskontrollen.";i['declineTooFast']="Denne tidskontrollen er for snøgg for meg, men send gjerne ei ny utfordring til eit langsamare parti.";i['declineTooSlow']="Denne tidskontrollen er for langsam for meg, men send gjerne ei ny utfordring til eit snøggare parti.";i['declineVariant']="Eg ønskjer ikkje å spela denne varianten nett no.";i['inviteLichessUser']="Eller inviter ein Lichess-brukar:";i['registerToSendChallenges']="Du må registrera deg om du vil oppretta utfordringar.";i['xDoesNotAcceptChallenges']=s("%s tek ikkje i mot utfordringar.");i['xOnlyAcceptsChallengesFromFriends']=s("%s tek berre mot utfordringar frå vener.");i['youCannotChallengeX']=s("Du kan ikkje utfordra %s.");i['yourXRatingIsTooFarFromY']=s("Ratinga di i %1$s er for langt unna %2$s.")})()