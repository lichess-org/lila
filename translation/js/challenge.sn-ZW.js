"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.challenge)window.i18n.challenge={};let i=window.i18n.challenge;i['cannotChallengeDueToProvisionalXRating']=s("Haukwanisi kuChallenger nepamusaka pezera iri %s.");i['challengeAccepted']="\\\"Abvuma Kutamba newe!\\\"!";i['challengeCanceled']="Mutambo wavhariswa.";i['challengeDeclined']="Aramba kutamba newe";i['challengeToPlay']="Dana munhu kumutambo";i['declineCasual']="Ndokumbirawo ugadzire game risina zvemaRating since tirikutamba friendly.";i['declineGeneric']="Handisi kuda kutamba nevanhu parizvino.";i['declineLater']="Nguva yaurikuda kuti titambe haiite, wozonditsvaga imwe nguva.";i['declineNoBot']="Handidi zvekutamba nemaBots ini.";i['declineOnlyBot']="Ndirikungoda kutamba nemaBots chete.";i['declineRated']="Ndokumbirawo ugadzire rimwe game rekuti kana rapera maRatings edu anochinja.";i['declineStandard']="Ikozvino handisi kutamba maVariant Challenges wangu.";i['declineTimeControl']="Handitambe maGame anopera nenguva yawaisa iyo.";i['declineTooFast']="Mutambo wawaisa unoita kuti titambe nekukasika ende ini handikwanise kukasika kudaro, ndokumbirawo ugadzire rimwe game rine nguva yakati wandei.";i['declineTooSlow']="Game iri rakarebesa wangu, ndokumbirawo ugadzire umwe mutambo unopera nekukasika.";i['declineVariant']="Handisi kuda kumba variant iyi izvezvi.";i['registerToSendChallenges']="Joina Lichess kana uchida kumba neMutambi uyu.";i['xDoesNotAcceptChallenges']=s("%s haagamuchiri maChallenges.");i['xOnlyAcceptsChallengesFromFriends']=s("%s anongotamba neshamwari dzake chete.");i['youCannotChallengeX']=s("Haukwanisi kutamba na %s.");i['yourXRatingIsTooFarFromY']=s("Zera rako re %1$s ririkure zvisingaite ne %2$s.")})()