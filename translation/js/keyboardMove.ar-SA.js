"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.keyboardMove)window.i18n.keyboardMove={};let i=window.i18n.keyboardMove;i['bothTheLetterOAndTheDigitZero']="يمكن استخدام كل من الحرف \\\"o\\\" و الرقم صفر \\\"0\\\" عند التبييت";i['capitalizationOnlyMattersInAmbiguousSituations']="الرسملة مهمة فقط في الأوضاع الغامضة التي تتضمن فيلاً أو بيدق ال b";i['dropARookAtB4']="ضع القلعة في b4 (في نمط Crazyhouse فقط)";i['ifItIsLegalToCastleBothWays']="إذا كان التبيت قانونياً في كلا الاتجاهين، استخدم زر enter للتبيبت القصير";i['ifTheAboveMoveNotationIsUnfamiliar']="إذا كان تدوين النقل أعلاه غير مألوف، تعلم المزيد هنا:";i['includingAXToIndicateACapture']="إدراج \\\"x\\\" للإشارة إلى التقاط هو اختياري";i['keyboardInputCommands']="أوامر الادخال عبر لوحة المفاتيح";i['kingsideCastle']="تبيت قصير";i['moveKnightToC3']="حرك الحصان الى c3";i['movePieceFromE2ToE4']="حرك القطعة من e2 الى e4";i['offerOrAcceptDraw']="اعرض أو اقبل التعادل";i['otherCommands']="أوامر أخرى";i['performAMove']="قم بالنقلة";i['promoteC8ToQueen']="ترقية c8 الى وزير";i['queensideCastle']="تبيت طويل";i['readOutClocks']="اقرأ الساعات";i['readOutOpponentName']="اقرأ اسم الخصم";i['tips']="نصائح";i['toPremoveSimplyTypeTheDesiredPremove']="للتحرك المسبق، ببساطة اكتب المسبق المطلوب قبل أن يكون دورك"})()