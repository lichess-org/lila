"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.broadcast)window.i18n.broadcast={};let i=window.i18n.broadcast;i['aboutBroadcasts']="دەربارەی پەخش";i['addRound']="زیادکردنی خولێکی تر";i['ageThisYear']="تەمەنی ئەمساڵ";i['broadcasts']="پەخشەکان.";i['completed']="تەواو بوو";i['credits']="لە سەرچاوەکە دڵنیابە";i['currentGameUrl']="لینکی یاریەکانی ئێستا";i['definitivelyDeleteRound']="بە دڵنیاییەوە خولەکە و یاریەکانی بسڕەوە.";i['definitivelyDeleteTournament']="بە دڵنیاییەوە تەواوی پاڵەوانێتییەکە و هەموو خولەکانی و هەموو یارییەکانی بسڕەوە.";i['deleteAllGamesOfThisRound']="هەموو یارییەکانی ئەم خولە بسڕەوە. سەرچاوەکە پێویستی بە چالاکبوون دەبێت بۆ ئەوەی دووبارە دروستیان بکاتەوە.";i['deleteRound']="سڕینەوەی ئەم خولە";i['deleteTournament']="ئەم پاڵەوانێتییە بسڕەوە";i['downloadAllRounds']="دابەزاندنی ھەموو خولەکان";i['editRoundStudy']="دەسکاری کردنی خولی فێربون";i['federation']="فیدراسیۆن";i['fideFederations']="فیدراسیۆنی FIDE";i['fidePlayerNotFound']="یاریزانی FIDE نەدۆزرایەوە";i['fidePlayers']="یاریزانەکانی FIDE";i['fideProfile']="پرۆفایلی FIDE";i['fullDescription']="بە گشتی باسی پالەوانێتیەکە بکە";i['fullDescriptionHelp']=s("وەسفێکی درێژی ھەڵبژاردنی پاڵەوانێتییەکە. %1$s بەردەستە. درێژی دەبێت کەمتر بێت لە کاراکتەرەکانی %2$s.");i['howToUseLichessBroadcasts']="چۆنیەتی بەکارهێنانی پەخشی لیچێس.";i['liveBroadcasts']="پەخشی ڕاستەوخۆی پاڵەوانییەتیەکان";i['nbBroadcasts']=p({"one":"%s پەخش دەکرێت","other":"%s پەخش دەکرێت"});i['newBroadcast']="پەخشێکی تازە";i['ongoing']="بەردەوامە";i['recentTournaments']="پاڵەوانێتییەکانی ئەم دواییە";i['replacePlayerTags']="ئارەزوومەندانە: گۆڕینی ناوی یاریزانان، هەڵسەنگاندن و نازناوەکان";i['resetRound']="دەسکاری کردنی ئەم خولە";i['roundName']="ناوی خولەکە";i['roundNumber']="ناوی خول";i['showScores']="نمرەکانی یاریزانان پیشان بدە بە پشتبەستن بە ئەنجامی یارییەکان";i['sourceUrlHelp']="URL مافی لی چێسە بۆ بەردەست خستنی تازەکردنەوە PGN پێویستە بۆ ھەموان بەردەست بێت لەسەر ئەنتەرنێت";i['startDateHelp']="بە ھەلبژاردنی خۆتە چ کاتێک دەس پێ بکات";i['subscribedBroadcasts']="پەخشی بەشداربووان";i['theNewRoundHelp']="خولی نوێ هەمان ئەندام و بەشداربووانی پێشووی دەبێت.";i['top10Rating']="ڕیزبەندی ١٠ باشترینەکان";i['tournamentDescription']="باسی پاڵەوانێتیەکە بکە";i['tournamentName']="ناوی پاڵەوانێتیەکە";i['unrated']="ڕیزبەندی نەکراوە";i['upcoming']="لە داھاتوو دا"})()