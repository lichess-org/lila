"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.streamer)window.i18n.streamer={};let i=window.i18n.streamer;i['allStreamers']="جميع الستريمرز";i['approved']="لقد تمت الموافقة على البث الخاص بك.";i['becomeStreamer']="اصبح ستريمرز لموقع الليتش";i['changePicture']="تغيير/حذف صورتك";i['currentlyStreaming']=s("يبث حاليا: %s");i['downloadKit']="تحميل streamer kit";i['doYouHaveStream']="هل لديك قناة تويتش أو يوتيوب؟";i['editPage']="تعديل صفحة البث";i['headline']="العنوان";i['hereWeGo']="فلنبدأ!";i['keepItShort']=p({"zero":"ابقه قصيرًا: %s حرف أقصى","one":"ابقه قصيرًا: حرف واحد كحد أقصى","two":"ابقه قصيرًا: حرفان كحد أقصى","few":"ابقه قصيرًا: %s حروف كحد أقصى","many":"ابقه قصيرًا: %s حرف أقصى","other":"ابقه قصيرًا: %s حرف كحد أقصى"});i['lastStream']=s("آخر بث %s");i['lichessStreamer']="بثوث ليشس";i['lichessStreamers']="بثوث ليشس";i['live']="مباشر!";i['longDescription']="وصف مطول";i['maxSize']=s("الحجم الأقصى: %s");i['offline']="غير متصل";i['optionalOrEmpty']="اختياري. اتركه فارغاً إذا لم يكن هناك";i['pendingReview']="تتم حاليا مراجعة البث الخاص بك من قبل المشرفين.";i['perk1']="احصل على أيقونة بث مشتعلة على ملفك الشخصي \\\"Lichess\\\".";i['perk2']="احجز مكانك مباشرة في أعلى القائمة الخاصة بالستريمرز.";i['perk3']="أعلِم متابعيك على Lichess.";i['perk4']="أظهر البث الخاص بك في المباريات والبطولات التي تلعبها إضافة إلى الدراسات.";i['perks']="فوائد البث باستعمال الكلمة المفتاحية";i['pleaseFillIn']="الرجاء ملء معلومات البث الخاصة بك، و قم برفع صورة.";i['requestReview']="أطلب مراجعة مشرف";i['rule1']="قم بإدراج الكلمة المفتاحية \\\"lichess.org\\\" في عنوان البث الخاص بك ولا تنسى كذلك تصنيفه تحت فئة \\\"Chess\\\".";i['rule2']="قم بنزع الكلمة المفتاحية عندما تبث أمور غير متعلقة بليشيس.";i['rule3']="سيقوم ليشيس بالتعرف على بثك بصورة آلية ويمنحه الإمتيازات التالية:";i['rule4']=s("اقرأ %s لضمان شروط اللعب العادل خلال بثك المباشر.");i['rules']="قواعد البث";i['streamerLanguageSettings']="صفحة البث Lichess تستهدف جمهورك مع اللغة التي توفرها منصة البث الخاصة بك. قم بتعيين اللغة الافتراضية الصحيحة لبث الشطرنج الخاص بك في التطبيق أو الخدمة التي تستخدمها للبث.";i['streamerName']="اسم البث الخاص بك على Lichess";i['streamingFairplayFAQ']="الاسئلة الشائعة للعب العادل أثناء البث";i['tellUsAboutTheStream']="أخبرنا عن البث الخاص بك في جملة واحدة";i['twitchUsername']="اسم مستخدم تويتش أو عنوان URL الخاص بك";i['uploadPicture']="رفع صورة";i['visibility']="إظهار على قائمة بثوث اللاعبين";i['whenApproved']="بعد الموافقة من قبل المشرفين";i['whenReady']=s("عندما تكون جاهزاً لتصبح ستريمر Lichess ، %s");i['xIsStreaming']=s("%s يقوم بعمل بث");i['xStreamerPicture']=s("%s صورة الستريمر");i['yourPage']="صفحة البث الخاصة بك";i['youTubeChannelId']="معرف قناتك على اليوتيوب"})()