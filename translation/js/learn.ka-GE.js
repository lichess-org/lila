"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.learn)window.i18n.learn={};let i=window.i18n.learn;i['advanced']="გაფართოებული";i['aPawnOnTheSecondRank']="პაიკს მეორე ჰორიზონტალზე შეუძლია 2 უჯრით გადაადგილება მხოლოდ ერთხელ!";i['attackTheOpponentsKing']="დაემუქრე მოწინააღმდეგის მეფეს";i['attackYourOpponentsKing']="დაემუქრე მეფეს ისე რომ ვერ დაიცვან!";i['awesome']="კარგია!";i['backToMenu']="მენიუში დაბრუნება";i['bishopComplete']="გილოცავ! შენ იცი კუთი თამაში.";i['bishopIntro']="ახლა ვისწავლით კუს მანევრირებას!";i['blackJustMovedThePawnByTwoSquares']="შავმა ორი სვლით წამოწია პაიკი!\nაიყვანე გავლით.";i['boardSetup']="დაფის დაწყობა";i['boardSetupComplete']="გილოცავ! დაფის დაწყობა ისწავლე.";i['boardSetupIntro']="ორი არმია ერთმანეთის წინააღმდეგ.";i['byPlaying']="თამაშით!";i['capture']="აყვანა";i['captureAndDefendPieces']="აიყვანე და დაიცავი ფიგურები";i['captureComplete']="გილოცავ! უკვე იცი ფიგურებით ბრძოლა!";i['captureIntro']="იპოვე მოწინააღმდეგის დაუცველი ფიგურები, და აიყვანე!";i['captureThenPromote']="აიყვანე, შემდეგ გააცოცხლე!";i['castleKingSide']="გადაადგილე მეფე ორი უჯრით\nრათა გააკეთო მოკლე როქი!";i['castleKingSideMovePiecesFirst']="გააკეთე მოკლე როქი!\nჯერ შუა ფიგურები უნდა გაწიო.";i['castleQueenSide']="გადაადგილე მეფე ორი უჯრით\nრათა გააკეთო გრძელი როქი!";i['castleQueenSideMovePiecesFirst']="გააკეთე გრძელი როქი!\nჯერ შუა ფიგურები უნდა გაწიო.";i['castling']="როქი";i['castlingComplete']="გილოცავ! როქი თითქმის ყოველთვის საჭიროა.";i['castlingIntro']="დაიცავი მეფე და გამოიყვანე ეტლი!";i['checkInOne']="ქიში ერთ სვლაში";i['checkInOneComplete']="გილოცავ! შენ ქიში გამოუცხადე და აიძულე დაეცვათ მეფე!";i['checkInOneGoal']="დაუმიზნე მოწინააღმდეგის მეფეს ერთ სვლაში!";i['checkInOneIntro']="დაემუქრე მეფეს რათა ქიში უთხრა. უნდა დაიცვან!";i['checkInTwo']="ქიში ორ სვლაში";i['checkInTwoComplete']="გილოცავ! ქიში უთხარი, და აიძულე დაეცვათ მეფე!";i['checkInTwoGoal']="დაემუქრე მოწინააღმდეგის მეფეს\nორ სვლაში!";i['checkInTwoIntro']="იპოვე ორსვლიანი კომბინაცია ქიშისთვის!";i['chessPieces']="ჭადრაკის ფიგურები";i['combat']="შეტევა";i['combatComplete']="გილოცავ! უკვე იცი ფიგურებით ბრძოლა!";i['combatIntro']="კარგმა მებრძოლმა შეტევაც იცის და დაცვაც!";i['defeatTheOpponentsKing']="დაამარცხე მოწინააღმდეგის მეფე";i['defendYourKing']="დაიცავი მეფე";i['dontLetThemTakeAnyUndefendedPiece']="არ ააყვანანინო არცერთი\nდაუცველი ფიგურა!";i['enPassant']="გავლით აყვანა";i['enPassantComplete']="გილოცავ! გავლით აყვანა იცი.";i['enPassantIntro']="როცა მოწინააღდეგის პაიკი ორი უჯრით წამოვა, შეგიძლია აიყვანო გეგონება ერთი უჯრით წამოვიდა.";i['enPassantOnlyWorksImmediately']="გავლით აყვანა მხოლოდ შემდეგ სვლაზევე შეგიძლია.";i['enPassantOnlyWorksOnFifthRank']="გავლით მხოლოდ მაშინ აიყვან\nთუ შენი პაიკი მეხუთე ჰორიზონტალზეა.";i['escape']="გიტევენ!\nდააღწიე თავი!";i['escapeOrBlock']="დააღწიე თავი ან \nდაბლოკე შეტევა!";i['escapeWithTheKing']="გაიქეცი მეფით!";i['evaluatePieceStrength']="გამოთვალე ფიგურის სიძლიერე";i['excellent']="გადასარევია!";i['exerciseYourTacticalSkills']="ივარჯიშე ტაქტიკურ სიძლიერეზე";i['findAWayToCastleKingSide']="ცადე გააკეთო მოკლე როქი!";i['findAWayToCastleQueenSide']="ცადე გააკეთო გრძელი როქი!";i['firstPlaceTheRooks']="ჯერ ეტლები დააწყე!\nკუთხეში დგანან.";i['fundamentals']="საფუძვლები";i['getAFreeLichessAccount']="მიიღე უფასო lichess პროფილი";i['grabAllTheStars']="აიყვანე ყველა ვარსკვლავი!";i['grabAllTheStarsNoNeedToPromote']="აიღე ყველა ვარსკლავი!\nგაყვანა საჭირო არაა.";i['greatJob']="შესანიშნავია!";i['howTheGameStarts']="როგორ იწყება თამაში";i['intermediate']="საშუალო";i['itMovesDiagonally']="მოძრაობს დიაგონალურად";i['itMovesForwardOnly']="მხოლოდ წინ მიდის";i['itMovesInAnLShape']="გადაადგილდება ლათინური ასო L - ს ფორმით";i['itMovesInStraightLines']="ის მოძრაობს სწორ ხაზებზე";i['itNowPromotesToAStrongerPiece']="ახლა უფრო ძლიერ ფიგურად გადაიქცევა.";i['keepYourPiecesSafe']="დაიცავი შენი ფიგურები";i['kingComplete']="შენ იცი მეფით თამაში!";i['kingIntro']="შენ მეფე ხარ. თუ დამარცხდები, თამაშს წააგებ.";i['knightComplete']="გილოცავ! შენ მხედარსაც დაეუფლე.";i['knightIntro']="ახლა კი პატარა გამოწვევა შენთვის. მხედარი... კვიმატი ფიგურაა.";i['knightsCanJumpOverObstacles']="მხედარს შეუძლია ფიგურებს გადაახტეს!\nაიღე ყველა ვარსკვლავი!";i['knightsHaveAFancyWay']="მხედარი მაგრად დახტის აქეთ-იქით!";i['lastOne']="საბოლოო!";i['learnChess']="ისწავლე ჭადრაკი";i['learnCommonChessPositions']="ისწავლე ცნობილი პოზიციები";i['letsGo']="დავიწყოთ!";i['mateInOne']="მატი ერთ სვლაში";i['mateInOneComplete']="გილოცავ! აი ასე იგებ ჭადრაკს!";i['mateInOneIntro']="როცა შენს ქიშს ვერაფერს უხერხებენ, იგებ.";i['menu']="მენიუ";i['mostOfTheTimePromotingToAQueenIsBest']="ხშირ შემთხვევებში ლაზიერის გაცოცხლება საუკეთესოა.\nთუმცა ხან მხედარად გადაქცევა სჯობს!";i['nailedIt']="დაგლიჯე.";i['next']="შემდეგი";i['nextX']=s("შემდეგი %s");i['noEscape']="გასაქცევი არაა,\nმაგრამ დაცვა შეგიძლია!";i['opponentsFromAroundTheWorld']="მოწინააღმდეგეები მსოფლიოს ყველა წერტილიდან";i['outOfCheck']="ქიშის გარეთ";i['outOfCheckComplete']="გილოცავ! შენს მეფეს ვერასდროს აიყვანენ!";i['outOfCheckIntro']="ქიში გითხრეს! უნდა დააღწიო თავი ან დაბლოკო შეტევა.";i['outstanding']="ძალიან კარგია!";i['pawnComplete']="გილოცავ! პაიკებზე ყველაფერი იცი.";i['pawnIntro']="პაიკი სუსტია მაგრამ დიდი შესაძლებლობები აქვს.";i['pawnPromotion']="პაიკის გაცოცხლება,, გაყვანა, გარდასახვა";i['pawnsFormTheFrontLine']="პაიკები ფრონტზე დგანან.\nგააკეთე სვლა.";i['pawnsMoveForward']="პაიკები წინ გადაადგილდებიან,\nმაგრამ იყვანენ დიაგონალურად!";i['pawnsMoveOneSquareOnly']="პაიკი გადადის ერთ უჯრით.\nმაგრამ როცა დაფის ბოლოში გადის, ძლიერი ფიგურა ხდება!";i['perfect']="იდეალურია!";i['pieceValue']="ფიგურის ღირებულება";i['pieceValueComplete']="გილოცავ! უკვე იცი რომ\nდედოფალი = 9\nეტლი = 5\nკუ = 3\nმხედარი = 3\nპაიკი = 1";i['pieceValueExchange']="აიყვანე ყველაზე ძლიერი ფიგურა!\nარ გაცვალო უფრო ძლიერი ფიგურა უფრო სუსტ ფიგურაში.";i['pieceValueIntro']="რაც უკეთ გადაადგილდება, მეტი ფასი აქვს!\nდედოფალი = 9\nეტლი = 5\nკუ = 3\nმხედარი = 3\nპაიკი = 1\nმეფე ყველაფერს ნიშნავს, დაკარგავ - წააგებ.";i['pieceValueLegal']="აიყვანე ყველაზე ძლიერი ფიგურა!\nდარწმუნდი რომ აკეთებ შესაძლებელ სვლას!";i['placeTheBishops']="კუები დაალაგე!\nმხედრის გვერდით დგანან.";i['placeTheKing']="დასვი მეფე!\nმისი ცოლის გვერდით.";i['placeTheQueen']="დასვი დედოფალი!\nთავისსავე ფერზე დგას.";i['play']="ითამაშე!";i['playMachine']="ეთამაშე კომპიუტერს";i['playPeople']="ეთამაშე სხვებს";i['practice']="პრაქტიკა";i['progressX']=s("პროგრესი %s");i['protection']="დაცვა";i['protectionComplete']="გილოცავ! ფიგურა, რომელსაც არ კარგავ, იგებ!";i['protectionIntro']="გამოარკვიე რომელ ფიგურებზე გიტევენ, და დაიცავი!";i['puzzleFailed']="თავსატეხში ჩაიჭერი!";i['puzzles']="თავსატეხები";i['queenCombinesRookAndBishop']="ლაზიერი = ეტლს + კუ";i['queenComplete']="გილოცავ! ყველაფერი იცი ლაზიერის შესახებ.";i['queenIntro']="ყველაზე ძლიერი ფიგურის ჯერია, მისი აღმატებულება ლაზიერი!";i['queenOverBishop']="აიყვანე ყველაზე\nძლიერი ფიგურა\nდედოფალი > კუ";i['register']="დარეგისტრირდი";i['resetMyProgress']="განაახლე ჩემი პროგრესი";i['retry']="ხელახლა";i['rightOn']="ზუსტადაც!";i['rookComplete']="გილოცავ! შენ წარმატებით დაეუფლე ეტლის სვლებს.";i['rookGoal']="დააჭირე ეტლს და მიიყვანე ვარკსვლავთან!";i['rookIntro']="ეტლი ძლიერი ფიგურაა. მზად ხარ მართო იგი?";i['selectThePieceYouWant']="აირჩიე სასურველი ფიგურა!";i['stageX']=s("ეტაპი %s");i['stageXComplete']=s("ეტაპი %s დასრულდა");i['stalemate']="პატი";i['stalemateComplete']="გილოცავ! წაგებას პატი ჯობია!";i['stalemateGoal']="პატისთვის:\n- არ უნდა შეეძლოს სვლის გაკეთება\n- არ უნდა ქონდეს ქიში.";i['stalemateIntro']="როცა მოთამაშეს ქიში არ აქვს გამოცხადებული და ლეგალური სვლა არ გააჩნია, პატი ხდება. თამაში ფრეა, არც იგებ, არც აგებ.";i['takeAllThePawnsEnPassant']="ყველა პაიკი გავლით აიყვანე!";i['takeTheBlackPieces']="აიყვანე შავები!";i['takeTheBlackPiecesAndDontLoseYours']="აიყვანე შავები!\nდა შენები არ დაკარგო.";i['takeTheEnemyPieces']="აიყვანე მოწინააღმდეგის ფიგურები";i['takeThePieceWithTheHighestValue']="აიყვანე ყველაზე ძლიერი ფიგურა!";i['testYourSkillsWithTheComputer']="გამოცადე ძალები კომპიუტერთან";i['theBishop']="კუ";i['theFewerMoves']="რაც ნაკლებ სვლას გააკეთებ, მეტ ქულას აიღებ!";i['theGameIsADraw']="თამაში ფრედ დასრულდა";i['theKing']="მეფე";i['theKingCannotEscapeButBlock']="მეფეს გასაქცევი არ აქვს,\nმაგრამ დაბლოკვა შეგიძლია!";i['theKingIsSlow']="მეფე ნელა მოძრაობს.";i['theKnight']="მხედარი";i['theKnightIsInTheWay']="მხედარია შუაში!\nგაწიე რო როქი გააკეთო.";i['theMostImportantPiece']="ყველაზე მნიშვნელოვანი ფიგურა";i['thenPlaceTheKnights']="ახლა მხედრები!\nეტლის გვერდზე დგანან.";i['thePawn']="პაიკი";i['theQueen']="ლაზიერი";i['theRook']="ეტლი";i['theSpecialKingMove']="სპეციალური მეფის სვლა";i['theSpecialPawnMove']="სპეციალური პაიკის სვლა";i['thisIsTheInitialPosition']="ეს საწყისი პოზიციაა ყველა თამაშში!\nგააკეთე სვლა.";i['thisKnightIsCheckingThroughYourDefenses']="მხედარი ქიშს გეუბნება!";i['twoMovesToGiveCheck']="ორი სვლა ქიშისთვის";i['useAllThePawns']="გამოიყენე ყველა პაიკი!\nგაცოცხლება საჭირო არაა.";i['useTwoRooks']="გამოიყენე ორი ეტლი რათა მალე მოიგო!";i['videos']="ვიდეოები";i['watchInstructiveChessVideos']="უყურე ინსტრუქციულ ჭადრაკის ვიდეოებს";i['wayToGo']="ეგრე, ეგრე!";i['whatNext']="შემდეგ?";i['yesYesYes']="კი, კი, კი!";i['youCanGetOutOfCheckByTaking']="ქიშს დაბლოკავ თუ შემტევ ფიგურას აიყვან.";i['youCannotCastleIfAttacked']="როქს ვერ გააკეთებ ქიშის დროს.\nჯერ ქიში დაბლოკე!";i['youCannotCastleIfMoved']="როქს ვერ გააკეთებ \nთუ მეფე ან ეტლი უკვე\nგადაადგილდა.";i['youKnowHowToPlayChess']="გილოცავ, თამაში იცი! გინდა უფრო ძლიერი მოთამაშე იყო?";i['youNeedBothBishops']="ერთი ღია-უჯრის კუ,\nერთი მუქი-უჯრის კუ.\nორივე გჭირდება!";i['youreGoodAtThis']="კარგი ხარ!";i['yourPawnReachedTheEndOfTheBoard']="შენი პაიკი დაფის ბოლოში გავიდა!";i['youWillLoseAllYourProgress']="დაკარგავთ თქვენს პროგრესს!"})()