"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.coach)window.i18n.coach={};let i=window.i18n.coach;i['aboutMe']="دەربارەی من";i['accepting']="قبوڵکردنی خوێندکاران";i['areYouCoach']=s("ئایا تۆ ڕاهێنەرێکی گەورەی شەترەنجیت کە %s ـت هەیە?");i['availability']="بەردەست بوون";i['bestSkills']="باشترین لێهاتوویی";i['confirmTitle']="لێرەدا ناونیشانەکەت پشتڕاست بکەرەوە و ئێمە پێداچوونەوە بە داواکارییەکەتدا دەکەین.";i['hourlyRate']="نرخى کاتژمێرێک";i['languages']="زمان";i['lichessCoach']="ڕاهێنەری lichess";i['lichessCoaches']="ڕاهێنەرانی lichess";i['location']="شوێن";i['nmOrFideTitle']="نازناوی پاڵەوانی نیشتمانی یان نازناوێک لە فیدراسیۆنی نێودەوڵەتی شەتڕنجەوە";i['notAccepting']="قبوڵنەکردنی خوێندکار لە ئێستادا";i['otherExperiences']="ئەزموونەکانی تر";i['playingExperience']="ئەزموونی یاریکردن";i['publicStudies']="لێکۆڵینەوە گشتییەکان";i['rating']="ئاست";i['sendApplication']=s("ئیمەیڵێکمان بۆ بنێرە لە %s و ئێمە پێداچوونەوە بە داواکارییەکەت دەکەین.");i['sendPM']="نامەیەکی تایبەت بنێرە";i['teachingExperience']="ئەزموونی وانەوتنەوە";i['teachingMethod']="شێوازی وانەوتنەوە";i['viewXProfile']=s("بینینی پەڕگەی Lichess بۆ %s");i['xCoachesStudents']=s("%s ڕاهێنەری خوێندکارانی شەترەنجە");i['youtubeVideos']="ڤیدیۆکانی یوتیوب"})()