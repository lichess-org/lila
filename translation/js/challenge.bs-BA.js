"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.challenge)window.i18n.challenge={};let i=window.i18n.challenge;i['cannotChallengeDueToProvisionalXRating']=s("Nemoguće izazvati zbog privremenog %s rejtinga.");i['challengeAccepted']="Izazov prihvaćen!";i['challengeCanceled']="Izazov otkazan.";i['challengeDeclined']="Izazov odbijen";i['challengesX']=s("Izazovi: %1$s");i['challengeToPlay']="Izazov na partiju";i['declineCasual']="Umjesto toga, pošaljite mi usputni izazov.";i['declineGeneric']="Trenutno ne prihvatam izazove.";i['declineLater']="Ne odgovara mi ovaj trenutak; molim, izazovite me ponovo kasnije.";i['declineNoBot']="Ne prihvatam izazove od botova.";i['declineOnlyBot']="Prihvatam izazove samo od botova.";i['declineRated']="Umjesto toga, pošaljite mi ocijenjeni izazov.";i['declineStandard']="Trenutno ne prihvatam varijantne izazove.";i['declineTimeControl']="Ne prihvatam izazove sa ovom kontrolom vremena.";i['declineTooFast']="Ova kontrola vremena je prebrza za mene, molim vas ponovo izazovite sporijom igrom.";i['declineTooSlow']="Ova kontrola vremena je prespora za mene, molim vas ponovo izazovite bržom igrom.";i['declineVariant']="Trenutno nisam voljan da igram ovu varijantu.";i['inviteLichessUser']="Ili pozovite korisnika Lichessa:";i['registerToSendChallenges']="Molimo, registrirajte se da biste mogli slati izazove.";i['xDoesNotAcceptChallenges']=s("%s ne prihvata izazove.");i['xOnlyAcceptsChallengesFromFriends']=s("%s prihvata samo izazove od prijatelja.");i['youCannotChallengeX']=s("Ne možete izazvati %s.");i['yourXRatingIsTooFarFromY']=s("Vaš %1$s rejting predaleko je od %2$s.")})()