"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.contact)window.i18n.contact={};let i=window.i18n.contact;i['accountLost']="Ако обаче наистина сте използвали помощ от двигател, дори само веднъж, тогава вашият акаунт е за съжаление загубен.";i['accountSupport']="Имам нужда от помощ за акаунта";i['authorizationToUse']="Разрешение за използване на Lichess";i['banAppeal']="Жалба за бан или IP ограничение";i['botRatingAbuse']="В някои случаи, игри с рейтинг срещу ботове могат да не донесат точки, ако система определи че играчът експлоатира бота за увеличаване на рейтинга си.";i['buyingLichess']="Покупка на Lichess";i['calledEnPassant']="Нарича се \\\"ан пасан\\\" и е едно от правилата на шаха.";i['cantChangeMore']="Не можем да променим повече. По технически причини е направо невъзможно.";i['cantClearHistory']="Не е възможно да почистите в играта своите история, шахматни задачи или рейтинги.";i['castlingImported']="Ако сте импортирали играта или сте я започнали от позиция, уверете се, че сте определили правилно правата на рокада.";i['castlingPrevented']="Рокадата се предотвратява само ако кралят мине през контролиран квадрат.";i['castlingRules']="Уверете се, че разбирате правилата за рокада";i['changeUsernameCase']="Посетете тази страница, за да промените регистъра на потребителското си име";i['closeYourAccount']="Може да затворите акаунта Ви на тази страница";i['collaboration']="Сътрудничество, правно, търговско";i['contact']="Контакти";i['contactLichess']="Свържи се с нас";i['creditAppreciated']="Кредитът е препоръчителен, но не е задължителен.";i['doNotAskByEmail']="Не питайте за затваряне на акаунт по имейл, няма да го направим.";i['doNotAskByEmailToReopen']="Не ни питайте да отворим отново акаунт по имейл, няма да го направим.";i['doNotDeny']="Не отричайте, че сте лъгали. Ако искате да ви бъде позволено да създадете нов акаунт, просто признайте какво сте направили и покажете, че сте разбрали, че е било грешка.";i['doNotMessageModerators']="Моля, не изпращайте директни съобщения до модераторите.";i['doNotReportInForum']="Не докладвайте играчи във форума.";i['doNotSendReportEmails']="Не докладвайте по имейл.";i['doPasswordReset']="Направете нулиране на паролата, за да премахнете втория фактор";i['engineAppeal']="Програма или измама";i['errorPage']="Страница за грешката";i['explainYourRequest']="Моля, обяснете заявката си ясно и подробно. Посочете вашето потребителско име в Lichess и всяка информация, която може да ни помогне да ви помогнем.";i['falsePositives']="Понякога се случват фалшиви положителни резултати и ние съжаляваме за това.";i['fideMate']="Според правилата на ФИДЕ за шахмат §6. 9, ако матът е възможен с всяка легална последователност от ходове, то партията не е реми";i['forgotPassword']="Забравих паролата си";i['forgotUsername']="Забравих потребителското си име";i['howToReportBug']="Моля, опишете как изглежда грешката, какво очаквате да се случи вместо това и стъпките за възпроизвеждане на грешката.";i['iCantLogIn']="Не мога да вляза в профила си";i['ifLegit']="Ако жалбата ви е основателна, ще отменим забраната възможно най-скоро.";i['illegalCastling']="Нелегална или невъзможна рокада";i['illegalPawnCapture']="Нелегално взимане на пешка";i['insufficientMaterial']="Недостатъчен материал";i['knightMate']="Възможно е да дадете мат само с кон или офицер, ако противникът има и други фигури освен един цар на дъската.";i['learnHowToMakeBroadcasts']="Научете се как да правите излъчвания в Lichess";i['lost2FA']="Нямам достъп до 2-стъпковите кодове за аутентикация";i['monetizing']="Монетизация на Lichess";i['noConfirmationEmail']="Не получих имейл за потвърждение";i['noneOfTheAbove']="Никое от горните";i['noRatingPoints']="Не бяха присъдени рейтинг точки";i['onlyReports']="Ефективно е само докладването на играчи чрез формуляра.";i['orCloseAccount']="Обаче може да затворите текущия си акаунт и да създадете нов.";i['otherRestriction']="Друго ограничение";i['ratedGame']="Уверете се, че сте играли игра с рейтинг. Приятелските игри не влияят на рейтинга на играчите.";i['reopenOnThisPage']="Можете да отворите отново акаунта си на тази страница. Работи само веднъж.";i['reportBugInDiscord']="В Discord сървъра на Lichess";i['reportBugInForum']="В раздела за обратна връзка на Lichess на форума";i['reportErrorPage']="Ако сте се сблъскали с грешна страница, можете да съобщите за нея:";i['reportMobileIssue']="Като проблем за мобилно приложение Lichess в GitHub";i['reportWebsiteIssue']="Като проблем с уебсайта на Lichess в GitHub";i['sendAppealTo']=s("Можете да изпратите жалба до %s.");i['sendEmailAt']=s("Изпратете ни имейл на %s.");i['toReportAPlayerUseForm']="За да подадете сигнал за играч, използвайте формуляра";i['tryCastling']="Опитайте тази малка интерактивна игра, за да упражните рокадата";i['tryEnPassant']="Опитайте тази малка интерактивна игра за да научите повече за \\\"ан пасан\\\".";i['videosAndBooks']="Можете да го покажете във видеоклиповете си и можете да отпечатате екранни снимки на Lichess в книгите си.";i['visitThisPage']="Посетете тази страница за разрешаване на проблема";i['visitTitleConfirmation']="За да се показва титла Ви в профила Ви в Lichess и да участвате в Titled Арени, посетете страницата за потвърждение на титлата";i['wantChangeUsername']="Искам да променя потребителското ми име";i['wantClearHistory']="Искам да изчистя моята история или рейтинг";i['wantCloseAccount']="Искам да затворя моя акаунт";i['wantReopen']="Искам отново да отворя акаунта ми";i['wantReport']="Искам да докладвам играч";i['wantReportBug']="Искам да докладвам бъг";i['wantTitle']="Искам моята титла да се показва в Lichess";i['welcomeToUse']="Можете да използвате Lichess за Вашата дейност, дори ако е търговска.";i['whatCanWeHelpYouWith']="Как можем да бъдем полезни?";i['youCanAlsoReachReportPage']=s("Също можете да посетите тази страница с натискането на %s бутона за докладване на страница на профил.");i['youCanLoginWithEmail']="Можете да влезете с имейл адреса, с който сте регистрирани"})()