"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.preferences)window.i18n.preferences={};let i=window.i18n.preferences;i['bellNotificationSound']="Qo‘ng‘iroq xabari tovushi";i['boardCoordinates']="Doska koordinatalari (A-H, 1-8)";i['boardHighlights']="Doskadagi rang bila ajratsih (oxirgi yurish va shoh)";i['bothClicksAndDrag']="Ikkalasi ham";i['castleByMovingOntoTheRook']="Qirolni to\\'ra tomonga yuring";i['castleByMovingTheKingTwoSquaresOrOntoTheRook']="Rokirovka usuli";i['castleByMovingTwoSquares']="Qirolni ikki katakka yuring";i['chessClock']="Shaxmat Soati";i['chessPieceSymbol']="Sipoh belgisi";i['claimDrawOnThreefoldRepetitionAutomatically']="Avtomatik tarzda uch karra takrorlashga ariza berish";i['clickTwoSquares']="Ikkita kvadratni bosing";i['confirmResignationAndDrawOffers']="Durrangga rozi bo\\'lib, o\\'yinni yakunlashni tasdiqlash";i['correspondenceAndUnlimited']="Yozishmali va vaqtda chegaralanmagan";i['display']="Displey";i['displayBoardResizeHandle']="Doskani o\\'lchamini o\\'gartiruvchi uskunani namoyishi";i['dragPiece']="Sipohni suring";i['explainShowPlayerRatings']="Bu barcha reytinglarni websiteda yashirib fokusni faqat shaxmatga qaratish imkonini beradi. O‘yinlar reytingli bo‘lishi saqlanib qoladi. Bu faqat siz nimani ko‘rishingiz mumkinligi haqida.";i['gameBehavior']="O\\'yin hatti harakatlari";i['giveMoreTime']="Takroran berish";i['horizontalGreenProgressBars']="Gorizantal yashil rangdagi jarayon indikatori";i['howDoYouMovePieces']="Sipohlarni qanday yurasiz?";i['inCasualGamesOnly']="Faqat vaqtinchalik o\\'yinlarda";i['inCorrespondenceGames']="Yozishmali o\\'yinlar";i['inGameOnly']="Faqat o‘yin ichida";i['inputMovesWithTheKeyboard']="Yurish klaviatura orqali amalga oshiriladi";i['materialDifference']="Materiallar farqi";i['moveConfirmation']="Yurishni tasdiqlash";i['moveListWhilePlaying']="O\\'yin davomida yurishlar ro\\'yhatini ko\\'rsatish";i['notifications']="Bildirishnomalar";i['notifyBell']="Lichess ichidagi qo‘ng‘iroqli xabar";i['notifyDevice']="Qurilma";i['notifyPush']="Lichessdan tashqarida bo‘lganingizdagi qurilma xabari";i['notifyWeb']="Brauzer";i['onlyOnInitialPosition']="Faqat boshlang\\'ich holatda";i['pgnLetter']="Harflar (K, Q, R, B, N)";i['pgnPieceNotation']="Yurishlarni qayd qilib borish";i['pieceAnimation']="Qism animatsiyalar";i['pieceDestinations']="Mumkin bo\\'lgan yurishlani ko\\'rsatish";i['preferences']="Shaxsiy sozlanmalar";i['premovesPlayingDuringOpponentTurn']="Oldindan yurishlar (sherik yurganicha yurish)";i['privacy']="Maxfiylik";i['promoteToQueenAutomatically']="Qirolicha darajasigacha avtomatik tarzda oshirish";i['sayGgWpAfterLosingOrDrawing']="Magʻlubiyatga uchraganingizda \\\"Yaxshi oʻyin, yaxshi oʻnaldi\\\" deb ayting";i['showPlayerRatings']="Oʻyinchi reytinglarini koʻrsatish";i['snapArrowsToValidMoves']="Ishonchli yurishlarga oʻqlarni torting";i['soundWhenTimeGetsCritical']="Vaqt kritik holatga kelganda ovoz ovoz berish";i['takebacksWithOpponentApproval']="Ortga qaytarishlar (sherik roziligi bilan)";i['tenthsOfSeconds']="10 lab sekundlar";i['whenPremoving']="Oldindan yurilganda";i['whenTimeRemainingLessThanTenSeconds']="Qachonki vaqt tugashiga < 10 sekundlar bo\\'lsa";i['whenTimeRemainingLessThanThirtySeconds']="Qachonki vaqt tugashiga < 30 sekundlar bo\\'lsa";i['yourPreferencesHaveBeenSaved']="Sizning sozlashlaringiz saqlandi.";i['zenMode']="Zen holati (faqat doska va soat)"})()