"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.features)window.i18n.features={};let i=window.i18n.features;i['allChessBasicsLessons']="Всі уроки основ гри в шахи";i['allFeaturesAreFreeForEverybody']="Усі функції безплатні для всіх, назавжди!";i['allFeaturesToCome']="Усі майбутні функції, назавжди!";i['boardEditorAndAnalysisBoardWithEngine']=s("Редактор дошки та дошка аналізу з %s");i['chessInsights']="Шахова аналітика (детальний аналіз ваших ігор)";i['cloudEngineAnalysis']="Хмарний аналіз";i['contributeToLichessAndGetIcon']="Зробіть свій внесок у Lichess і отримайте чудовий значок Patron";i['correspondenceWithConditionalPremoves']="Заочні шахи з умовними попередніми ходами";i['deepXServerAnalysis']=s("Глибокий серверний аналіз %s");i['downloadOrUploadAnyGameAsPgn']="Завантажити/Вивантажити будь-яку гру у форматі PGN";i['endgameTablebase']="База ендшпілів з 7 фігурами";i['everybodyGetsAllFeaturesForFree']="Так, обидва облікові записи мають однакові функції!";i['gamesPerDay']=p({"one":"%s гра на день","few":"%s гри на день","many":"%s ігор на день","other":"%s гри на день"});i['globalOpeningExplorerInNbGames']=s("Глобальний провідник дебютів (%s ігор!)");i['ifYouLoveLichess']="Якщо ви любите Lichess,";i['landscapeSupportOnApp']="iPhone & Телефони та планшети Android, підтримка горизонтального режиму";i['lightOrDarkThemeCustomBoardsPiecesAndBackground']="Світла/Темна теми, користувацькі дошки, фігури та тло";i['personalOpeningExplorer']="Особистий провідник дебютів";i['personalOpeningExplorerX']=s("%1$s (також працює на %2$s)");i['standardChessAndX']=s("Стандартні шахи та %s");i['studies']="Дослідження (доступний і постійний аналіз)";i['supportLichess']="Підтримка Lichess";i['supportUsWithAPatronAccount']="Підтримайте нас за допомогою облікового запису Patron!";i['tacticalPuzzlesFromUserGames']="Тактичні задачі з ігор гравців";i['tvForumBlogTeamsMessagingFriendsChallenges']="Блог, форум, команди, TV, повідомлення, друзі, виклики";i['ultraBulletBulletBlitzRapidClassicalAndCorrespondenceChess']="УльтраКуля, Куля, Бліц, Рапід, Класичні, Заочні Шахи";i['weBelieveEveryChessPlayerDeservesTheBest']="Ми віримо, що кожен шахіст заслуговує найкращого, тому:";i['zeroAdsAndNoTracking']="Без реклами, без відстеження"})()