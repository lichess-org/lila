"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.learn)window.i18n.learn={};let i=window.i18n.learn;i['advanced']="Прасунуты этап";i['aPawnOnTheSecondRank']="Пешка з другога шэрагу можа хадзіць на два палі за ход!";i['attackTheOpponentsKing']="Атакуйце караля суперніка";i['attackYourOpponentsKing']="Атакуйце караля вашага суперніка такім чынам, каб яго няможна было абараніць!";i['awesome']="Выдатна!";i['backToMenu']="Вярнуцца ў меню";i['bishopComplete']="Віншуем! Вы апанавалі слана.";i['bishopIntro']="Далей мы навучымся хадзіць сланом!";i['blackJustMovedThePawnByTwoSquares']="Чорныя толькі што схадзілі пешкай\nна дзве клеткі!\nВазьміце яе.";i['boardSetup']="Пазіцыя на дошцы";i['boardSetupComplete']="Віншуем! Зараз вы ведаеце, як ставіць фігуры на шахматнай дошцы.";i['boardSetupIntro']="Абодва войскі сустракаюцца адно з адным, гатовыя да бітвы.";i['byPlaying']="гуляючы!";i['capture']="Узяцце";i['captureAndDefendPieces']="Узяцце і абарона фігур";i['captureComplete']="Віншуем! Зараз вы ведаеце, як ваяваць з шахматнымі фігурамі!";i['captureIntro']="Знайдзіце неабароненыя фігуры суперніка і забярыце іх!";i['captureThenPromote']="Пабіце, потым ператварыцеся!";i['castleKingSide']="Перасуньце свайго караля на дзве клеткі да ладдзі каралеўскага фланга!";i['castleKingSideMovePiecesFirst']="Кароткая ракіроўка! Спачатку вам трэба вывесці фігуры.";i['castleQueenSide']="Перасуньце свайго караля на дзве клеткі да ладдзі ферзевага фланга!";i['castleQueenSideMovePiecesFirst']="Доўгая ракіроўка! Спачатку вам трэба вывесці фігуры.";i['castling']="Ракіроўка";i['castlingComplete']="Віншуем! Вы павінны амаль заўсёды ракіравацца ў партыях.";i['castlingIntro']="Абараніце свайго караля і выведзіце ў бой ладдзю!";i['checkInOne']="Шах у адзін ход";i['checkInOneComplete']="Віншуем! Вы паставілі шах свайму суперніку, вымусіўшы яго бараніць свайго караля!";i['checkInOneGoal']="Нападзіце на караля суперніка\nза адзін ход!";i['checkInOneIntro']="Каб абвясціць шах, нападайце на караля суперніка. Ён вымушаны будзе бараніцца!";i['checkInTwo']="Шах у 2 хады";i['checkInTwoComplete']="Віншуем! Вы паставілі шах вашаму суперніку, вымусіўшы яго бараніць свайго караля!";i['checkInTwoGoal']="Стварыце пагрозу каралю суперніка\nў два хады!";i['checkInTwoIntro']="Знайдзіце камбінацыю ў два хады, каб паставіць шах каралю суперніка!";i['chessPieces']="Шахматныя фігуры";i['combat']="Бітва";i['combatComplete']="Віншуем! Цяпер вы ведаеце, як ідзе бой у шахматах!";i['combatIntro']="Добры ваяр ведае і як нападаць, і як абараняцца!";i['defeatTheOpponentsKing']="Вазьміце верх над каралём суперніка";i['defendYourKing']="Абараніце свайго караля";i['dontLetThemTakeAnyUndefendedPiece']="Не дайце ім забраць\nніводнай неабароненай фігуры!";i['enPassant']="Узяцце на праходзе";i['enPassantComplete']="Віншуем! Зараз вы можаце рабіць узяцце на праходзе.";i['enPassantIntro']="Калі пешка суперніка робіць ход на дзве клеткі, вы можаце ўзяць яе, як калі б яна зрабіла ход на адну.";i['enPassantOnlyWorksImmediately']="Пешка суперніка можа быць узята на праходзе толькі наўпростае пасля ходу.";i['enPassantOnlyWorksOnFifthRank']="Узяцце на праходзе можна зрабіць,\nкалі ваша пешка на пятым шэрагу.";i['escape']="Вас атакуюць! Ухіліцеся ад пагрозы!";i['escapeOrBlock']="Адвядзіце караля\nці бараніцеся!";i['escapeWithTheKing']="Адводзьце свайго караля!";i['evaluatePieceStrength']="Ацаніце сілу фігуры";i['excellent']="Цудоўна!";i['exerciseYourTacticalSkills']="Патрэніруйце свае тактычныя навыкі";i['findAWayToCastleKingSide']="Знайдзіце спосаб зрабіць кароткую ракіроўку!";i['findAWayToCastleQueenSide']="Знайдзіце спосаб зрабіць доўгую ракіроўку!";i['firstPlaceTheRooks']="Спачатку расстаўце ладдзі!\nЯны знаходзяцца па кутах.";i['fundamentals']="Асновы";i['getAFreeLichessAccount']="Стварыце бясплатны ўліковы запіс";i['grabAllTheStars']="Збярыце ўсе зоркі!";i['grabAllTheStarsNoNeedToPromote']="Збярыце ўсе зоркі!\nНеабавязкова ператвараць пешкі.";i['greatJob']="Выдатная праца!";i['howTheGameStarts']="Пачатак шахматнай партыі";i['intermediate']="Прамежкавы этап";i['itMovesDiagonally']="Рухаецца па дыяганалях";i['itMovesForwardOnly']="Рухаецца выключна наперад";i['itMovesInAnLShape']="Рухаецца літарай \\\"Г\\\"";i['itMovesInStraightLines']="Рухаецца па простых лініях";i['itNowPromotesToAStrongerPiece']="Цяпер яна можа стаць больш моцнай фігурай.";i['keepYourPiecesSafe']="Бараніце вашы фігуры";i['kingComplete']="Кароль цяпер пад тваім кіраўніцтвам!";i['kingIntro']="Вы - кароль. Калі вы загінеце ў бітве - гульня прайграна.";i['knightComplete']="Віншуем! Вы навучыліся панаваць над канём.";i['knightIntro']="Асвоіць каня не так проста, бо ён — хітрая фігура.";i['knightsCanJumpOverObstacles']="Коні могуць скакаць праз перашкоды!\nВыберацеся і збярыце ўсе зоркі!";i['knightsHaveAFancyWay']="Коні маюць мудрагелісты спосаб пераскокваць!";i['lastOne']="І апошні крок!";i['learnChess']="Вучыцеся";i['learnCommonChessPositions']="Вучыце асноўныя пазіцыі";i['letsGo']="Паехалі!";i['mateInOne']="Мат у адзін ход";i['mateInOneComplete']="Віншуем! Вось так перамагаюць у шахматах!";i['mateInOneIntro']="Вы перамагаеце, калі ваш супернік не можа абараніць караля ад шаха.";i['menu']="Меню";i['mostOfTheTimePromotingToAQueenIsBest']="У большасці выпадкаў пешку лепш ператварыць у ферзя.\nАле часам ператварэнне ў каня можа быць выгадней!";i['nailedIt']="Усё так!";i['next']="Далей";i['nextX']=s("Далей: %s");i['noEscape']="Няма куды бегчы,\nале вы можаце абараніцца!";i['opponentsFromAroundTheWorld']="Супернікі з усяго свету";i['outOfCheck']="Сыход з-пад шаха";i['outOfCheckComplete']="Віншуем! Цяпер вы ведаеце, як абараніць свайго караля ад шаха!";i['outOfCheckIntro']="Вам шах! Вы павінны сыходзіць ці бараніцца ад атакі.";i['outstanding']="Адменна!";i['pawnComplete']="Віншуем! У пешак няма сакрэтаў для вас.";i['pawnIntro']="Пешкі слабыя, але ў іх вялікі патэнцыял.";i['pawnPromotion']="Ператварэнне пешкі";i['pawnsFormTheFrontLine']="Пешкі стаяць на пярэдняй лініі.\nЗрабіце які-небудзь ход, каб працягнуць.";i['pawnsMoveForward']="Пешкі рухаюцца наперад, але б\\'юць па дыяганалях!";i['pawnsMoveOneSquareOnly']="Пешкі крочаць толькі на адну клетку наперад. Але дасягнуўшы іншага канца дошкі, яны ператвараюцца ў мацнейшую фігуру!";i['perfect']="Цудоўненька!";i['pieceValue']="Каштоўнасць фігур";i['pieceValueComplete']="Віншуем! Вы ведаеце каштоўнасць фігур!\nФерзь = 9 пешак\nЛаддзя= 5 пешак\nСлон = 3 пешкі\nКонь = 3 пешкі\nПешка = 1 пешка";i['pieceValueExchange']="Вазьміце найбольш каштоўную фігуру!\nНе разменьвайце больш каштоўную фігуру на менш каштоўную.";i['pieceValueIntro']="Фігуры з высокай рухомасцю маюць больш высокую каштоўнасць!\nФерзь = 9 пешак\nЛаддзя= 5 пешак\nСлон = 3 пешкі\nКонь = 3 пешкі\nПешка = 1 пешка\nКароль неацэнны! Яго страта азначае, што гульня прайграна.";i['pieceValueLegal']="Вазьміце\nнайбольш каштоўную фігуру!\nПераканайцеся, што ход магчымы!";i['placeTheBishops']="Расстаўце сланоў!\nЯны стаяць каля коней.";i['placeTheKing']="Пастаўце караля!\nЁн стаіць каля ферзя.";i['placeTheQueen']="Пастаўце ферзя!\nЁн ставіцца на свой колер.";i['play']="гуляць!";i['playMachine']="Гульня з ботам";i['playPeople']="Гульня анлайн";i['practice']="Практыка";i['progressX']=s("Зроблена: %s");i['protection']="Абарона";i['protectionComplete']="Віншуем! Кожная абароненая фігура – крок да перамогі!";i['protectionIntro']="Вызначыце, якія вашыя фігуры атакуе супернік, і абараніце іх!";i['puzzleFailed']="Задача не вырашана!";i['puzzles']="Задачы";i['queenCombinesRookAndBishop']="Ферзь = ладдзя + слон";i['queenComplete']="Віншуем! Ферзь для вас больш не з\\'яўляецца загадкай.";i['queenIntro']="Вось сапраўдная моц. Яго вялікасць — ферзь!";i['queenOverBishop']="Атакуйце фігуры з больш высокай каштоўнасцю!\nФерзь > Слон";i['register']="Рэгістрацыя";i['resetMyProgress']="Скасаваць дасягненні";i['retry']="Яшчэ раз";i['rightOn']="Слушна!";i['rookComplete']="Віншуем! Вы з поспехам апанавалі ладдзю.";i['rookGoal']="Націсніце на ладдзю і давядзіце яе да зорачкі!";i['rookIntro']="Ладдзя — гэта цяжкая фігура. Ці гатовы вы кіраваць ёй?";i['selectThePieceYouWant']="Абярыце якую пажадаеце!";i['stageX']=s("Этап %s");i['stageXComplete']=s("Этап %s завершаны");i['stalemate']="Пат";i['stalemateComplete']="Віншуем! Лепш атрымаць пат, чым мат!";i['stalemateGoal']="Пат чорным:\n- Чорныя не могуць пахадзіць.\n- Кароль не пад шахам.";i['stalemateIntro']="Калі гульцу не пастаўлены шах і няма магчымасці зрабіць ход, гэта пат. Гульня сканчаецца: без пераможцаў і пераможаных.";i['takeAllThePawnsEnPassant']="Вазьміце ўсе пешкі на праходзе!";i['takeTheBlackPieces']="Забярыце чорныя фігуры!";i['takeTheBlackPiecesAndDontLoseYours']="Забярыце ўсе чорныя фігуры! І не страцьце сваіх.";i['takeTheEnemyPieces']="Забярыце варожыя фігуры";i['takeThePieceWithTheHighestValue']="Вазьміце найкаштоўнейшую фігуру!";i['testYourSkillsWithTheComputer']="Праверце свае навыкі";i['theBishop']="Слон";i['theFewerMoves']="Чым менш рухаў вы зробіце,\nтым больш балаў атрымаеце!";i['theGameIsADraw']="Гульня з\\'яўляецца нічыйнай";i['theKing']="Кароль";i['theKingCannotEscapeButBlock']="Каралю няма куды бегчы, але можна абараніцца!";i['theKingIsSlow']="Кароль рухаецца павольна.";i['theKnight']="Конь";i['theKnightIsInTheWay']="Конь перашкаджае! Пахадзіце канём, потым зрабіце ракіроўку.";i['theMostImportantPiece']="Найважнейшая фігура";i['thenPlaceTheKnights']="Потым расстаўце коней!\nЯны стаяць каля ладдзей.";i['thePawn']="Пешка";i['theQueen']="Ферзь";i['theRook']="Ладдзя";i['theSpecialKingMove']="Адмысловы ход караля";i['theSpecialPawnMove']="Адмысловы ход пешкай";i['thisIsTheInitialPosition']="Гэта - пачатковая пазіцыя ў шахматах! Зрабіце любы ход, каб працягнуць.";i['thisKnightIsCheckingThroughYourDefenses']="Гэты конь ставіць шах,\nабыходзячы вашую абарону!";i['twoMovesToGiveCheck']="Два хады на тое, каб зрабіць шах";i['useAllThePawns']="Скарыстайцеся ўсімі пешкамі!\nНеабавязкова іх ператвараць.";i['useTwoRooks']="Скарыстайцеся дзвюма ладдзямі, каб паскорыць працэс!";i['videos']="Відэа";i['watchInstructiveChessVideos']="Глядзець навучальныя відэазапісы";i['wayToGo']="Так трымаць!";i['whatNext']="Што далей?";i['yesYesYes']="Так, так, так!";i['youCanGetOutOfCheckByTaking']="Вы можаце забраць фігуру, якая ставіць шах.";i['youCannotCastleIfAttacked']="Вы не можаце рабіць ракіроўку,\nкалі кароль знаходзіцца пад шахам.\nСпачатку абараніце караля!";i['youCannotCastleIfMoved']="Вы не можаце рабіць ракіроўку,\nкалі кароль ці ладдзя,\nякія ўдзельнічаюць у ракіроўцы, рабілі ход.";i['youKnowHowToPlayChess']="Вы ўмееце гуляць у шахматы, віншуем! Вы хочаце стаць мацнейшым гульцом?";i['youNeedBothBishops']="Палі белага колеру кантралюе адзін слон,\nпалі чорнага колеру — іншы.\nВам патрэбныя абодва сланы!";i['youreGoodAtThis']="Вы малайчына!";i['yourPawnReachedTheEndOfTheBoard']="Ваша пешка дасягнула апошняй лініі дошкі!";i['youWillLoseAllYourProgress']="Усе вашыя дасягненні будуць скасаваныя!"})()