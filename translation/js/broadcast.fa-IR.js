"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.broadcast)window.i18n.broadcast={};let i=window.i18n.broadcast;i['aboutBroadcasts']="درباره پخش‌های همگانی";i['addRound']="اضافه کردن یک دور";i['ageThisYear']="سنِ امسال";i['broadcastCalendar']="تقویم پخش";i['broadcasts']="پخش همگانی";i['completed']="کامل‌شده";i['completedHelp']="Lichess تکمیل دور را بر اساس بازی‌های منبع تشخیص می‌دهد. اگر منبعی وجود ندارد، از این کلید استفاده کنید.";i['credits']="به منبع اعتبار دهید";i['currentGameUrl']="نشانی بازی کنونی";i['definitivelyDeleteRound']="این دور و همه بازی‌هایش را به طور کامل حذف کن.";i['definitivelyDeleteTournament']="کل مسابقات، شامل همه دورها و بازی‌هایش را به طور کامل حذف کن.";i['deleteAllGamesOfThisRound']="همه بازی‌های این دور را حذف کن. منبع باید فعال باشد تا بتوان آنها را بازساخت.";i['deleteRound']="حذف این دور";i['deleteTournament']="حذف این مسابقات";i['downloadAllRounds']="بارگیری همه دورها";i['editRoundStudy']="ویرایش مطالعه دور";i['federation']="کشورگان";i['fideFederations']="کشورگان‌های فیده";i['fidePlayerNotFound']="بازیکن فیده پیدا نشد";i['fidePlayers']="بازیکنان فیده";i['fideProfile']="رُخ‌نمای فیده";i['fullDescription']="توضیحات کامل مسابقات";i['fullDescriptionHelp']=s("توضیحات بلند و اختیاری پخش همگانی. %1$s قابل‌استفاده است. طول متن باید کمتر از %2$s نویسه باشد.");i['howToUseLichessBroadcasts']="نحوه استفاده از پخش همگانی Lichess.";i['liveBroadcasts']="پخش زنده مسابقات";i['myBroadcasts']="پخش همگانی من";i['nbBroadcasts']=p({"one":"%s پخش همگانی","other":"%s پخش همگانی"});i['newBroadcast']="پخش زنده جدید";i['ongoing']="ادامه‌دار";i['periodInSeconds']="مدت در واحد ثانیه";i['periodInSecondsHelp']="اختیاری است، چه مدت باید بین درخواست‌ها صبر کرد. حداقل 2 ثانیه، حداکثر 60 ثانیه. بر اساس تعداد بینندگان، پیشفرض‌ها، به صورت خودکار مقدار می‌گیرند.";i['recentTournaments']="مسابقاتِ اخیر";i['replacePlayerTags']="اختیاری: عوض کردن نام، درجه‌بندی و عنوان بازیکنان";i['resetRound']="ازنوکردن این دور";i['roundName']="نام دور";i['roundNumber']="شماره دور";i['showScores']="نمایش امتیاز بازیکنان بر پایه نتیجه بازی‌ها";i['sourceGameIds']="تا ۶۴ شناسه بازی لیچس٬ جداشده با فاصله.";i['sourceSingleUrl']="وب‌نشانیِ PGN";i['sourceUrlHelp']="وب‌نشانی‌ای که Lichess برای دریافت به‌روزرسانی‌های PGN می‌بررسد. آن باید از راه اینترنت در دسترس همگان باشد.";i['startDateHelp']="اختیاری است، اگر می‌دانید چه زمانی رویداد شروع می‌شود";i['startDateTimeZone']=s("تاریخ آغاز در زمان-یانه محلی مسابقات: %s");i['subscribedBroadcasts']="پخش‌های دنبال‌شده";i['theNewRoundHelp']="دور جدید، همان اعضا و مشارکت‌کنندگان دور قبلی را خواهد داشت.";i['top10Rating']="ده درجه‌بندی برتر";i['tournamentDescription']="توضیحات کوتاه مسابقات";i['tournamentName']="نام مسابقات";i['unrated']="بی‌درجه‌بندی";i['upcoming']="آینده"})()