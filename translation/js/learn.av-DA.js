"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.learn)window.i18n.learn={};let i=window.i18n.learn;i['advanced']="ЦӀебетӀураб";i['aPawnOnTheSecondRank']="2-б мухъалда бугеб пешка цого мехалъ 2 майданалдасан хъурщизе кӀола!";i['attackTheOpponentsKing']="Дандиясул къираласда тӀадекӀанцӀе";i['attackYourOpponentsKing']="Дур дандиясул къираласда тӀадекӀанцӀе\nгьев цӀунизе кӀоларев куцалъ!";i['awesome']="ХӀикматго!";i['backToMenu']="Менуялде тӀадвуссине";i['bishopComplete']="Баркула! Дуда пилалъе нухмалъизе кӀола.";i['bishopIntro']="Хадуса нилъеца пил кин хъурщулебали лъазабила!";i['blackJustMovedThePawnByTwoSquares']="ЧӀегӀераз гьале гьабсагӀат\nпешка кӀиго майдан бахинабуна!\nГьеб нахъаса чӀвай.";i['boardSetup']="Кескал кколе-кколелъур лъей";i['boardSetupComplete']="Баркула! Дуда хъорщода кескал лъолеб куц лъала.";i['boardSetupIntro']="КӀиго аскар дандчӀвала, рагъде хӀадурлъун.";i['byPlaying']="хӀалаго!";i['capture']="ЧӀвай";i['captureAndDefendPieces']="Кескал чӀвай ва цӀуне";i['captureComplete']="Баркула! Дуда лъала кин шахматазул кесказ къеркьезе!";i['captureIntro']="Дандиясул цӀунчӀел кескал ралагье, гьел чӀвазеги чӀвай!";i['captureThenPromote']="ЧӀвай, хадуб сверизабе!";i['castleKingSide']="Къокъаб рокировка гьабизе\nдур къирал кӀиго майдан бахинаве!";i['castleKingSideMovePiecesFirst']="Къокъаб рокировка гьабе!\nДуца цин кескал къватӀире хъурщизе ккола.";i['castleQueenSide']="Халатаб рокировка гьабизе\nдур къирал кӀиго майдан бахинаве!";i['castleQueenSideMovePiecesFirst']="Халатаб рокировка гьабе!\nДуца цин кескал къватӀире хъурщизе ккола.";i['castling']="Рокировка";i['castlingComplete']="Баркула! Дуца хӀаялда гӀемерисеб мехалъ рокировка гьабизе ккола.";i['castlingIntro']="Дур къирал хӀинкъи гьечӀеб бакӀалдеги лъе, хъала тӀадекӀанцӀизеги хӀадуре!";i['checkInOne']="Цо хъурщиялъ шах";i['checkInOneComplete']="Баркула! Дуца дандиясда шах лъазабун, гьев жиндир къирал цӀунизавуна!";i['checkInOneGoal']="Кираласде ишан босе\nцо хъурщиялъ!";i['checkInOneIntro']="Дур дандиясда шах лъазабизе, къираласда тӀадекӀанцӀе. Гьес гьев цӀунизе ккола!";i['checkInTwo']="КӀиго хъурщиялъ шах";i['checkInTwoComplete']="Баркула! Дуца дандиясда шах лъазабун, гьев жиндир къирал цӀунизавуна!";i['checkInTwoGoal']="Дандиясул къираласе хӀинкъи кье\nкӀиго хъурщиялъ!";i['checkInTwoIntro']="Дандиясул къираласда шах лъазабулеб кӀиго хъурщиялъул комбинация балагье!";i['chessPieces']="Шахматазул кескал";i['combat']="Къеркьей";i['combatComplete']="Баркула! Дуда кесказ тӀадекӀанцӀизе кколеб куц лъала!";i['combatIntro']="ЛъикӀав рагъухъанасда кӀиябго лъала, тӀадекӀанцӀизеги цӀунизеги!";i['defeatTheOpponentsKing']="Дандиясул къирал къезабе";i['defendYourKing']="Дур къирал цӀуне";i['dontLetThemTakeAnyUndefendedPiece']="Цониги цӀунчӀеб кесек чӀвазе биччаге!";i['enPassant']="Нахъаса чӀвай";i['enPassantComplete']="Баркула! Дуда гьанже нахъаса чӀвазе кӀола.";i['enPassantIntro']="Дандияс пешка кӀиго майдан бахинабураб мехалъ, дуда гьеб чӀвазе кӀола гьелъ цо майдан бахун бугеб гӀадин.";i['enPassantOnlyWorksImmediately']="Нахъаса чӀвазе бегьула\nдандияс пешка хъущарабго.";i['enPassantOnlyWorksOnFifthRank']="Нахъаса чӀвазе бегьула \nдур пешка 5-б мухъалда бугони.";i['escape']="Дуда тӀадекӀанцӀана!\nХӀинкъиялдаса ворчӀа!";i['escapeOrBlock']="Къирал борчӀизабе\nяги тӀадекӀанцӀи нахъчӀвай!";i['escapeWithTheKing']="Къирал лъутун ворчӀи!";i['evaluatePieceStrength']="Кескил къуваталъе къимат кье";i['excellent']="ЦӀакъ лъикӀ!";i['exerciseYourTacticalSkills']="Дур тактикиял ругьунлъаби церетӀе";i['findAWayToCastleKingSide']="Къираласул рахъалде\nрокировка гьабизе рес балагье!";i['findAWayToCastleQueenSide']="Къиралалъул рахъалде\nрокировка гьабизе рес балагье!";i['firstPlaceTheRooks']="Цин хъулби лъе!\nГьел бокӀназда рукӀуна.";i['fundamentals']="КьучӀал";i['getAFreeLichessAccount']="ЧӀобого Lichess хъвай-хъвагӀай гьабе";i['grabAllTheStars']="Киналго цӀваби росе!";i['grabAllTheStarsNoNeedToPromote']="Киналго цӀваби росе!\nСверизабизе хӀажалъи гьечӀо.";i['greatJob']="ЦӀакъаб хӀалтӀи!";i['howTheGameStarts']="ХӀай байбихьулеб куц";i['intermediate']="Гьоркьохъеб";i['itMovesDiagonally']="Диагоналазда хъурщула";i['itMovesForwardOnly']="ГӀицӀго цебехун хъурщула";i['itMovesInAnLShape']="Г хӀарпалъул куцалда хъурщула";i['itMovesInStraightLines']="РитӀарал хӀуччазда хъурщула";i['itNowPromotesToAStrongerPiece']="Гьанже гьеб кутакаб кесекалде сверула.";i['keepYourPiecesSafe']="Дур кескал цӀуне";i['kingComplete']="Дуда гьанже бетӀерасе бетӀерлъи гьабизе кӀола!";i['kingIntro']="Мун къирал вуго. Мун рагъда хвани, мун къола.";i['knightComplete']="Баркула! Дуца чол гъваридго лъазабуна.";i['knightIntro']="Гьале дуе масъала. Чу буго... захӀматаб кесек.";i['knightsCanJumpOverObstacles']="Чуязда цогидал кесказда тӀасан кӀанцӀизе кӀола!\nТӀасан кӀанцӀун цӀваби росе!";i['knightsHaveAFancyWay']="Чуял гӀажаибаб куцалъ кӀанцӀола!";i['lastOne']="Ахирисеб!";i['learnChess']="Шахматал лъазаризе";i['learnCommonChessPositions']="Аслиял шахматазул позицияби лъазаре";i['letsGo']="Байбихьизе!";i['mateInOne']="Цо хъурщиялъ мат";i['mateInOneComplete']="Баркула! Гьеле гьедин бергьуна шахматазда!";i['mateInOneIntro']="Дур дандиясда шахалдаса ворчӀизе кӀолареб мехалъ мун бергьуна.";i['menu']="Мену";i['mostOfTheTimePromotingToAQueenIsBest']="ГӀемерисеб мехалъ пешка къиралалде сверизе лъикӀ.\nАмма цо-цо мехалъ чу хӀажалъизе бегьула!";i['nailedIt']="Бажарана.";i['next']="Хадусеб";i['nextX']=s("Хадусеб: %s");i['noEscape']="ВорчӀизе рес гьечӀо,\nамма дуда цӀунизе кӀола!";i['opponentsFromAroundTheWorld']="ТӀолго дуниялалдаса дандиял";i['outOfCheck']="Шахалдаса борчӀи";i['outOfCheckComplete']="Баркула! Дур къирал чӀвазе рес гьечӀо, шахалдаса цӀунулеб куц кӀочон тоге!";i['outOfCheckIntro']="Дуда шах лъазабуна! Мун нахъе ине ккола яги тӀадекӀанцӀи нахъчӀвазе ккола.";i['outstanding']="Гьайбатго!";i['pawnComplete']="Баркула! Дуда пешкаялъул кинабго лъала.";i['pawnIntro']="Пешкаби загӀипал руго, амма гьезул цо кӀудияб рес буго.";i['pawnPromotion']="Пешка сверизаби";i['pawnsFormTheFrontLine']="Пешкабаз цебесеб мухъ лъугьинабула.\nЦебе бачине бокьохъе хъурще.";i['pawnsMoveForward']="Пешка цебехун хъурщула,\nамма диагоналалда чӀвала!";i['pawnsMoveOneSquareOnly']="Пешкаби цохӀо майданалдасан хъурщула.\nАмма гьел батӀияб рахъалде щваралъур кутакаб кесекалде сверула!";i['perfect']="Камилго!";i['pieceValue']="Кескил багьа";i['pieceValueComplete']="Баркула! Дуда кесказул багьаби лъала!\nКъиралай = 9\nХъала = 5\nПил = 3\nЧу = 3\nПешка = 1";i['pieceValueExchange']="Бищун хирияб багьа бугеб кесек чӀвай!\n Хириябги чӀвазе кьун\n учузаб чӀван хисуге.";i['pieceValueIntro']="ЦӀикӀкӀун рагъа-рачаризе кӀолел кесказул хирияб багьа буго.\nКъиралай = 9\nХъала = 5\nПил = 3\nЧу = 3\nПешка = 1\nКъирал къимат кьезе кӀолареб вуго! Гьев къуни мун тӀубанго хӀаялда къола.";i['pieceValueLegal']="Бищун хирияб багьа\nбугеб кесек чӀвай!\nБитӀухъе хъурщарабали ракӀ чӀун лъазе балагье!";i['placeTheBishops']="Пилал лъе!\nГьел чуязда аскӀор рукӀуна.";i['placeTheKing']="Къирал лъе!\nКъиралалда аскӀов.";i['placeTheQueen']="Къиралай лъе!\nГьей жиндирго кьералъул бакӀалда йикӀуна.";i['play']="хӀазе!";i['playMachine']="Компутергун хӀай";i['playPeople']="ЧагӀигун хӀазе";i['practice']="Ругьунлъизави";i['progressX']=s("Лъазабуна: %s");i['protection']="ЦӀуни";i['protectionComplete']="Баркула! Дуца къезе биччачӀеб кесек ккола дуе бергьун щвараб кесек!";i['protectionIntro']="Дур дандияв киназда тӀадекӀанцӀулев ралагье, цинги гьел цӀуне!";i['puzzleFailed']="Масъала гьабизе кӀвечӀо!";i['puzzles']="Масъалаби";i['queenCombinesRookAndBishop']="Къиралай = хъала + пил";i['queenComplete']="Баркула! Дуда къиралалъул кинабго лъала.";i['queenIntro']="Бищун кутакаб шахматазул кесек. БетӀерчӀахъаяй къиралай!";i['queenOverBishop']="Бищун кӀудаб \nбагьа бугеб кесек чӀвай!\nКъиралай > Пил";i['register']="ХӀисабалде воси";i['resetMyProgress']="ЦӀидасан байбихьизе";i['retry']="Цоги нухалъ";i['rightOn']="Гьеле гьедин!";i['rookComplete']="Баркула! Дуца бергьенлъиялда хъаладул гъваридго лъазабуна.";i['rookGoal']="Хъала цӀваялде щвезабизе, гьелда тӀадецуй!";i['rookIntro']="Хъала кутакаб кесек буго. Гьелъие нухмалъизе хӀадур вугищ?";i['selectThePieceYouWant']="Дуе рокьарал кескал рище!";i['stageX']=s("%s дарс");i['stageXComplete']=s("%s дарс тӀубазабуна");i['stalemate']="Пат";i['stalemateComplete']="Баркула! Маталдаса пат щвезе лъикӀ!";i['stalemateGoal']="ЧӀегӀеразда пат лъазабизе:\n- ЧӀегӀеразда кирениги хъурщизе кӀоларо\n- Шах гьечӀо.";i['stalemateIntro']="ХӀалесда шахги гьечӀони гьесе хъурщизе бакӀги гьечӀони, гьеб пат ккола. ХӀаялда ращад ккана: щивго бергьинчӀо, щивго къечӀо.";i['takeAllThePawnsEnPassant']="Киналго пешкаби нахъаса чӀвай!";i['takeTheBlackPieces']="ЧӀегӀеразул кескал нахъе росе!";i['takeTheBlackPiecesAndDontLoseYours']="ЧӀегӀеразул кескал нахъе росе!\nДурал тӀагӀине риччаге.";i['takeTheEnemyPieces']="Тушманасул кескал нахъе росе";i['takeThePieceWithTheHighestValue']="Бищун хирияб багьа \nбугеб кесек чӀвай!";i['testYourSkillsWithTheComputer']="Дур ругьунлъабазул даражаялъухъ компутералъ хал гьабе";i['theBishop']="Пил";i['theFewerMoves']="Дагьаб хъурщун цӀикӀкӀун балал щола!";i['theGameIsADraw']="Ращад ккана";i['theKing']="Къирал";i['theKingCannotEscapeButBlock']="Къираласда лъутизе кӀоларо,\nамма дуда тӀадекӀанцӀи нахъчӀвазе кӀола!";i['theKingIsSlow']="Къирал гӀодове виччарав вуго.";i['theKnight']="Чу";i['theKnightIsInTheWay']="Чу бачӀунеб буго!\nХъурще гьеб, цинги къокъаб рокировка гьабе.";i['theMostImportantPiece']="Бищун кӀвар бугеб кесек";i['thenPlaceTheKnights']="Цинги чуял лъе!\nГьел хъулбузда аскӀор рукӀуна.";i['thePawn']="Пешка";i['theQueen']="Къиралай";i['theRook']="Хъала";i['theSpecialKingMove']="Къираласул батӀияб хъурщи";i['theSpecialPawnMove']="Пешкаялъул батӀияб хъурщи";i['thisIsTheInitialPosition']="Гьаб буго щибаб шахматазул \nхӀаялъул байбихьулеб бакӀ!\nЦебе бачине бокьохъе хъурще.";i['thisKnightIsCheckingThroughYourDefenses']="Чоца шах лъазабула\nцӀунулел кесказдасан!";i['twoMovesToGiveCheck']="Шах лъазабизе кӀиго хъурщи";i['useAllThePawns']="Киналго пешкаби хӀалтӀизаре!\nСверизабизе хӀажалъи гьечӀо.";i['useTwoRooks']="Хехлъизе,\nкӀиго хъала хӀалтӀизабе!";i['videos']="Видеаби";i['watchInstructiveChessVideos']="Ругьунлъизарулел шахматазул видеабазухъ валагье";i['wayToGo']="Гьайгьай!";i['whatNext']="Щиб хадуб?";i['yesYesYes']="У, у, у!";i['youCanGetOutOfCheckByTaking']="Дуда шахалдаса ворчӀизе кӀола\nтӀадекӀанцӀулеб кесек чӀван.";i['youCannotCastleIfAttacked']="Дуда рокировка гьабизе кӀоларо\nкъираласда тӀадекӀанцӀулел ратани.\nШах нахъчӀвай цинги рокировка гьабе!";i['youCannotCastleIfMoved']="Дуда рокировка гьабизе кӀоларо\nкъирал яги хъала цебе \nхъурщун букӀун батани.";i['youKnowHowToPlayChess']="Дуда шахматал хӀалеб куц лъала, баркула! Дуе жеги кутакав хӀалев чилъун ккезе бокьун бугищ?";i['youNeedBothBishops']="Цояб хъахӀаб майданалъул пил,\nЦояб чӀегӀераб майданалъул пил.\nДуе кӀиябго къваригӀуна!";i['youreGoodAtThis']="Дур махщел буго!";i['yourPawnReachedTheEndOfTheBoard']="Дур пешка хъорщол рагӀалде щвана!";i['youWillLoseAllYourProgress']="Дур киналго хӀасилал тӀагӀуна!"})()