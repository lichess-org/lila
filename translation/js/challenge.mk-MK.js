"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.challenge)window.i18n.challenge={};let i=window.i18n.challenge;i['cannotChallengeDueToProvisionalXRating']=s("Не можеш да го предизвикаш на игра поради привремениот %s рејтинг.");i['challengeAccepted']="Предизвикот е прифатен!";i['challengeCanceled']="Предизвикот е откажан.";i['challengeDeclined']="Предизвикот е одбиен";i['challengeToPlay']="Предизвикај на Игра";i['declineCasual']="Ве молам испратете ми покана за неформална игра.";i['declineGeneric']="Не прифаќам предизвици во моментов.";i['declineLater']="Во моментов не можам да прифатам, ве молам пробајте повторно подоцна.";i['declineNoBot']="Не прифаќам предизвици од ботови.";i['declineOnlyBot']="Прифаќам предизвици само од ботови.";i['declineRated']="Ве молам испратете ми покана за рангиран предизвик.";i['declineStandard']="Во моментов не прифаќам варијантни предизвици.";i['declineTimeControl']="Не прифаќам предизвици со овој временски формат.";i['declineTooFast']="Овој временски формат е многу брз за мене, те молам предизвикај ме на побавна игра.";i['declineTooSlow']="Овој временски формат е многу бавен за мене, те молам предизвикај ме на побрза игра.";i['declineVariant']="Во моментов не сакам да ја играм оваа варијација.";i['registerToSendChallenges']="Ве молиме регистрирајте се за испраќање предизвици.";i['xDoesNotAcceptChallenges']=s("%s не прифаќа предизвици.");i['xOnlyAcceptsChallengesFromFriends']=s("%s прифаќа само предизвици од пријатели.");i['youCannotChallengeX']=s("Не можете да го предизвикате %s.");i['yourXRatingIsTooFarFromY']=s("Вашиот %1$s рејтинг е многу поголем од %2$s.")})()