"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.faq)window.i18n.faq={};let i=window.i18n.faq;i['faqAbbreviation']="FAQ";i['frequentlyAskedQuestions']="नेहमीचे प्रश्न";i['hearItPronouncedBySpecialist']="विशेषज्ञाकडून ऐका.";i['howCanIContributeToLichess']="मी लिचेस मध्ये योगदान कसे देऊ शकतो?";i['leechess']="ली-चेस";i['lichessTraining']="लीचेस प्रशिक्षण";i['similarOpponents']="समान ताकदीचे प्रतिस्पर्धी";i['threefoldRepetition']="तीनदा पुनरावृत्ती";i['threefoldRepetitionLowerCase']="तीनदा पुनरावृत्ती";i['whatIsACPL']="सरासरी सेंटीपॉन तोटा (ACPL) म्हणजे काय?";i['whyIsLichessCalledLichess']="Lichess ला Lichess का म्हणतात?"})()