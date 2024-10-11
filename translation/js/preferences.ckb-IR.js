"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.preferences)window.i18n.preferences={};let i=window.i18n.preferences;i['bellNotificationSound']="دەنگی زەنگی ئاگادارکردنەوە";i['boardCoordinates']="ڕێکخەری بۆرد (A-H، 1-8)";i['boardHighlights']="جیاکردنەوەی تایبەتمەندیەکانی بۆردەکە (دوا جوڵە و کش)";i['bothClicksAndDrag']="هەردووكيان";i['castleByMovingOntoTheRook']="پاشا بەرەو قەڵایەکە بجوڵێنە";i['castleByMovingTheKingTwoSquaresOrOntoTheRook']="شێوازی ماڵکردن";i['castleByMovingTwoSquares']="دوو خانە پاشا بجوڵێنە";i['chessClock']="کاتژمێری شەترەنج";i['chessPieceSymbol']="هێمای پارچەی شەترەنج";i['claimDrawOnThreefoldRepetitionAutomatically']="داواکاری لەسەر دووبارەبوونەوەی سێ جار لەسەر یەک بە شێوەیەکی ئۆتۆماتیکی یاریەکە یەکسان دەبێت";i['clickTwoSquares']="کلیک کرد دووجار لەسەر چوارگۆشەکە";i['confirmResignationAndDrawOffers']="تەسلیمبون و پێشنیاری یەکسانبون قبوڵ بکە";i['correspondenceAndUnlimited']="نامە ناردن بەبێ کات";i['correspondenceEmailNotification']="ئیمەیڵی ڕۆژانە کە نامە ناردنەکەت لە یاریەکاندا دەخاتە ڕوو";i['display']="نیشاندان";i['displayBoardResizeHandle']="گۆڕینی قەبارەی بۆرد پیشان بدە";i['dragPiece']="پارچەیەک ڕابکێشە";i['explainCanThenBeTemporarilyDisabled']="ئەم تایبەتمەندییە دەتوانرێت لەناو یارییەکەدا بە بەکارهێنانی لیستی تەختەی شەتڕەنجەکە لەکاربخرێت";i['explainPromoteToQueenAutomatically']="اضغط مفتاح<ctrl> اثناء الترقية لتعطيل الترقية التلقائية مؤقتاً";i['explainShowPlayerRatings']="ئەمەش ڕێگە بە شاردنەوەی هەموو ئیلۆکان دەدات لە ماڵپەڕەکە، بۆ ئەوەی یارمەتیت بدات لە سەرنجدان لەسەر شەترەنج، یارییەکان هێشتا دەتوانرێت هەڵسەنگاندنیان بۆ بکرێت، ئەمە تەنها پەیوەندی بەوە ھەیە کە چی دەبینی.";i['gameBehavior']="هەڵسوکەوتی یاری";i['giveMoreTime']="کاتێکی زیاتر ببەخشە";i['horizontalGreenProgressBars']="ئەو هێڵە ئاسۆییەکانەی کە کاتێک جوڵە دەکەی بەرەو پێش ڕەنگی سەوز دەبێت";i['howDoYouMovePieces']="چۆن پارچەکان دەجوڵێنیت?";i['inCasualGamesOnly']="تەنها لەو یاریانەی بێ ڕایتینگن";i['inCorrespondenceGames']="نامەناردن لە یاریدا";i['inGameOnly']="تەنیا لەناو یاری";i['inputMovesWithTheKeyboard']="چوونە ژوورەوە بە کیبۆردەکە دەجوڵێت";i['inputMovesWithVoice']="فرمانی جوڵە بکە بە دەنگت";i['materialDifference']="جیاوازی نێوان تۆ و بەرامبەرەکەت لە ڕووی پارچەوە";i['moveConfirmation']="پشتڕاستکردنەوەی جوڵە";i['moveListWhilePlaying']="لیستی جوڵە لەکاتی یاریکردندا";i['notifications']="ئاگادارکردنەوەکان";i['notifyBell']="زەنگی ئاگادارکردنەوەی لە ناو Lichess";i['notifyChallenge']="بەرەنگاریەکان";i['notifyDevice']="ئامێر";i['notifyForumMention']="کۆمێنتێک لە مەکۆکەدا ئاماژە بە تۆ دەکات";i['notifyGameEvent']="نوێکردنەوەی نامەناردن لە یاریدا";i['notifyInboxMsg']="پەیامی نوێ";i['notifyInvitedStudy']="بانگهێشتنامەی خوێندن";i['notifyPush']="ئاگادارکردنەوەی ئامێرەکەت کاتێک لەسەر Lichess نیت";i['notifyStreamStart']="کەسێک دەچێتە پەخشی ڕاستەوخۆ";i['notifyTimeAlarm']="کاتژمێری نامەنارد تەواو بووە";i['notifyTournamentSoon']="بەم زووانە پاڵەوانێتییەکە دەستپێدەکات";i['notifyWeb']="وێبگەڕ";i['onlyOnInitialPosition']="تەنها لەسەر شوێنی سەرەتایی";i['pgnLetter']="پیتی (K, Q, R, B, N)";i['pgnPieceNotation']="نووسینی جوڵە";i['pieceAnimation']="جوڵەی پارچەکان بەشێوەی ئەنیمەیشن";i['pieceDestinations']="شوێنی مەبەستی پارچە (جوڵە دروستەکان و جوڵە کردن بێ ئەوەی چاوەڕێی بەرامبەرەکەت بکەی)";i['preferences']="ھەڵبژاردنەکان";i['premovesPlayingDuringOpponentTurn']="جوڵەی پێشوەختە (یاریکردن لە کاتی نۆرەی بەرامبەر)";i['privacy']="پاراستنی نهێنی(نیشان نەدان)";i['promoteToQueenAutomatically']="بە شێوەیەکی ئۆتۆماتیکی بەرز دەبێتەوە بۆ شاژن";i['sayGgWpAfterLosingOrDrawing']="لە کاتی شکست یان یەکسانبووندا بڵێ \\\"یارییەکی باش، بە باشی یاریت کرد\\\"";i['scrollOnTheBoardToReplayMoves']="بۆ دووبارەکردنەوەی جوڵەکان، تختەی شەتڕەنجەکە بەرەو سەر و خوار بجوڵێنە";i['showPlayerRatings']="مستەوای یاریزانەکان پیشان بدە";i['snapArrowsToValidMoves']="خەتەکان بە ئاراستەیەکی دروستدا ڕابکێشە";i['soundWhenTimeGetsCritical']="ھەبوونی دەنگ کاتێک کات گرینگتر دەبێت";i['takebacksWithOpponentApproval']="پەشیمانبونەوە لە جوڵەیەک (بە ڕەزامەندی بەرامبەر)";i['tenthsOfSeconds']="بەشی دووەم";i['whenPremoving']="لە کاتی جوڵەی پێشوەختەدا";i['whenTimeRemainingLessThanTenSeconds']="کاتێک کات دەمێنێتەوە < ١٠ چرکە";i['whenTimeRemainingLessThanThirtySeconds']="کاتێک کات دەمێنێتەوە < ٣٠ چرکە";i['yourPreferencesHaveBeenSaved']="هەڵبژاردنەکانت هەڵگیراون.";i['zenMode']="دۆخی zen"})()