"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.class)window.i18n.class={};let i=window.i18n.class;i['addLichessUsernames']="ناوی بەکارهێنەری لیچێس زیاد بکە بۆ ئەوەی وەک مامۆستا بانگهێشتیان بکەیت. یەک دانە بۆ هەر دێڕێک.";i['addStudent']="زیادکردنی خوێندکار";i['aLinkToTheClassWillBeAdded']="لە کۆتایی نامەکەدا بە شێوەیەکی خۆکارانە بەستەرێک بۆ پۆلەکە زیاد دەکرێت، بۆیە پێویست ناکات خۆت ئەو کارە بکەیت.";i['anInvitationHasBeenSentToX']=s("بانگهێشتنامەکە نێردا بۆ %s");i['applyToBeLichessTeacher']="داواکاری پێشکەش بکە بۆ ئەوەی ببیتە مامۆستای لیچێس";i['classDescription']="کورتە باسێکی پۆلەکە";i['className']="ناوی پۆل";i['classNews']="هەواڵی پۆل";i['clickToViewInvitation']="کلیک لەسەر بەستەرەکە بکە بۆ ئەوەی بانگهێشتنامەکە ببینی:";i['closeClass']="داخستنی پۆل";i['closeDesc1']="جارێکی تر خوێندکار ناتوانێت ئەم ئەکاونتە بەکاربهێنێت. داخستنی کۆتاییە. دڵنیابە کە خوێندکارەکە تێگەیشتووە و ڕەزامەندە.";i['closeDesc2']="دەکرێ لەبری ئەوە کۆنترۆڵی ئەکاونتەکە بدەیت بە خوێندکارەکە، بۆ ئەوەی بتوانێت بەردەوام بێت لە بەکارهێنانی.";i['closeStudent']="داخستنی هەژمار";i['closeTheAccount']="داخستنی هەژماری خوێندکار بۆ هەمیشە.";i['createANewLichessAccount']="هەژمارێکی نوێی لیچێس دروست بکە";i['createDesc1']="ئەگەر خوێندکارەکە هێشتا هەژماری لیچێسی نییە، دەتوانیت لێرەدا هەژمارێکی بۆ دروست بکەیت.";i['createDesc2']="ناونیشانی ئیمێڵ پێویست نییە. وشەی نهێنی دروست دەکرێت، و دەبێت بیدەیت بە خوێندکارەکە، بۆ ئەوەی بتوانێت بچێتە ژوورەوە.";i['createDesc3']="گرنگ: نابێت خوێندکار زیاد لە هەژمارێکی هەبێت.";i['createDesc4']="ئەگەر پێشتر هەژماری دروستکردوە، ئەوا فۆڕمی بانگهێشتکردن بەکاربێنە.";i['createMoreClasses']="پۆلی زۆرتر دروستبکە";i['createMultipleAccounts']="چەند هەژمارێکی زۆری لیچێس بە یەکجار دروستبکە";i['createStudentWarning']="تەنها ئەکاونت بۆ خوێندکارە ڕاستەقینەکان دروست بکە. ئەمە بەکارمەهێنە بۆ دروستکردنی چەند ئەکاونتێک بۆ خۆت. دواتر بان دەکرێیت.";i['editNews']="دەستکاریکردنی هەواڵ";i['features']="تایبەتمەندیەکان";i['freeForAllForever']="100% بەخۆڕاییە بۆ هەمووان، بۆ هەمیشە، بەبێ هیچ ڕیکلام و شوێنپێهەڵگرێک";i['generateANewPassword']="وشەی نهێنی نوێ بۆ خوێندکار دروست بکە";i['generateANewUsername']="ناوی بەکارهێنەرێکی نوێ دروست بکە";i['invitationToClass']=s("تۆ بانگهێشتکراوی بۆ بەشداریکردن لە پۆلی \\\"%s\\\" وەک a خوێندکار.");i['invite']="بانگهێشتکردن";i['inviteALichessAccount']="ئەکاونتێکی لیچێس بانگهێشت بکە";i['inviteDesc1']="ئەگەر خوێندکارەکە پێشتر ئەکاونتی لیچێسی هەیە، دەتوانیت بانگهێشتی پۆلەکەی بکەیت.";i['inviteDesc2']="پەیامێک لە لیچێس وەردەگرن کە لینکی بەشداریکردنی لە پۆلەکەدا لەگەڵە.";i['inviteDesc3']="گرنگ: تەنها ئەو خوێندکارانە بانگهێشت بکە کە دەیانناسیت، و چالاکانە دەیانەوێت بەشداری پۆلەکە بکەن.";i['inviteDesc4']="هەرگیز بانگهێشتی بێ داواکراو مەنێرە بۆ یاریزانە ئارەزوومەندەکان.";i['invitedToXByY']=s("بانگهێشتکرا بۆ %1$s لە لایەن %2$s");i['inviteTheStudentBack']="خوێندکارەکە بانگهێشت بکەرەوە";i['lastActiveDate']="چالاک";i['lichessClasses']="پۆلەکان";i['lichessProfileXCreatedForY']=s("پڕۆفایلی لیچێس %1$s دروستکرا بۆ %2$s.");i['lichessUsername']="ناوی بەکارهێنەری لیچێس";i['makeSureToCopy']="دڵنیابە لەوەی وشەی نهێنیت لایخۆت تۆمارکردووە. جارێکی تر ناتوانیت بیبینیتەوە!";i['managed']="بەڕێوەدەبرێت";i['maxStudentsNote']=s("تێبینی بکە کە پۆلێک دەتوانێت تا %1$s خوێندکاری هەبێت. بۆ بەڕێوەبردنی خوێندکاری زیاتر، %2$s.");i['messageAllStudents']="پەیام بۆ هەموو خوێندکاران بنێرە دەربارەی مادەی نوێی ناو پۆلەکەت";i['multipleAccsFormDescription']=s("هەروەها دەتوانیت %s بکەیت بۆ دروستکردنی چەندین ئەکاونتی لیچێس لە لیستی ناوی خوێندکاران.");i['na']="بەردەست نییە";i['nbPendingInvitations']=p({"one":"یەک بانگهێشتنامەی چاوەڕوانکراو","other":"%s بانگهێشتنامەی چاوەڕوانکراو"});i['nbStudents']=p({"one":"خوێندکار","other":"%s خوێندکار"});i['nbTeachers']=p({"one":"مامۆستا","other":"%s مامۆستا"});i['newClass']="پۆلی نوێ";i['news']="هەواڵ";i['newsEdit1']="هەموو هەواڵەکانی پۆل لە یەک شوێن.";i['newsEdit2']="لە سەرەوە دواین هەواڵ زیاد بکە. هەواڵی پێشوو مەسڕەوە.";i['newsEdit3']="هەواڵەکان جیابکەوە بە ---\nبۆ ئەوەی بە هێڵێکی ئاسۆیی نیشانی بدات.";i['noClassesYet']="هێشتا هیچ پۆلێک نییە.";i['noRemovedStudents']="هیچ خوێندکارێک لا نەبراوە.";i['noStudents']="هێشتا هیچ خوێندکارێک لە پۆل نییە.";i['nothingHere']="هێشتا هیچ شتێک نییە.";i['notifyAllStudents']="سەرجەم خوێندکاران ئاگادار بکەوە";i['onlyVisibleToTeachers']="تەنها لای مامۆستایانی پۆلەکە دەردەکەوێت";i['orSeparator']="یان";i['overDays']="بە تێپەڕبوونی چەند ڕۆژێک";i['overview']="تێڕوانینێکی گشتی";i['passwordX']=s("وشەی نهێنی: %s");i['privateWillNeverBeShown']="تایبەت. هەرگیز لە دەرەوەی پۆل نیشان نادرێت. یارمەتیت دەدات لەبیرت بێت کە خوێندکارەکە کێیە.";i['progress']="ئاستی پێشکەوتن";i['quicklyGenerateSafeUsernames']="دروستکردنی ناوی بەکارهێنەر و وشەی نهێنی پارێزراو بۆ خوێندکاران";i['realName']="ناوی راستەقینەت";i['realUniqueEmail']="ناونیشانی ئیمەیڵی ڕاستەقینەی خوێندکار. ئێمە ئیمەیڵێکی پشتڕاستکردنەوەی بۆ دەنێرین، لەگەڵ لینکی دەرچوون لە ئەکاونتەکە.";i['release']="دەرچوو";i['releaseDesc1']="ئەکاونتێکی دەرچووی پۆلەکە ناتوانرێت جارێکی تر بەڕێوەببرێتەوە. خوێندکار دەتوانێت خۆی دۆخی منداڵان بگۆڕێت و وشەی نهێنی رێکبخاتەوە.";i['releaseDesc2']="خوێندکار دوای دەرچوونی ئەکاونتەکەی لە پۆلەکەدا دەمێنێتەوە.";i['releaseTheAccount']="کە هەژمارەکە دەرچوو، خوێندکارەکە دەتوانێت سەربەخۆ هەژمارەکەی خۆی بەڕێوە ببات.";i['removedByX']=s("لەلایەن %s لابرا");i['removedStudents']="لابردن";i['removeStudent']="لابردنی خوێندکار";i['reopen']="دووبارە بیکەوە";i['resetPassword']="وشەی نهێنی ڕێکبخەرەوە";i['sendAMessage']="نامە بۆ سەرجەم قوتابیان بنێرە.";i['studentCredentials']=s("خوێندکار:  %1$s\nناوی بەکارهێنەر: %2$s\nوشەی نهێنی: %3$s");i['students']="خوێندکارەکان";i['studentsRealNamesOnePerLine']="ناوی راستەقینەی خوێندکار، بۆ هەر دێڕێک یەک ناو";i['teachClassesOfChessStudents']="بە بەکارهێنانی ئامرازەکانی پۆلی لیچێس پۆلێک لە خوێندکارانی شەترەنج فێر بکە.";i['teachers']="مامۆستایان";i['teachersOfTheClass']="مامۆستایانی پۆلەکە";i['teachersX']=s("مامۆستایان: %s");i['thisStudentAccountIsManaged']="هەژماری ئەو خوێندکارە بەڕێوەدەبردرێت";i['timePlaying']="کاتی یاری";i['trackStudentProgress']="بەدواداچوون بۆ پێشکەوتنی ئاستی خوێندکار لە یاری و مەتەڵەکاندا";i['upgradeFromManaged']="بەرزکردنەوە بۆ هەژمارێکی سەربەخۆ";i['useThisForm']="ئەم فۆرمە بەکاربێنە";i['variantXOverLastY']=s("%1$s لە ماوەی کۆتا %2$s");i['visibleByBothStudentsAndTeachers']="لالەیەن هەردوولا، مامۆستایان و خوێندکارانی پۆل دەردەکەوێت";i['welcomeToClass']=s("بەخێربێن بۆ پۆلەکەت: %s.\nئەمە لینکی دەستگەیشتنە بە پۆلەکە.");i['winrate']="ڕێژەی براوە";i['xAlreadyHasAPendingInvitation']=s("%s بانگهێشتنامەی بۆکراوە");i['xIsAKidAccountWarning']=s("%1$s ئەکاونتێکی منداڵانە و ناتوانێت نامەکەت وەربگرێت. پێویستە بە دەستی بەستەری بانگهێشتکردنیان پێ بدەیت: %2$s");i['xisNowAStudentOfTheClass']=s("%s ئێستا خوێندکاری پۆلەکەیە");i['youAcceptedThisInvitation']="ئەم بانگهێشتنامەیەت قبوڵکرد.";i['youDeclinedThisInvitation']="ئەم بانگهێشتنامەیەت رەتکردەوە.";i['youHaveBeenInvitedByX']=s("تۆ بانگهێشتکراویت لەلایەن %s.")})()