"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.study)window.i18n.study={};let i=window.i18n.study;i['addMembers']="Добави членове";i['addNewChapter']="Добавяне на нов раздел";i['allowCloning']="Позволи клониране";i['allStudies']="Всички казуси";i['allSyncMembersRemainOnTheSamePosition']="Всички синхронизирани членове остават на същата позиция";i['alphabetical']="Азбучно";i['analysisMode']="Режим на анализ";i['annotateWithGlyphs']="Анотация със специални символи";i['attack']="Атака";i['automatic']="Автоматичен";i['back']="Обратно";i['blackIsBetter']="Черните са по-добре";i['blackIsSlightlyBetter']="Черните са малко по-добре";i['blackIsWinning']="Черните печелят";i['blunder']="Груба грешка";i['brilliantMove']="Отличен ход";i['chapterPgn']="PGN на главата";i['chapterX']=s("Глава: %s");i['clearAllCommentsInThisChapter']="Изтрий всички коментари, специални символи и нарисувани форми в главата?";i['clearAnnotations']="Изтрий анотациите";i['clearChat']="Изтрий чат съобщенията";i['clearVariations']="Изчисти вариациите";i['cloneStudy']="Клонирай";i['commentThisMove']="Коментирай хода";i['commentThisPosition']="Коментирай позицията";i['confirmDeleteStudy']=s("Изтриване на целия казус? Това е необратимо! Въведете името на казуса за да потвърдите: %s");i['contributor']="Сътрудник";i['contributors']="Сътрудници";i['copyChapterPgn']="Копирай PGN";i['counterplay']="Контра атака";i['createChapter']="Създай";i['createStudy']="Създай казус";i['currentChapterUrl']="URL на настоящата глава";i['dateAddedNewest']="Дата на добавяне (най-нови)";i['dateAddedOldest']="Дата на добавяне (най-стари)";i['deleteChapter']="Изтрий глава";i['deleteStudy']="Изтрий казуса";i['deleteTheStudyChatHistory']="Изтриване на чат историята? Това е необратимо!";i['deleteThisChapter']="Изтриване на главата? Това е необратимо!";i['development']="Развитие";i['downloadAllGames']="Изтегли всички партии";i['downloadGame']="Изтегли партия";i['dubiousMove']="Съмнителен ход";i['editChapter']="Промени глава";i['editor']="Редактор";i['editStudy']="Редактирай казус";i['embedInYourWebsite']="Вгради в твоя сайт или блог";i['empty']="Празна";i['enableSync']="Разреши синхронизиране";i['equalPosition']="Равна позиция";i['everyone']="Всички";i['first']="Първа";i['getAFullComputerAnalysis']="Вземи пълен сървърен анализ на основна линия.";i['goodMove']="Добър ход";i['hideNextMoves']="Скриване на следващите ходове";i['hot']="Популярни";i['importFromChapterX']=s("Импортиране от %s");i['initiative']="Инициатива";i['interactiveLesson']="Интерактивен урок";i['interestingMove']="Интересен ход";i['inviteOnly']="Само с покани";i['inviteToTheStudy']="Покани към казуса";i['kick']="Изритване";i['last']="Последна";i['leaveTheStudy']="Напусни казуса";i['like']="Харесай";i['loadAGameByUrl']="Зареди партии от URL";i['loadAGameFromPgn']="Зареди партии от PGN";i['loadAGameFromXOrY']=s("Зареди партии от %1$s или %2$s");i['loadAPositionFromFen']="Зареди позиция от FEN";i['makeSureTheChapterIsComplete']="Уверете се, че главата е завършена. Можете да пуснете анализ само веднъж.";i['manageTopics']="Управление на темите";i['members']="Членове";i['mistake']="Грешка";i['mostPopular']="Най-популярни";i['myFavoriteStudies']="Моите любими казуси";i['myPrivateStudies']="Моите лични казуси";i['myPublicStudies']="Моите публични казуси";i['myStudies']="Моите казуси";i['myTopics']="Моите теми";i['nbChapters']=p({"one":"%s Глава","other":"%s Глави"});i['nbGames']=p({"one":"%s Игра","other":"%s Игри"});i['nbMembers']=p({"one":"%s Член","other":"%s Членове"});i['newChapter']="Нова глава";i['newTag']="Нов таг";i['next']="Следваща";i['nextChapter']="Следваща глава";i['nobody']="Никой";i['noLetPeopleBrowseFreely']="Не: позволи свободно разглеждане";i['noneYet']="Все още няма.";i['noPinnedComment']="Никакви";i['normalAnalysis']="Нормален анализ";i['novelty']="Нововъведeние";i['onlyContributorsCanRequestAnalysis']="Само сътрудници към казуса могат да пускат компютърен анализ.";i['onlyMe']="Само за мен";i['onlyMove']="Единствен ход";i['onlyPublicStudiesCanBeEmbedded']="Само публични казуси могат да бъдат вграждани!";i['open']="Отвори";i['orientation']="Ориентация";i['pasteYourPgnTextHereUpToNbGames']=p({"one":"Постави твоя PGN текст тук, до %s партия","other":"Постави твоя PGN текст тук, до %s партии"});i['pgnTags']="PGN тагове";i['pinnedChapterComment']="Коментар на главата";i['pinnedStudyComment']="Коментар на казуса";i['playAgain']="Играйте отново";i['playing']="Играе се";i['pleaseOnlyInvitePeopleYouKnow']="Моля канете само хора, които познавате и които биха искали да се присъединят.";i['popularTopics']="Популярни теми";i['prevChapter']="Предишна глава";i['previous']="Предишна";i['private']="Лични";i['public']="Публични";i['readMoreAboutEmbedding']="Прочети повече за вграждането";i['recentlyUpdated']="Скоро обновени";i['rightUnderTheBoard']="Точно под дъската";i['save']="Запази";i['saveChapter']="Запази глава";i['searchByUsername']="Търсене по потребителско име";i['shareAndExport']="Сподели";i['shareChanges']="Споделете промените със зрителите и ги запазете на сървъра";i['spectator']="Зрител";i['start']="Начало";i['startAtInitialPosition']="Започни от начална позиция";i['startAtX']=s("Започни от %s");i['startFromCustomPosition']="Започни от избрана позиция";i['startFromInitialPosition']="Започни от начална позиция";i['studiesCreatedByX']=s("Казуси от %s");i['studiesIContributeTo']="Казуси, към които допринасям";i['studyActions']="Опции за учене";i['studyNotFound']="Казусът не бе открит";i['studyPgn']="PGN на казуса";i['studyUrl']="URL на казуса";i['theChapterIsTooShortToBeAnalysed']="Тази глава е твърде къса и не може да бъде анализирана.";i['timeTrouble']="Проблем с времето";i['topics']="Теми";i['unclearPosition']="Неясна позиция";i['unlike']="Не харесвам";i['unlisted']="Несподелени";i['urlOfTheGame']="URL на партиите, по една на линия";i['visibility']="Видимост";i['whatAreStudies']="Какво представляват казусите?";i['whatWouldYouPlay']="Какво бихте играли в тази позиция?";i['whereDoYouWantToStudyThat']="Къде да бъде проучено това?";i['whiteIsBetter']="Белите са по-добре";i['whiteIsSlightlyBetter']="Белите са малко по-добре";i['whiteIsWinning']="Белите печелят";i['withCompensation']="С компенсация";i['withTheIdea']="С идеята";i['xBroughtToYouByY']=s("%1$s, предоставени от %2$s");i['yesKeepEveryoneOnTheSamePosition']="Да: дръж всички на същата позиция";i['youAreNowAContributor']="Вие сте сътрудник";i['youAreNowASpectator']="Вие сте зрител";i['youCanPasteThisInTheForumToEmbed']="Можете да поставите това във форум и ще бъде вградено";i['youCompletedThisLesson']="Поздравления! Вие завършихте този урок.";i['zugzwang']="Цугцванг"})()