"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.challenge)window.i18n.challenge={};let i=window.i18n.challenge;i['cannotChallengeDueToProvisionalXRating']=s("තාවකාලික %s ශ්‍රේණිගත කිරීම නිසා අභියෝග කළ නොහැක.");i['challengeAccepted']="අභියෝගය බාරගත්තා!";i['challengeCanceled']="අභියෝගය අවලංගු කරන ලදි.";i['challengeDeclined']="අභියෝගය ප්‍රතික්ෂේප කරනු ලැබුවා";i['challengesX']=s("අභියෝග%1$s");i['challengeToPlay']="තරඟයට​ අභියෝග කරන්න";i['declineCasual']="කරුණාකර මට ඒ වෙනුවට අනියම් අභියෝගයක් එවන්න.";i['declineGeneric']="මම මේ මොහොතේ අභියෝග භාර ගන්නේ නැහැ.";i['declineLater']="මෙය මට සුදුසු කාලය නොවේ, කරුණාකර පසුව නැවත විමසන්න.";i['declineNoBot']="මම බොට් වලින් අභියෝග භාර ගන්නේ නැහැ.";i['declineOnlyBot']="මම පිළිගන්නේ බොට් වලින් ලැබෙන අභියෝග පමණි.";i['declineRated']="කරුණාකර ඒ වෙනුවට මට ශ්‍රේණිගත අභියෝගයක් එවන්න.";i['declineStandard']="මම දැන් ප්‍රභේදන අභියෝග භාර ගන්නේ නැත.";i['declineTimeControl']="මෙම කාල පාලනය සමඟ මම අභියෝග භාර ගන්නේ නැත.";i['declineTooFast']="මෙම කාල පාලනය මට වේගවත් ය, කරුණාකර මන්දගාමී ක්‍රීඩාවක් සමඟ නැවත අභියෝග කරන්න.";i['declineTooSlow']="මෙම කාල පාලනය මට ඉතා මන්දගාමී ය, කරුණාකර වේගවත් ක්‍රීඩාවක් සමඟ නැවත අභියෝග කරන්න.";i['declineVariant']="මම දැන් මෙම ප්‍රභේදය වාදනය කිරීමට කැමති නැත.";i['inviteLichessUser']="නැත්නම් Lichess පරිශීලකයෙකුට ආරාධනා කරන්න​:";i['registerToSendChallenges']="අභියෝග යැවීමට කරුණාකර ලියාපදිංචි වන්න.";i['xDoesNotAcceptChallenges']=s("%s අභියෝග භාර නොගනී.");i['xOnlyAcceptsChallengesFromFriends']=s("%s පිළිගන්නේ මිතුරන්ගෙන් අභියෝග පමණි.");i['youCannotChallengeX']=s("ඔබට %s අභියෝග කළ නොහැක.");i['yourXRatingIsTooFarFromY']=s("ඔබගේ %1$s ශ්‍රේණිගත කිරීම %2$s ට වඩා වැඩිය.")})()