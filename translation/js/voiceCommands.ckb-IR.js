"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.voiceCommands)window.i18n.voiceCommands={};let i=window.i18n.voiceCommands;i['castle']="ماڵکردن (هەر لایەک)";i['instructions1']=s("دوگمەی %1$s بەکاربهێنە بۆ گۆڕینی ناسینەوەی دەنگ، دوگمەی %2$s بۆ کردنەوەی ئەم دیالۆگە یاریدەدەرە، و مینیوی %3$s بۆ گۆڕینی ڕێکخستنەکانی قسەکردن.");i['instructions2']="ئێمە تیرەکان بۆ چەندین جوڵە نیشان دەدەین کاتێک دڵنیا نین. بە فەرمانی دەنگی ڕەنگ یان ژمارەی تیرێکی جوڵە هەڵبژێرە.";i['instructions3']=s("ئەگەر تیرێک ڕادارێکی ڕژێنەر پیشان بدات، ئەوا ئەو جوڵەیە لە دوای تەواوبوونی بازنەکە دەکرێت. لەم ماوەیەدا، ڕەنگە تەنها بڵێیت %1$s بۆ ئەوەی دەستبەجێ جوڵەکە یاری پێبکەیت، %2$s بۆ هەڵوەشاندنەوە، یان ڕەنگ/ژمارەی تیرێکی جیاواز بڵێیت. ئەم تایمەرە دەتوانرێت لە ڕێکخستنەکاندا ڕێکبخرێت یان بکوژێنرێتەوە.");i['instructions4']=s("%s لە بوونی ژاوەژاودا لە دەوروبەرت چالاک بکە. لەکاتی قسەکردنی فەرمانەکاندا shift ڕابگرە کاتێک ئەمە داگیرساوە.");i['instructions5']="ئەلفوبێی فۆنێتیک بەکاربهێنە بۆ باشترکردنی ناسینەوەی ئاراستەکانی تەختەی شەترەنج.";i['instructions6']=s("%s ڕێکخستنەکانی جوڵەی دەنگ بە وردی ڕوون دەکاتەوە.");i['moveToE4OrSelectE4Piece']="بچۆ بۆ e4 یان داشی e4 هەڵبژێرە";i['selectOrCaptureABishop']="فیلێک هەڵبژێرە یان بیگرە";i['takeRookWithQueen']="بە شاژن داشی رۆک ببە";i['thisBlogPost']="ئەم بڵاوکراوەیەی بلۆگەکە";i['voiceCommands']="فەرمانە دەنگییەکان";i['watchTheVideoTutorial']="بینەری فێرکاری ڤیدیۆکە بن"})()