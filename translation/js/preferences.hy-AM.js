"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.preferences)window.i18n.preferences={};let i=window.i18n.preferences;i['bellNotificationSound']="Ծանուցումների զանգակի ձայնը";i['boardCoordinates']="Խաղատախտակի համակարգում (A-H, 1-8)";i['boardHighlights']="Խաղատախտակի բնութագիր (վերջին քայլը և շախը)";i['bothClicksAndDrag']="Երկուսն էլ";i['castleByMovingOntoTheRook']="Արքան տեղափոխել նավակի վրա";i['castleByMovingTheKingTwoSquaresOrOntoTheRook']="Փոխատեղման եղանակը";i['castleByMovingTwoSquares']="Արքան տեղափոխել երկու վանդակ";i['chessClock']="շախմատի ժամացույց";i['chessPieceSymbol']="Շախմատային խաղաքարի պատկերանշան";i['claimDrawOnThreefoldRepetitionAutomatically']="Քայլերի եռակի կրկնության դեպքում ինքնաբերաբար պահանջել ոչ-ոքի";i['clickTwoSquares']="Երկու վանդակները սեղմելով";i['confirmResignationAndDrawOffers']="Հաստատել պարտությունը և ոչ-ոքիի առաջարկը";i['correspondenceAndUnlimited']="Նամակագրական և առանց ժամանակի սահմանափակման";i['display']="Ցուցադրել";i['displayBoardResizeHandle']="Ցույց տալ խաղատախտակի չափսի փոփոխության պատկերագիրը";i['dragPiece']="Խաղաքարը տեղափոխելով";i['explainShowPlayerRatings']="Հնարավորություն է տալիս թաքցնելու կայքի բոլոր վարկանիշները՝ խաղի վրա կենտրոնանալու համար։ Պարտիաները մնում են վարկանիշային, պարզապես Դուք դա չեք տեսնի։";i['gameBehavior']="Խաղի կարգավորումներ";i['giveMoreTime']="Ավելացնել ժամանակ";i['horizontalGreenProgressBars']="Հորիզոնական կանաչ գծով";i['howDoYouMovePieces']="Ինչպե՞ս տեղաշարժել խաղաքարերը։";i['inCasualGamesOnly']="Միայն ընկերական խաղերում";i['inCorrespondenceGames']="Նամակագրական խաղեր";i['inGameOnly']="Միայն խաղի մեջ";i['inputMovesWithTheKeyboard']="Քայլերը մուտքագրել ստեղնաշարով";i['inputMovesWithVoice']="Քայլերի ներմուծումը ձայնի միջոցով";i['materialDifference']="Ցուցադրել նյութական տարբերությունը";i['moveConfirmation']="Քայլի հաստատում";i['moveListWhilePlaying']="քայլերի ցուցակը խաղի ժամանակ";i['notifications']="Ծանուցումներ";i['notifyBell']="Lichess-ի ձայնային տեղեկացում";i['notifyDevice']="Սարք";i['notifyGameEvent']="Նամակագրական խաղին առնչվող թարմացումներ";i['notifyInvitedStudy']="Ստուդիայի հրավեր";i['notifyTimeAlarm']="Նամակագրական խաղում ժամանակը շուտով կսպառվի";i['notifyTournamentSoon']="Մրցաշարը շուտով կսկսվի";i['notifyWeb']="Դիտարկիչ";i['onlyOnInitialPosition']="Միայն սկզբնական դիրքում";i['pgnLetter']="Խաղաքարի տառը (K, Q, R, B, N)";i['pgnPieceNotation']="Շախմատային նոտագրություն";i['pieceAnimation']="Խաղաքարերի ձևավորում";i['pieceDestinations']="Ցույց տալ թույլատրելի քայլերը";i['preferences']="Կարգավորումներ";i['premovesPlayingDuringOpponentTurn']="Նախնական քայլ (քանի դեռ մրցակիցը մտածում է)";i['privacy']="Գաղտնիություն";i['promoteToQueenAutomatically']="Ավտոմատ փոխակերպվել թագուհու";i['sayGgWpAfterLosingOrDrawing']="Պարտությունից կամ ոչ-ոքիից հետո զրուցարանում գրել. «Good game, well played»";i['scrollOnTheBoardToReplayMoves']="Քայլերը դիտելու համար մկնիկի անիվը պտտեք խաղատախտակի վրա";i['showPlayerRatings']="Ցուցադրել խաղացողի վարկանիշը";i['snapArrowsToValidMoves']="Սլաքներով ցույց տալ միայն թույլատրելի քայլերը";i['soundWhenTimeGetsCritical']="ձայնով, երբ ժամանակը վերջանում է";i['takebacksWithOpponentApproval']="քայլը հետ վերցնելու առաջարկ (հակառակորդի թույլտվությամբ)";i['tenthsOfSeconds']="ցուցադրել վայրկյանները";i['whenPremoving']="Նախաքայլ անելիս";i['whenTimeRemainingLessThanTenSeconds']="Երբ ժամանակը մնացել է <10 վայրկյան";i['whenTimeRemainingLessThanThirtySeconds']="Երբ մնում է < 30 վայրկյանից քիչ";i['yourPreferencesHaveBeenSaved']="Ձեր նախընտրությունները պահպանված են";i['zenMode']="Ձեն ռեժիմ"})()