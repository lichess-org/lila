"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.study)window.i18n.study={};let i=window.i18n.study;i['addMembers']="A’zo qo‘shish";i['addNewChapter']="Yangi bo\\'lim qo\\'shish";i['allowCloning']="Klonlash uchun ruhsat";i['allStudies']="Barcha ta\\'limlar";i['allSyncMembersRemainOnTheSamePosition']="Barcha SYNC a\\'zolari o\\'sha joyida saqlanib qoladi";i['alphabetical']="Alfavitli";i['analysisMode']="Tahlil qilish holati";i['annotateWithGlyphs']="Gliflar bilan izohlash";i['attack']="Hujum";i['automatic']="Avtomatik";i['back']="Ortga";i['blackIsBetter']="Qoralarda vaziyat zo‘r";i['blackIsSlightlyBetter']="Qoralarda vaziyat yaxshiroq";i['blackIsWinning']="Qoralar yutayabdi";i['blunder']="Qo\\'pol xato";i['brilliantMove']="Zo‘r yurish";i['chapterPgn']="PGN bo\\'limi";i['chapterX']=s("%s bo\\'limi");i['clearAllCommentsInThisChapter']="Ushbu bo\\'limdagi barcha kommentlar, rasmlar, sipohlar, shakllar tozalansinmi?";i['clearAnnotations']="Annotatsiyalarni tozalash";i['clearChat']="Suhbatni tozalash";i['clearVariations']="Variantlarni tozalash";i['cloneStudy']="Klonlash";i['commentThisMove']="Ushbu yurishga izoh berish";i['commentThisPosition']="Ushbu holatga izoh berish";i['confirmDeleteStudy']=s("Ta‘limni to‘laligicha o‘chirilsinmi? Ortga yo‘l yo‘q! Tasdiqlash uchun %s ta‘lim nomini kiriting");i['contributor']="Yordamchi";i['contributors']="Yordamchilar";i['copyChapterPgn']="PGN ni nusxalash";i['counterplay']="Qarshi o‘yin";i['createChapter']="Bo\\'lim yaratish";i['createStudy']="Ta\\'lim yaratish";i['currentChapterUrl']="Joriy bo\\'lim URL i";i['dateAddedNewest']="Qo\\'shilgan vaqt (yangiroq)";i['dateAddedOldest']="Qo\\'shilgan vaqt (eskiroq)";i['deleteChapter']="Bo\\'limni o\\'chirish";i['deleteStudy']="Ta\\'limni o\\'chirish";i['deleteTheStudyChatHistory']="Ta\\'lim suhbatini o\\'chirilsinmi? Bundan keyin uni ortga qaytarib bo\\'lmaydi!";i['deleteThisChapter']="Ushbu bo\\'lim o\\'chirilsinmi? Amalni ortga qaytarib bo\\'lmaydi!";i['development']="Ishlab chiqish";i['downloadAllGames']="Barcha oʻyinlarni yuklab olish";i['downloadGame']="Oʻyinni yuklab olish";i['dubiousMove']="Shubhali yurish";i['editChapter']="Bo\\'limni tahrirlash";i['editor']="Tahrirlovchi";i['editStudy']="Ta\\'limni tahrirlash";i['embedInYourWebsite']="Oʻz websaytingizda yoki blogingizda joylang";i['empty']="Bo‘sh";i['enableSync']="Sinxronizatsiya yoqilgan";i['equalPosition']="Teng holat";i['everyone']="Har kim";i['first']="Birinchi";i['getAFullComputerAnalysis']="Asosiy qatorni server tomonidagi tahlilini olish.";i['goodMove']="Yaxshi yurish";i['hideNextMoves']="Navbatdagi yurishni yashirish";i['hot']="Qaynoq";i['importFromChapterX']=s("%s dan import qilish");i['initiative']="Initsiativa";i['interactiveLesson']="Faol dars";i['interestingMove']="Qiziq yurish";i['inviteOnly']="Faqat taklif";i['inviteToTheStudy']="Ta\\'lim uchun chorlash";i['kick']="Ro\\'yhatdan chiqarish";i['last']="Oxirgi";i['leaveTheStudy']="Ta\\'limni tark etish";i['like']="Yoqdi";i['loadAGameByUrl']="O\\'yinni URL dan o\\'qib olish";i['loadAGameFromPgn']="PGN dan o\\'yinni o\\'qib olish";i['loadAGameFromXOrY']=s("%1$s yoki %2$s dan o\\'yinni yuklash");i['loadAPositionFromFen']="Boshlang\\'ich hiolatni FEN dan yuklab olish";i['makeSureTheChapterIsComplete']="Bo\\'lim tugaganiga ishonch xosil qiling. Siz bir marta so\\'rov yuborishingiz mumkin.";i['manageTopics']="Mavzularni boshqarish";i['members']="A\\'zolar";i['mistake']="Xato";i['mostPopular']="Eng ommabop";i['myFavoriteStudies']="Mening yoqtirgan ta\\'limlarim";i['myPrivateStudies']="Mening yashirin ta\\'limlarim";i['myPublicStudies']="Mening ochiq ta\\'limlarim";i['myStudies']="Mening ta\\'limlarim";i['myTopics']="Mening mavzularim";i['nbChapters']=p({"one":"%s bo\\'lim","other":"%s bo\\'limlar"});i['nbGames']=p({"one":"%s o\\'yin","other":"%s o\\'yinlar"});i['nbMembers']=p({"one":"%s a\\'zo","other":"%s a\\'zolar"});i['newChapter']="Yangi bo\\'lim";i['newTag']="Yangi yorliq";i['next']="Keyingi";i['nextChapter']="Navbatdagi bo‘lim";i['nobody']="Hech kim";i['noLetPeopleBrowseFreely']="Yo\\'q: odamlarga bemalol sharhlashga imkon berilsin";i['noneYet']="Xozircha yo\\'q.";i['noPinnedComment']="Hech qayerda";i['normalAnalysis']="Normal tahlil";i['novelty']="Yangi";i['onlyContributorsCanRequestAnalysis']="Faqat ta\\'lim ishtirokchilari kompyuter tahlilini so\\'rashi mumkin.";i['onlyMe']="Faqat menga";i['onlyMove']="Yagona yurish";i['onlyPublicStudiesCanBeEmbedded']="Faqat ochiq taʻlimlar joylanishi mumkin!";i['open']="Ochish";i['orientation']="Joylashuv";i['pasteYourPgnTextHereUpToNbGames']=p({"one":"%s o\\'yinni yuklash uchun PGN matnni shu yerga qo\\'ying","other":"%s o\\'yinlarni yuklash uchun PGN matnni shu yerga qo\\'ying"});i['pgnTags']="PGN yorliqlar";i['pinnedChapterComment']="Bo\\'lim mahkamlangan kommenti";i['pinnedStudyComment']="Ta\\'lim mahkamlangan kommenti";i['playAgain']="Qayta o‘ynash";i['playing']="O\\'ynash";i['pleaseOnlyInvitePeopleYouKnow']="Iltimos faqat o\\'zingiz taniydigan odamlarni va haqiqatdan ham qo\\'shilishni xohlayotganlarni ta\\'limga qo\\'shing.";i['popularTopics']="Nomdor mavzular";i['prevChapter']="Avvalgi bo‘lim";i['previous']="Oldingi";i['private']="Yashirin";i['public']="Ommaviy";i['readMoreAboutEmbedding']="Joylash haqida koʻproq oʻqing";i['recentlyUpdated']="Yaqinda yangilangan";i['rightUnderTheBoard']="Shunday doskani tagida";i['save']="Saqlash";i['saveChapter']="Bo\\'limni saqlash";i['searchByUsername']="Foydalanuvchi nomi bo\\'yicha qidirish";i['shareAndExport']="Ulashish & eksport";i['shareChanges']="O\\'zgarishlarni serverda saqlab juzatuvchilar bilan baham ko\\'ring";i['spectator']="Kuzatuvchi";i['start']="Boshlash";i['startAtInitialPosition']="Boshlang\\'ich holatdan boshlash";i['startAtX']=s("%s da boshlash");i['startFromCustomPosition']="Ko\\'rsatilgan holatdan boshlash";i['startFromInitialPosition']="Boshlang\\'ich holatdan boshlash";i['studiesCreatedByX']=s("%s tomonidan yaratilgan ta\\'limlar");i['studiesIContributeTo']="Men ishtirok etayotgan ta\\'limlar";i['studyActions']="Ta‘lim harakatlari";i['studyNotFound']="Ta\\'lim topilmadi";i['studyPgn']="PGN o\\'rganish";i['studyUrl']="URL o\\'rganish";i['theChapterIsTooShortToBeAnalysed']="Ushbu bo\\'lim tahlil uchun juda ham qisqa.";i['timeTrouble']="Vaqt yetishmovchiligi";i['topics']="Mavzular";i['unclearPosition']="Noaniq holat";i['unlike']="O‘xshamagan";i['unlisted']="Ro\\'yhatga olinmagan";i['urlOfTheGame']="O\\'yin URL i";i['visibility']="Ko\\'rinishi";i['whatAreStudies']="Ta\\'limlar nima o\\'zi?";i['whatWouldYouPlay']="Ushbu holatda qanday o‘ynagan bo‘lardingiz?";i['whereDoYouWantToStudyThat']="Buni qayerda o\\'rganmoqchisiz?";i['whiteIsBetter']="Oqlarda vaziyat zo‘r";i['whiteIsSlightlyBetter']="Oqlarda vaziyat yaxshiroq";i['whiteIsWinning']="Oqlar yutayabdi";i['withCompensation']="Kompensatsiya bilan";i['withTheIdea']="G‘oya bilan";i['xBroughtToYouByY']=s("%1$s sizga %2$s orqali keltirdi");i['yesKeepEveryoneOnTheSamePosition']="Ha: hamma o\\'z o\\'rnida saqlansin";i['youAreNowAContributor']="Siz xozir yordamchisiz";i['youAreNowASpectator']="Siz xozir kuzatuvchisiz";i['youCanPasteThisInTheForumToEmbed']="Siz buni bo\\'limga bog\\'lash uchun forumga joylashingiz mumkin";i['youCompletedThisLesson']="Tabriklaymiz! Siz bu darsni tugatdingiz.";i['zugzwang']="Sugsvang"})()