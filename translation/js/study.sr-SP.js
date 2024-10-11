"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.study)window.i18n.study={};let i=window.i18n.study;i['addMembers']="Додај чланове";i['addNewChapter']="Додајте ново поглавље";i['allowCloning']="Дозволите клонирање";i['allStudies']="Све студије";i['allSyncMembersRemainOnTheSamePosition']="Сви SYNC чланови остају на истој позицији";i['analysisMode']="Врста анализе";i['annotateWithGlyphs']="Прибележите глифовима";i['automatic']="Аутоматски";i['chapterPgn']="PGN поглавља";i['chapterX']=s("Поглавље %s");i['clearAllCommentsInThisChapter']="Избриши све коментаре, глифове и нацртане облике у овом поглављу?";i['clearAnnotations']="Избриши анотације";i['clearChat']="Очисти ћаскање";i['cloneStudy']="Клонирај";i['commentThisMove']="Прокоментаришите овај потез";i['commentThisPosition']="Прокоментаришите ову позицију";i['contributor']="Cарадник";i['contributors']="Сарадници";i['createChapter']="Направи поглавље";i['createStudy']="Направи студију";i['currentChapterUrl']="Линк тренутног поглавља";i['dateAddedNewest']="Датум додавања (најновије)";i['dateAddedOldest']="Датум додавања (најстарије)";i['deleteChapter']="Избриши поглавље";i['deleteStudy']="Избриши студију";i['deleteTheStudyChatHistory']="Избриши историју ћаскања студије? Нема повратка назад!";i['deleteThisChapter']="Избриши ово поглавље? Нема повратка назад!";i['downloadAllGames']="Преузми све партије";i['downloadGame']="Преузми партију";i['editChapter']="Измени поглавље";i['editor']="Уређивач";i['editStudy']="Измени студију";i['embedInYourWebsite']="Угради у свој сајт или блог";i['empty']="Празно";i['enableSync']="Омогући синхронизацију";i['everyone']="Сви";i['first']="Прва";i['getAFullComputerAnalysis']="Добијте потпуну рачунарску анализу главне варијације од стране сервера.";i['hideNextMoves']="Сакриј следеће потезе";i['hot']="У тренду";i['interactiveLesson']="Интерактивна лекција";i['inviteOnly']="Само по позиву";i['inviteToTheStudy']="Позовите у студију";i['kick']="Избаци";i['last']="Последња";i['leaveTheStudy']="Напусти студију";i['like']="Свиђа ми се";i['loadAGameByUrl']="Учитајте партије преко линкова";i['loadAGameFromPgn']="Учитајте партију из PGN-а";i['loadAGameFromXOrY']=s("Учитајте партије са %1$s или %2$s");i['loadAPositionFromFen']="Учитајте позицију из FEN-а";i['makeSureTheChapterIsComplete']="Побрините се да је поглавље завршено. Само једном можете захтевати анализу.";i['members']="Чланови";i['mostPopular']="Најпопуларније";i['myFavoriteStudies']="Моје омиљене студије";i['myPrivateStudies']="Моје приватне студије";i['myPublicStudies']="Моје јавне студије";i['myStudies']="Моје студије";i['nbChapters']=p({"one":"%s Поглавље","few":"%s Поглављa","other":"%s Поглављa"});i['nbGames']=p({"one":"%s Партија","few":"%s Партијe","other":"%s Партија"});i['nbMembers']=p({"one":"%s Члан","few":"%s Чланa","other":"%s Чланова"});i['newChapter']="Ново поглавље";i['newTag']="Нова ознака";i['next']="Следећа";i['nobody']="Нико";i['noLetPeopleBrowseFreely']="Не: дозволи људима да слободно прегледају";i['noneYet']="Ниједна за сад.";i['noPinnedComment']="Ниједан";i['normalAnalysis']="Нормална анализа";i['onlyContributorsCanRequestAnalysis']="Само сарадници у студији могу захтевати рачунарску анализу.";i['onlyMe']="Само ја";i['onlyPublicStudiesCanBeEmbedded']="Само јавне студије могу бити уграђене!";i['open']="Отворите";i['orientation']="Оријентација";i['pasteYourPgnTextHereUpToNbGames']=p({"one":"Налепите свој PGN текст овде, до %s партије","few":"Налепите свој PGN текст овде, до %s партије","other":"Налепите свој PGN текст овде, до %s партија"});i['pgnTags']="PGN ознаке";i['pinnedChapterComment']="Закачен коментар поглавља";i['pinnedStudyComment']="Закачен коментар студије";i['playing']="У току";i['pleaseOnlyInvitePeopleYouKnow']="Молимо вас да само позивате људе које познајете и који активно желе да се придруже овој студији.";i['previous']="Претходна";i['private']="Приватна";i['public']="Јавно";i['readMoreAboutEmbedding']="Прочитај више о уграђивању";i['recentlyUpdated']="Недавно ажуриране";i['rightUnderTheBoard']="Одмах испод табле";i['save']="Сачувај";i['saveChapter']="Сачувај поглавље";i['searchByUsername']="Претражујте по корисничком имену";i['shareAndExport']="Подели и извези";i['shareChanges']="Делите измене са посматрачима и сачувајте их на сервер";i['spectator']="Посматрач";i['start']="Започни";i['startAtInitialPosition']="Започни на иницијалној позицији";i['startAtX']=s("Започни на %s");i['startFromCustomPosition']="Започните од жељене позиције";i['startFromInitialPosition']="Започните од иницијалне позиције";i['studiesCreatedByX']=s("Студије које је %s направио/ла");i['studiesIContributeTo']="Студије којима доприносим";i['studyNotFound']="Студија није пронађена";i['studyPgn']="PGN студије";i['studyUrl']="Линк студије";i['theChapterIsTooShortToBeAnalysed']="Поглавље је прекратко за анализу.";i['unlisted']="Неприказано";i['urlOfTheGame']="Линкови партија, једна по реду";i['visibility']="Видљивост";i['whatAreStudies']="Шта су студије?";i['whereDoYouWantToStudyThat']="Где желите то проучити?";i['xBroughtToYouByY']=s("%2$s Вам доноси %1$s");i['yesKeepEveryoneOnTheSamePosition']="Да: задржи све на истој позицији";i['youAreNowAContributor']="Сада сте сарадник";i['youAreNowASpectator']="Сада сте посматрач";i['youCanPasteThisInTheForumToEmbed']="Ово можете налепити у форум да уградите"})()