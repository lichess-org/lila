"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.challenge)window.i18n.challenge={};let i=window.i18n.challenge;i['cannotChallengeDueToProvisionalXRating']=s("%s レーティングが暫定のため挑戦できません。");i['challengeAccepted']="挑戦が承認されました！";i['challengeCanceled']="挑戦がキャンセルされました。";i['challengeDeclined']="挑戦が拒否されました。";i['challengesX']=s("チャレンジ：%1$s");i['challengeToPlay']="対局を申し込む";i['declineCasual']="代わりに非レート戦での挑戦を送ってください。";i['declineGeneric']="現在、挑戦を受け付けていません。";i['declineLater']="今は都合が悪いので、後でもう一度尋ねてください。";i['declineNoBot']="ボットからの挑戦は受け付けていません。";i['declineOnlyBot']="私はボットからの挑戦しか受け付けません。";i['declineRated']="代わりにレート戦での挑戦を送ってください。";i['declineStandard']="現在、バリアントでの挑戦は受け付けていません。";i['declineTimeControl']="現在、この持時間での挑戦は受け付けていません。";i['declineTooFast']="この持時間は私には短すぎます。もっと長い持時間で挑戦してください。";i['declineTooSlow']="この持時間は私には長すぎます。もっと短い持時間で挑戦してください。";i['declineVariant']="今はこのバリアントで対戦するつもりはありません。";i['inviteLichessUser']="Lichess ユーザーを招待する：";i['registerToSendChallenges']="挑戦を送るには登録が必要です。";i['xDoesNotAcceptChallenges']=s("%s は挑戦を受け付けていません。");i['xOnlyAcceptsChallengesFromFriends']=s("%s は友達からの挑戦しか受け付けません。");i['youCannotChallengeX']=s("%s には挑戦できません。");i['yourXRatingIsTooFarFromY']=s("あなたの %1$s レーティングは %2$s と離れすぎています。")})()