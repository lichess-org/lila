"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.lag)window.i18n.lag={};let i=window.i18n.lag;i['andNowTheLongAnswerLagComposedOfTwoValues']="و ئێستاش، وەڵامی درێژ! خاوبونەوەی یارییەکە لە دوو بەهای ناپەیوەندیدار پێکدێت (کەمتر باشترە):";i['isLichessLagging']="ئایە لیچێس خاوبۆتەوە?";i['lagCompensation']="قەرەبووکردنەوەی خاوبونەوە";i['lagCompensationExplanation']="lichess قەرەبووی خاوبونەوەی تۆڕ دەکاتەوە. ئەمەش خاوبونەوەی بەردەوام و ناوبەناو بەرزبوونەوەی دواکەوتن لەخۆدەگرێت. سنوور و زانیاری هەیە لەسەر بنەمای کۆنترۆڵکردنی کات و دواکەوتنی قەرەبووکراو تا ئێستا، بەجۆرێک ئەنجامەکە دەبێت هەست بە گونجاوی بکات بۆ هەردوو یاریزانەکە. لە ئەنجامدا هەبوونی خاوبونەوەی تۆڕی زیاتر لە بەرامبەرەکەت بەربەست نییە!";i['lichessServerLatency']="وەستانی سێرڤەری lichess";i['lichessServerLatencyExplanation']="ئەو کاتەی کە پێویستە بۆ کردارکردنی جوڵەیەک لەسەر سێرڤەر. بۆ هەموو کەسێک وەک یەکە، و تەنها پەیوەستە بە پەستانی سێرڤەرەکانەوە. تا یاریزان زیاتر بێت و بەرزتر بێت، بەڵام گەشەپێدەرانی lichess هەموو هەوڵێکیان دەدەن بۆ ئەوەی بە نزمیی بمێنێتەوە. بە دەگمەن لە 10ms تێدەپەڕێت.";i['measurementInProgressThreeDot']="پێوانەکان بەردەوامن...";i['networkBetweenLichessAndYou']="تۆڕی نێوان lichess و تۆ";i['networkBetweenLichessAndYouExplanation']="ئەو کاتەی کە پێویستە بۆ ناردنی گواستنەوەیەک لە کۆمپیوتەرەکەتەوە بۆ سێرڤەری لیچێس،  وەرگرتنەوەی وەڵامەکە. تایبەتە بە مەودای تۆ لە شاری lichess(فەرەنسا)، و بە کوالێتی هێڵی ئینتەرنێتەکەت. گەشەپێدەرانی lichess ناتوانن وایفایەکەت چاک بکەنەوە یان خێراتری بکەن.";i['noAndYourNetworkIsBad']="نەخێر. وە تۆڕەکەت خراپە.";i['noAndYourNetworkIsGood']="نەخێر. وە تۆڕەکەت باشە.";i['yesItWillBeFixedSoon']="بەڵێ. بەم زووانە چاک دەکرێت!";i['youCanFindTheseValuesAtAnyTimeByClickingOnYourUsername']="دەتوانیت هەردوو ئەم بەهایە لە هەر کاتێکدا بدۆزیتەوە، بە کلیککردن لەسەر ناوی بەکارهێنەرەکەت لە تووڵی سەرەوەدا."})()