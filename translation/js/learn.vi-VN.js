"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.learn)window.i18n.learn={};let i=window.i18n.learn;i['advanced']="Nâng cao";i['aPawnOnTheSecondRank']="Một quân tốt trắng mà nằm ở hàng thứ 2 hoặc quân tốt đen ở hàng 7 có thể đi 2 ô trong một nước đi đấy!";i['attackTheOpponentsKing']="Tấn công Vua đối phương";i['attackYourOpponentsKing']="Tấn công Vua đối phương \nbằng một cách mà đối phương không thể phòng thủ!";i['awesome']="Tuyệt vời!";i['backToMenu']="Quay lại mục lục";i['bishopComplete']="Chúc mừng! Bạn đã có thể chỉ huy một quân tượng.";i['bishopIntro']="Tiếp theo chúng ta sẽ học cách điều khiển quân tượng!";i['blackJustMovedThePawnByTwoSquares']="Quân đen vừa di chuyển tốt\nlên hai ô!\nHãy bắt tốt qua đường.";i['boardSetup']="Thiết lập bàn cờ";i['boardSetupComplete']="Chúc mừng! Bạn đã biết cách thiết lập bàn cờ.";i['boardSetupIntro']="Hai bên quân đối mặt với nhau, sẵn sàng cho cuộc chiến.";i['byPlaying']="bằng cách chơi!";i['capture']="Ăn quân";i['captureAndDefendPieces']="Ăn và bảo vệ quân";i['captureComplete']="Chúc mừng! Bạn đã nắm được cách đánh với các quân cờ!";i['captureIntro']="Xác định các quân không được phòng thủ của đối phương và ăn chúng!";i['captureThenPromote']="Ăn quân, sau đó phong cấp!";i['castleKingSide']="Di chuyển quân Vua 2 ô để nhập thành gần!";i['castleKingSideMovePiecesFirst']="Nhập thành gần!\nBạn cần đi hết quân ở đó trước.";i['castleQueenSide']="Di chuyển quân Vua 2 ô\nđể nhập thành xa!";i['castleQueenSideMovePiecesFirst']="Nhập thành xa!\nBạn cần đi hết quân ở đó trước.";i['castling']="Nhập thành";i['castlingComplete']="Chúc mừng! Bạn luôn nên nhập thành trong mọi ván đấu.";i['castlingIntro']="Chuyển quân Vua đến vị trí an toàn, và triển khai quân Xe để tấn công!";i['checkInOne']="Chiếu trong 1 nước";i['checkInOneComplete']="Chúc mừng! Bạn đang chiếu đối phương, bắt buộc họ phải bảo vệ Vua!";i['checkInOneGoal']="Đe dọa Vua đối phương trong một nước!";i['checkInOneIntro']="Để chiếu đối thủ, hãy tấn công quân Vua. Họ sẽ phải phòng thủ!";i['checkInTwo']="Chiếu trong 2 nước";i['checkInTwoComplete']="Chúc mừng! Bạn đang chiếu, bắt buộc đối thủ phải phòng thủ quân Vua!";i['checkInTwoGoal']="Đe dọa Vua đối phương\ntrong 2 nước!";i['checkInTwoIntro']="Tìm sự kết hợp chính xác 2 nước đi để chiếu Vua đối phương!";i['chessPieces']="Các quân cờ";i['combat']="Giao chiến";i['combatComplete']="Chúc mừng! Bạn đã biết làm sao để chiến đấu với các quân cờ!";i['combatIntro']="Một chiến binh giỏi phải biết cả tấn công lẫn phòng thủ!";i['defeatTheOpponentsKing']="Đánh bại Vua đối phương";i['defendYourKing']="Phòng thủ Vua";i['dontLetThemTakeAnyUndefendedPiece']="Đừng để chúng bắt \nbất kỳ quân không được bảo vệ nào!";i['enPassant']="Bắt tốt qua đường";i['enPassantComplete']="Xin chúc mừng! Bây giờ bạn có thể bắt tốt qua đường.";i['enPassantIntro']="Khi đối thủ di chuyển tốt lên hai ô, bạn có thể bắt nó như khi nó di chuyên lên một ô.";i['enPassantOnlyWorksImmediately']="Bắt tốt qua đường chỉ xảy ra\nngay lập tức sau khi đối thủ\ndi chuyển quân tốt.";i['enPassantOnlyWorksOnFifthRank']="Bắt tốt qua đường chỉ xẩy ra\nnếu tốt của bạn ở hàng thứ 5.";i['escape']="Bạn đang bị tấn công! \nHãy thoát khỏi mối đe dọa đó!";i['escapeOrBlock']="Chạy quân Vua \nhoặc chặn nước chiếu!";i['escapeWithTheKing']="Chạy Vua khỏi bị chiếu!";i['evaluatePieceStrength']="Đánh giá sức mạnh của quân";i['excellent']="Xuất sắc!";i['exerciseYourTacticalSkills']="Luyện tập các kỹ năng chiến thuật của bạn";i['findAWayToCastleKingSide']="Tìm cách để\nnhập thành gần!";i['findAWayToCastleQueenSide']="Tìm cách để nhập thành xa!";i['firstPlaceTheRooks']="Đầu tiên hãy đặt các quân xe!\nChúng nằm ở các góc bàn cờ.";i['fundamentals']="Nguyên tắc cơ bản";i['getAFreeLichessAccount']="Tạo một tài khoản Lichess miễn phí";i['grabAllTheStars']="Lấy tất cả các ngôi sao!";i['grabAllTheStarsNoNeedToPromote']="Hãy lấy tất cả các ngôi sao!\nNhưng không cần phải phong cấp đâu.";i['greatJob']="Làm tốt lắm!";i['howTheGameStarts']="Ván cờ bắt đầu như thế nào";i['intermediate']="Trung cấp";i['itMovesDiagonally']="Nó đi chéo";i['itMovesForwardOnly']="Nó chỉ đi về phía trước";i['itMovesInAnLShape']="Nó đi theo hình chữ L";i['itMovesInStraightLines']="Nó đi theo các đường thẳng";i['itNowPromotesToAStrongerPiece']="Bây giờ nó cần phải được phong cấp lên một quân mạnh hơn.";i['keepYourPiecesSafe']="Giữ an toàn cho quân của bạn";i['kingComplete']="Bây giờ bạn có thể chỉ huy người chỉ huy!";i['kingIntro']="Bạn là quân vua. Nếu bạn bị ăn thì sẽ thua ván cờ.";i['knightComplete']="Chúc mừng! Bạn đã làm chủ được quân mã.";i['knightIntro']="Đây là một thử thách cho bạn. Quân mã là... một quân mưu mẹo.";i['knightsCanJumpOverObstacles']="Quân mã có thể nhảy qua các chướng ngại vật!\nTrốn thoát và lấy các ngôi sao!";i['knightsHaveAFancyWay']="Quân mã nhảy vòng quanh \nmột cách sành điệu!";i['lastOne']="Cuối cùng!";i['learnChess']="Học cờ vua";i['learnCommonChessPositions']="Học về các thế cờ cơ bản";i['letsGo']="Bắt đầu nào!";i['mateInOne']="Chiếu hết trong 1 nước";i['mateInOneComplete']="Chúc mừng! Đó là cách để bạn thắng ván cờ!";i['mateInOneIntro']="Bạn thắng khi đối phương không thể phòng thủ nước chiếu.";i['menu']="Mục lục";i['mostOfTheTimePromotingToAQueenIsBest']="Phần lớn trường hợp thì phong cấp thành một quân hậu là tốt nhất.\nNhưng đôi khi một quân mã có thể có ích!";i['nailedIt']="Luyện tập tốt.";i['next']="Tiếp";i['nextX']=s("Tiếp theo: %s");i['noEscape']="Không có nước để chạy thoát, \nnhưng bạn có thể phòng thủ!";i['opponentsFromAroundTheWorld']="Các đối thủ từ khắp nơi trên thế giới";i['outOfCheck']="Thoát chiếu";i['outOfCheckComplete']="Chúc mừng! Quân Vua có thể không bao giờ bị bắt, hãy đảm bảo bạn có thể phòng thủ nước chiếu!";i['outOfCheckIntro']="Bạn đang bị chiếu! Bạn phải thoát khỏi vị trí bị chiếu hoặc chặn nước chiếu.";i['outstanding']="Đáng chú ý!";i['pawnComplete']="Chúc mừng! Bạn đã nắm mọi bí mật của quân Tốt.";i['pawnIntro']="Tốt rất yếu, nhưng chúng có rất nhiều tiềm năng.";i['pawnPromotion']="Phong cấp cho Tốt";i['pawnsFormTheFrontLine']="Các quân tốt tạo nên hàng phía trước.\nHãy đi nước bất kỳ để tiếp tục.";i['pawnsMoveForward']="Tốt đi về phía trước\nnhưng ăn quân theo hướng chéo!";i['pawnsMoveOneSquareOnly']="Tốt chỉ đi về phía trước một ô.\nNhưng khi chúng đi đến bên kia của bàn cờ, chúng trở thành một quân mạnh hơn!";i['perfect']="Hoàn hảo!";i['pieceValue']="Giá trị của quân";i['pieceValueComplete']="Chúc mừng! Bạn đã hiểu giá trị từng quân cờ!\nHậu = 9\nXe = 5\nTượng = 3\nMã = 3\nTốt = 1";i['pieceValueExchange']="Ăn quân có giá trị cao nhất!\n Không đổi\n quân có giá trị cao với quân có giá trị thấp.";i['pieceValueIntro']="Quân cờ có khả năng di chuyển linh hoạt có giá trị cao hơn!\nHậu = 9\nXe = 5\nTượng = 3\nMã = 3\nTốt = 1\nQuân Vua là vô giá! Để mất Vua là thua cuộc.";i['pieceValueLegal']="Ăn quân\ncó giá trị cao nhất!\nĐảm bảo rằng nước đi của bạn là hợp lệ!";i['placeTheBishops']="Đặt quân Tượng!\nNgay cạnh quân Mã.";i['placeTheKing']="Đặt quân Vua!\nBên phải cạnh quân Hậu.";i['placeTheQueen']="Đặt quân Hậu!\nNó ở ô cùng màu với quân của bạn.";i['play']="chơi!";i['playMachine']="Đấu với máy";i['playPeople']="Đấu với người";i['practice']="Tập luyện";i['progressX']=s("Tiến trình: %s");i['protection']="Phòng thủ";i['protectionComplete']="Chúc mừng! Quân cờ của bạn không bị mất là quân cờ của bạn thắng!";i['protectionIntro']="Xác định các quân đang bị đối thủ tấn công và phòng thủ cho chúng!";i['puzzleFailed']="Giải sai!";i['puzzles']="Câu đố";i['queenCombinesRookAndBishop']="Hậu = xe + tượng";i['queenComplete']="Chúc mừng! Bạn đã nắm mọi bí mật của quân Hậu.";i['queenIntro']="Quân cờ mạnh nhất là đây. Hoàng hậu vĩ đại!";i['queenOverBishop']="Ăn quân cờ\ncó giá trị cao nhất!\nHậu > Tượng";i['register']="Đăng kí";i['resetMyProgress']="Đặt lại tiến trình của tôi";i['retry']="Thử lại";i['rightOn']="Chính xác, tuyệt vời!";i['rookComplete']="Chúc mừng! Bạn đã làm chủ được quân xe.";i['rookGoal']="Nhấn vào quân xe \nđể đưa nó đến ngôi sao!";i['rookIntro']="Quân xe là một quân cờ mạnh. Bạn đã sẵn sàng điều khiển nó chưa?";i['selectThePieceYouWant']="Chọn quân mà bạn muốn!";i['stageX']=s("Chặng %s");i['stageXComplete']=s("Hoàn thành chặng %s");i['stalemate']="Thủ hòa";i['stalemateComplete']="Chúc mừng! Thà bị hòa pat còn hơn bị chiếu hết!";i['stalemateGoal']="Để bên đen hòa pat:\n- Bên đen không thể di chuyển bất cứ đâu\n- Bên đen không bị chiếu.";i['stalemateIntro']="Khi người chơi không bị chiếu và không có nước đi nào hợp lệ, nó được tính là hòa pat. Ván cờ kết thúc hòa: không ai thắng, không ai thua.";i['takeAllThePawnsEnPassant']="Bắt tất các các quân tốt bằng bắt tốt qua đường!";i['takeTheBlackPieces']="Hãy ăn lấy quân đen!";i['takeTheBlackPiecesAndDontLoseYours']="Ăn lấy quân đen!\nNhưng đừng để mất quân của bạn.";i['takeTheEnemyPieces']="Ăn quân của đối thủ";i['takeThePieceWithTheHighestValue']="Hãy ăn quân\nvới giá trị cao nhất!";i['testYourSkillsWithTheComputer']="Kiểm tra các kỹ năng của bạn với máy";i['theBishop']="Quân tượng";i['theFewerMoves']="Bạn đi càng ít nước, \nbạn càng được nhiều điểm!";i['theGameIsADraw']="Ván cờ hòa do bí nước hay hết nước đi hợp lệ";i['theKing']="Quân vua";i['theKingCannotEscapeButBlock']="Quân Vua không thể thoát chiếu, \nnhưng bạn có thể chặn nước chiếu!";i['theKingIsSlow']="Quân vua thì đi chậm.";i['theKnight']="Quân mã";i['theKnightIsInTheWay']="Quân Mã đang chắn lối nhập thành!\nDi chuyển nó, và nhập thành gần.";i['theMostImportantPiece']="Quân cờ quan trọng nhất";i['thenPlaceTheKnights']="Rồi đặt các quân Mã! \nNgay cạnh quân Xe.";i['thePawn']="Quân tốt";i['theQueen']="Quân hậu";i['theRook']="Quân xe";i['theSpecialKingMove']="Nước đi đặc biệt của quân Vua";i['theSpecialPawnMove']="Nước đi đặc biệt của quân Tốt";i['thisIsTheInitialPosition']="Đây là thế trận ban đầu \ncủa mọi ván cờ!\nHãy đi nước bất kỳ để tiếp tục.";i['thisKnightIsCheckingThroughYourDefenses']="Quân mã này đang chiếu \nxuyên qua các quân phòng thủ!";i['twoMovesToGiveCheck']="Hai nước để chiếu";i['useAllThePawns']="Hãy sử dụng hết quân Tốt!\nKhông cần phong cấp đâu.";i['useTwoRooks']="Dùng hai quân xe \nđể tăng tốc mọi thứ!";i['videos']="Các thước phim";i['watchInstructiveChessVideos']="Xem các thước phim hướng dẫn chơi cờ";i['wayToGo']="Làm rất tốt!";i['whatNext']="Bước tiếp theo?";i['yesYesYes']="Đúng, đúng, rất đúng!";i['youCanGetOutOfCheckByTaking']="Bạn có thể thoát chiếu \nbằng cách ăn quân đang chiếu.";i['youCannotCastleIfAttacked']="Bạn không thể nhập thành nếu\nquân Vua bị chiếu trên đường nhập thành.\nHãy chặn nước chiếu sau đó nhập thành!";i['youCannotCastleIfMoved']="Bạn không thể nhập thành nếu\nquân Vua đã di chuyển\nhoặc quân Xe đã di chuyển.";i['youKnowHowToPlayChess']="Bạn biết cách chơi cờ vua rồi đấy, chúc mừng! Bạn có muốn trở thành một người chơi giỏi hơn?";i['youNeedBothBishops']="Một quân tượng ô trắng, \nmột quân tượng ô đen. \nBạn cần cả hai!";i['youreGoodAtThis']="Bạn giỏi quá!";i['yourPawnReachedTheEndOfTheBoard']="Quân Tốt của bạn đã đến bên kia của bàn cờ rồi đấy!";i['youWillLoseAllYourProgress']="Bạn sẽ mất tất cả các thành tích và tiến trình của bạn!"})()