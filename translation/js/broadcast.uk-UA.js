"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.broadcast)window.i18n.broadcast={};let i=window.i18n.broadcast;i['aboutBroadcasts']="Про трансляцію";i['addRound']="Додати раунд";i['ageThisYear']="Вік цього року";i['broadcastCalendar']="Календар трансляцій";i['broadcasts']="Трансляції";i['completed']="Завершені";i['completedHelp']="Lichess виявляє завершення раунду на основі ігор. Використовуйте цей перемикач якщо немає джерела.";i['credits']="Вдячність джерелу";i['currentGameUrl']="Посилання на поточну гру";i['definitivelyDeleteRound']="Видалити всі ігри цього раунду.";i['definitivelyDeleteTournament']="Остаточно видалити весь турнір, всі його раунди та всі його ігри.";i['deleteAllGamesOfThisRound']="Видалити всі ігри цього раунду. Джерело має бути активним для того, щоб повторно відтворити його.";i['deleteRound']="Видалити цей раунд";i['deleteTournament']="Видалити турнір";i['downloadAllRounds']="Завантажити всі тури";i['editRoundStudy']="Редагувати дослідження раунду";i['federation']="Федерація";i['fideFederations']="Федерації FIDE";i['fidePlayerNotFound']="Гравця FIDE не знайдено";i['fidePlayers']="Гравці FIDE";i['fideProfile']="Профіль FIDE";i['fullDescription']="Повний опис події";i['fullDescriptionHelp']=s("Необов\\'язковий довгий опис трансляції. Наявна розмітка %1$s. Довжина має бути менша ніж %2$s символів.");i['howToUseLichessBroadcasts']="Як користуватися Lichess трансляціями.";i['liveBroadcasts']="Онлайн трансляції турнірів";i['myBroadcasts']="Мої трансляції";i['nbBroadcasts']=p({"one":"%s трансляція","few":"%s трансляції","many":"%s трансляцій","other":"%s трансляцій"});i['newBroadcast']="Нова трансляція";i['ongoing']="Поточні";i['periodInSeconds']="Період у секундах";i['periodInSecondsHelp']="Опціонально: час очікування між запитами. Мінімально 2 с, максимально 60 с. За замовчуванням - автоматично, виходячи з кількості запитів глядачів.";i['recentTournaments']="Нещодавні турніри";i['replacePlayerTags']="За бажанням: замінити імена, рейтинги та титули гравців";i['resetRound']="Скинути цей раунд";i['roundName']="Назва раунду";i['roundNumber']="Номер раунду";i['showScores']="Показувати результати гравців за результатами гри";i['sourceGameIds']="До 64 ігрових ID Lichess, відокремлені пробілами.";i['sourceSingleUrl']="Адреса джерела PGN";i['sourceUrlHelp']="Посилання, яке Lichess перевірятиме, щоб отримати оновлення PGN. Воно має бути загальнодоступним в Інтернеті.";i['startDateHelp']="За бажанням, якщо ви знаєте, коли починається подія";i['subscribedBroadcasts']="Обрані трансляції";i['theNewRoundHelp']="У новому раунді будуть ті самі учасники та редактори, що й у попередньому.";i['top10Rating']="Топ 10 рейтингу";i['tournamentDescription']="Короткий опис турніру";i['tournamentName']="Назва турніру";i['unrated']="Без рейтингу";i['upcoming']="Майбутні"})()