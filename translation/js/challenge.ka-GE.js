"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.challenge)window.i18n.challenge={};let i=window.i18n.challenge;i['cannotChallengeDueToProvisionalXRating']=s("ვერ გამოიწვევთ გაუმყარებელი %s რეიტინგის გამო.");i['challengeAccepted']="გამოწვევა მიღებულია!";i['challengeCanceled']="გამოწვევა გაუქმებულია.";i['challengeDeclined']="გამოწვევა უარყოფილია";i['challengesX']=s("გამოწვევები %1$s");i['challengeToPlay']="გამოიწვიე სათამაშოთ";i['declineCasual']="ჯობია ჩვეულებრივი გამოწვევა გამომიგზავნო.";i['declineGeneric']="ამ მომენტში გამოწვევებს არ ვიღებ.";i['declineLater']="ახლა არ მცალია, მოგვიანებით იყოს.";i['declineNoBot']="ბოტებისგან არ ვიღებ გამოწვევებს.";i['declineOnlyBot']="მხოლოდ ბოტებისგან ვიღებ გამოწვევებს.";i['declineRated']="ჯობია შეფასებული გამოწვევა გამომიგზავნო.";i['declineStandard']="ვარიანტის გამოწვევებს ახლა არ ვიღებ.";i['declineTimeControl']="ამ დროის კონტროლში გამოწვევებს არ ვიღებ.";i['declineTooFast']="ეს დროის კონტროლი ძალიან სწრაფია ჩემთვის, უფრო ნელში ჯობს გამომიწვიო.";i['declineTooSlow']="ეს დროის კონტროლი ძალიან ნელია ჩემთვის, უფრო სწრაფში ჯობს გამომიწვიო.";i['declineVariant']="ამ ვარიანტის თამაში ახლა არ მსურს.";i['inviteLichessUser']="ან შეთავაზე ლიჩესის მომხმარებელს:";i['registerToSendChallenges']="გთხოვთ დარეგისტრირდეთ რათა გაგზავნოთ გამოწვევა.";i['xDoesNotAcceptChallenges']=s("%s არ იღებს გამოწვევებს.");i['xOnlyAcceptsChallengesFromFriends']=s("%s მხოლოდ მეგობრებს შეუძლიიათ გამოიწვიონ.");i['youCannotChallengeX']=s("ვერ გამოიწვევთ %s-ს.");i['yourXRatingIsTooFarFromY']=s("თქვენი %1$s რეიტინგი ძალიან განსხვავდება %2$s-სგან.")})()