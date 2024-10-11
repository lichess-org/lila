"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.swiss)window.i18n.swiss={};let i=window.i18n.swiss;i['absences']="غيبت ها";i['byes']="استراحت";i['comparison']="مقایسه";i['durationUnknown']="حداکثر تعداد دورها از قبل تعیین شده، ولی زمان نا مشخص است";i['dutchSystem']="سیستم هلندی";i['earlyDrawsAnswer']="در بازی های سوییسی بازیکنان نمیتوانند قبل از حرکت 30 ام بازی را مساوی کنند. با اینکه این اقدام نمیتواند جلوی مساوی های از پیش تعیین شده را بگیرد اما توافق بر مساوی حین عمل را سختتر میکند.";i['earlyDrawsQ']="در مورد تساوی‌های سریع چه اتفاقی می‌افتد؟";i['FIDEHandbook']="کتابچه فدراسیون جهانی شطرنج";i['forbiddedUsers']="اگر این لیست خالی نیست، بنابراین کاربرانی که در لیست نیستند امکان ورود ندارند. یک نام کاربری در هر خط.";i['forbiddenPairings']="رویارویی‌های ممنوع";i['forbiddenPairingsHelp']="نام کاربری بازیکنانی که نباید با هم بازی کنند (مثلا خواهر و برادرها). دو نام کاربری در هر خط، با فاصله از هم جدا شوند.";i['identicalForbidden']="ممنوع";i['identicalPairing']="بازی دوباره با یک فرد";i['joinOrCreateTeam']="به یک تیم بپیوندید یا یک تیم ایجاد نمایید";i['lateJoin']="ورود با تاخیر";i['lateJoinA']="بله،تا زمانی که بیش از نیمی از دورها اجرا شوند؛به عنوان مثال در یک مسابقه سوئیسی که در 11 دور برگزار می شود بازیکنان می توانند تا پیش از دور ششم حضور یابند و در مسابقه ای که در دوازده دور برگزار شود می توانند تا پیش از دور هفتم در مسابقه شرکت کنند. به بازیکنانی که دیرتر به مسابقه ملحق شوند فقط یک استراحت تعلق می گیرد حتی اگر چندین دور را از دست بدهند.";i['lateJoinQ']="آیا بازیکنان می توانند دیر تر در مسابقه شرکت کنند؟";i['lateJoinUntil']="بله تا زمانی که بیش از نیمی از دور شروع نشده باشد";i['manualPairings']="رویارویی دستی در دور پسین";i['manualPairingsHelp']="همه رویارویی‌های دور پسین را دستی مشخص کنید. یک جفت بازیکن در هر خط. نمونه:\nبازیکن‌آ بازیکن‌ب\nبازیکن‌پ بازیکن‌ت\nبرای استراحت دادن (یک امتیاز) به یک بازیکن به جای رویارویی، یک خط مانند این بیفزایید:\nبازیکن‌ث ۱\nبازیکنان نبوده، غایب در نظر گرفته می‌شوند و صفر امتیاز می‌گیرند.\nاگر این خانه را خالی گذارید، Lichess خودکار رویارویی‌ها را می‌چیند.";i['moreRoundsThanPlayersA']="هنگامی که تمام جفت های ممکن بازی شد، مسابقات به پایان می رسد و یک برنده اعلام می شود.";i['moreRoundsThanPlayersQ']="اگر در تورنمت راند ها بیشتر از بازیکنان شود چه اتفاقی میافتد؟";i['mustHavePlayedTheirLastSwissGame']="باید آخرین بازی سوییسی‌شان را کرده باشند";i['nbRounds']=p({"one":"%s دور","other":"%s دور"});i['newSwiss']="مسابقات سوئیسی جدید";i['nextRound']="دور بعد";i['nowPlaying']="در حال بازی";i['numberOfByesA']="یک بازیکن هر بار که سیستم جفت سازی نتواند جفتی برای او پیدا کند یک امتیاز دریافت می کند. علاوه بر این، زمانی که بازیکنی دیر به یک تورنمنت ملحق می شود، یک نیم امتیازی میگیرد.";i['numberOfByesQ']="یک بازیکن چند بدرود می‌تواند دریافت کند؟";i['numberOfGames']="تعداد بازی‌ها";i['numberOfGamesAsManyAsPossible']="هر تعداد که می توان در مدت زمان تعیین شده بازی کرد";i['numberOfGamesPreDefined']="از قبل تصمیم گرفته شده است، برای همه بازیکنان یکسان است";i['numberOfRounds']="تعداد دورها";i['numberOfRoundsHelp']="تعداد دورهای فرد، باعث ترازمندی رنگ (بازی با سفید و سیاه) بهینه می‌شود.";i['oneRoundEveryXDays']=p({"one":"یک دور در هر روز","other":"یک دور در هر %s روز"});i['ongoingGames']=p({"one":"بازی در جریان","other":"بازی های در جریان"});i['otherSystemsA']="در حال حاضر ما قصد اضافه کردن روش برگزاری مسابقه ی دیگری به لیچس نداریم.";i['otherSystemsQ']="در مورد سایر سیستم های مسابقه ای چطور؟";i['pairingsA']=s("با %1$s، اجرا با %2$s، مطابق با %3$s.");i['pairingsQ']="سیستم جفت سازی چطور کار میکند؟";i['pairingSystem']="روش تعیین حریف";i['pairingSystemArena']="هر حریف در دسترس با رنکینگ مشابه";i['pairingSystemSwiss']="بهترین جفت بر اساس امتیاز و امتیاز شکنی";i['pairingWaitTime']="زمان انتظار برای حریف‌یابی";i['pairingWaitTimeArena']="سریع: برای همه بازیکنان صبر نمیکند";i['pairingWaitTimeSwiss']="اهسته: منتظر ماندن برای همه بازیکنان";i['pause']="درنگ";i['pauseSwiss']="بله، اما ممکن است تعداد دورها را کاهش دهد";i['playYourGames']="بازی‌های خود را بازی کنید";i['pointsCalculationA']="به هر برد یک امتیاز و هر مساوی یک امتیاز تعلق می گیرد و باخت هیچ امتیازی ندارد.\nزمانی که یک بازیکن در یک دور هیچ حریفی نداشته باشد، یک استراحت به ایشان تعلق می گیرد که یک امتیاز به حسب می آید.";i['pointsCalculationQ']="چگونه امتیازات محاسبه میشوند؟";i['possibleButNotConsecutive']="ممکن است، اما متوالی نیست";i['predefinedDuration']="مدت زمان از پیش تعریف شده به دقیقه";i['predefinedUsers']="تنها کاربران از پیش تعریف شده اجازه ورود دارند";i['protectionAgainstNoShowA']="بازیکنانی که برای مسابقات سوییس ثبت‌نام می‌کنند اما در بازی‌هایشان شرکت نمی‌کنند٬ می‌توانند باعث ایجاد مشکلاتی شوند.\nبرای رفع این مشکل٬ لیچس بازیکنانی را که در بازی شرکت نمی‌کنند از شرکت در دیگر مسابقات سوییس برای مدت زمان مشخصی محروم می‌کند. \nبرگزارکننده یک مسابقه سوییس می‌تواند اجازه حضور در مسابقه را٬ علیرغم این محرومیت٬ برای این بازیکنان صادر کند.";i['protectionAgainstNoShowQ']="در موارد عدم حضور چه اتفاقی می‌افتد؟";i['restrictedToTeamsA']="مسابقات به شیوه سوئیسی برای شطرنج آنلاین طراحی نشده اند. آن ها نیازمند وقت شناسی، فداکاری و صبر از جانب بازیکنان هستند.\nما فکر می کنیم که این شرایط در تیم بیشتر از مسابقات جهانی بر آورده می شود.";i['restrictedToTeamsQ']="چرا محدود به تیم هاست؟";i['roundInterval']="فاصله بین دورها";i['roundRobinA']="دوست داریم که اضافه کنیم اما متاسفانه راند رابین بصورت آنلاین کار نمیکند.\nدلیل آن این است که هیچ راه عادلانه ای برای برخورد با افرادی که زودتر از موعد مسابقات را ترک می کنند، ندارد. ما نمی توانیم انتظار داشته باشیم که همه بازیکنان تمام بازی های خود را در یک رویداد آنلاین انجام دهند. این اتفاق نمی افتد، و در نتیجه در اکثر مسابقات راند رابین ناقص و ناعادلانه خواهند بود، که مخالف دلیل وجود آن است. \nنزدیک ترین چیزی که می توانید به راند رابین آنلاین داشته باشید، بازی در یک تورنمنت سوئیس با تعداد راندهای بسیار بالا است. سپس تمام جفت های ممکن قبل از پایان مسابقات انجام می شود.";i['roundRobinQ']="دوره‌ای چطور؟";i['roundsAreStartedManually']="دورها به صورت دستی مشخص می‌شوند";i['similarToOTB']="مشابه مسابقات بر روی تخته";i['sonnebornBergerScore']="امتیاز Sonneborn–Berger";i['startingIn']="شروع در";i['startingSoon']="بزودی شروع می‌شود";i['streaksAndBerserk']="توالی و جنون";i['swiss']="سؤيسى";i['swissDescription']=s("در یک تورنمنت سوئیسی %1$s، هر شرکت کننده لزوماً با سایر شرکت کنندگان بازی نمی کند. رقبا در هر دور یک به یک بازی میکنند و با استفاده از مجموعه‌ای از قوانین طراحی‌شده برای اطمینان از اینکه هر رقیب با حریفانی با امتیاز مشابه بازی می‌کند، جفت می‌شوند، اما با هر حریف بیشتر از یک بار نه. برنده مسابقه کسی هست که بیشترین امتیاز را در همه راندها کسب کرده باشد. همه باهم در هر دور بازی می کنند مگر اینکه تعداد بازیکنان فرد وجود داشته باشد.\\\"");i['swissTournaments']="مسابقات سوئیسی";i['swissVsArenaA']="در مسابقه با فرم سوئیسی، تمام شرکت کننده ها به تعداد برابر بازی انجام می دهند و هر دو بازیکن فقط یک بار با یکدیگر بازی می کنند.\nاین شکل از مسابقه می تواند گزینه مناسبی برای باشگاه ها و مسابقات رسمی باشد.";i['swissVsArenaQ']="چه زمانی از مسابقات با ساختار سوئیسی به جای آرنا استفاده کنیم؟";i['teamOnly']=s("مسابقات سوئیسی تنها میتوانند توسط رهبران تیم ایجاد شود، و تنها توسط اعضای تیم بازی شود. \n%1$s تا شروع بازی در مسابقات سوئیسی.");i['tieBreak']="تساوی‌شکنی";i['tiebreaksCalculationA']=s("با %s .\nامتیازهای هر حریفی را که بازیکن شکست می دهد و نیمی از امتیاز هر بازی که مساوی میشود را اضافه کنید.");i['tiebreaksCalculationQ']="امتیاز شکنی چطور محاسبه میشود؟";i['tournDuration']="مدت زمان مسابقه";i['tournStartDate']="تاریخ آغاز مسابقات";i['unlimitedAndFree']="بدون محدودیت و رایگان";i['viewAllXRounds']=p({"one":"این دور را ببینید","other":"همه %s دور را ببینید"});i['whatIfOneDoesntPlayA']="بازی ادامه پیدا میکند تا در نهایت تسلیم شده و بازی را واگذار کنند. سپس سیستم بازیکن را از مسابقات خارج می کند تا بازی های بیشتری را نبازند.\nآنها هر زمانی که خواستند میتوانند به مسابقه برگردند.";i['whatIfOneDoesntPlayQ']="اگر یک بازیکن بازی نکند چه اتفاقی میافتد؟";i['willSwissReplaceArenasA']="خیر. آنها ویژگی های مکمل هستند.";i['willSwissReplaceArenasQ']="آیا مسابقات سوئیسی می توانند جایگزین آرنا شوند?";i['xMinutesBetweenRounds']=p({"one":"%s دقیقه بین دورها","other":"%s دقیقه بین دورها"});i['xRoundsSwiss']=p({"one":"%s دور سوئیسی","other":"%s دور سوئیسی"});i['xSecondsBetweenRounds']=p({"one":"%s ثانیه بین دورها","other":"%s ثانیه بین دورها"})})()