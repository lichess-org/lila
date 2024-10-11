"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.preferences)window.i18n.preferences={};let i=window.i18n.preferences;i['bellNotificationSound']="زنگ اعلان";i['boardCoordinates']="مختصات صفحه(A-H، 1-8)";i['boardHighlights']="رنگ‌نمایی صفحه (آخرین حرکت و کیش)";i['bothClicksAndDrag']="هر دو";i['castleByMovingOntoTheRook']="شاه را روی رخ گذارید";i['castleByMovingTheKingTwoSquaresOrOntoTheRook']="روش قلعه‌روی";i['castleByMovingTwoSquares']="شاه را دو خانه حرکت دهید";i['chessClock']="ساعت شطرنج";i['chessPieceSymbol']="نماد مهره";i['claimDrawOnThreefoldRepetitionAutomatically']="ادعای خودکار تساوی، در تکرار سه‌گانه";i['clickTwoSquares']="انتخاب دو مربع مبدا و مقصد";i['confirmResignationAndDrawOffers']="نیاز به تایید دوباره؛ زمانی که تسلیم می شوید یا پیشنهاد تساوی می دهید";i['correspondenceAndUnlimited']="مکاتبه ای و نامحدود";i['correspondenceEmailNotification']="ایمیل های روزانه که بازی های شبیه شما را به صورت لیست درمی‌آورند";i['display']="صفحه نمایش";i['displayBoardResizeHandle']="نمایش دستگیره برای تغییر اندازه صفحه";i['dragPiece']="کشیدن یک مهره";i['explainCanThenBeTemporarilyDisabled']="می‌تواند هنگام بازی با «گزینگان صفحه‌بازی» خاموش شود";i['explainPromoteToQueenAutomatically']="<ctrl> را در هنگام تبلیغ بزنید تا به طور موقت تبلیغات خودکار را غیرفعال کنید";i['explainShowPlayerRatings']="این گزینه همه درجه‌بندی‌ها در Lichess را پنهان می‌کند تا کمک کند روی شطرنج تمرکز کنید. بازی‌های رسمی همچنان بر درجه‌بندی‌تان تاثیر می‌گذارند، این گزینه فقط مربوط به دیدن/ندیدن درجه‌بندی‌هاست.";i['gameBehavior']="تنظیمات بازی";i['giveMoreTime']="افزایش زمان حریف";i['horizontalGreenProgressBars']="نمودار زمان سبز رنگ افقی";i['howDoYouMovePieces']="تمایل دارید که چگونه مهره ها را حرکت دهید؟";i['inCasualGamesOnly']="فقط در بازی‌های نارسمی";i['inCorrespondenceGames']="در حال بازی مکاتبه ای";i['inGameOnly']="تنها در بازی";i['inputMovesWithTheKeyboard']="ورود حرکات با استفاده از صفحه کلید";i['inputMovesWithVoice']="حرکات را با صدای خود وارد کنید";i['materialDifference']="تفاوت مُهره‌ها";i['moveConfirmation']="تایید حرکت";i['moveListWhilePlaying']="لیست حرکات هنگام بازی کردن";i['notifications']="اعلانات";i['notifyBell']="زنگوله اعلانات لیچس";i['notifyChallenge']="پیشنهاد بازی";i['notifyDevice']="دستگاه";i['notifyForumMention']="در انجمن از شما نام‌بُرده‌اند";i['notifyGameEvent']="اعلان به روزرسانی بازی";i['notifyInboxMsg']="پیام جدید";i['notifyInvitedStudy']="دعوت به مطالعه";i['notifyPush']="اعلانات برای زمانی که شما در لیچس نیستید";i['notifyStreamStart']="استریمر شروع به فعالیت کرد";i['notifyTimeAlarm']="هشدار تنگی زمان";i['notifyTournamentSoon']="تورنمت به زودی آغاز می شود";i['notifyWeb']="مرورگر";i['onlyOnInitialPosition']="فقط در وضعیت آغازین";i['pgnLetter']="حرف (K, Q, R, B, N)";i['pgnPieceNotation']="نشانه‌گذاری حرکات";i['pieceAnimation']="حرکت مهره ها";i['pieceDestinations']="مقصد مهره(حرکت معتبر و پیش حرکت )";i['preferences']="تنظیمات";i['premovesPlayingDuringOpponentTurn']="پیش حرکت (بازی در نوبت حریف)";i['privacy']="امنیت و حریم شخصی";i['promoteToQueenAutomatically']="ارتقا خودکار به وزیر";i['sayGgWpAfterLosingOrDrawing']="گفتن \\\"بازی خوبی بود، خوب بازی کردی\\\" در هنگام باخت یا تساوی";i['scrollOnTheBoardToReplayMoves']="برای بازپخش حرکت‌ها، روی صفحه بازی بِنَوَردید";i['showFlairs']="نمایش نشان بازیکنان";i['showPlayerRatings']="نشان دادن درجه‌بندی بازیکنان";i['snapArrowsToValidMoves']="چسبیدن پیکان‌ها به حرکت‌های ممکن";i['soundWhenTimeGetsCritical']="صدا در هنگام زمان بحرانی زده می شود";i['takebacksWithOpponentApproval']="پس گرفتن حرکت (با تایید حریف)";i['tenthsOfSeconds']="دهم ثانیه";i['whenPremoving']="در زمان پیش حرکت";i['whenTimeRemainingLessThanTenSeconds']="وقتی زمان باقی مانده کمتر از ده ثانیه می باشد";i['whenTimeRemainingLessThanThirtySeconds']="وقتی زمان باقی مانده کمتر از سی ثانیه است";i['yourPreferencesHaveBeenSaved']="تغییرات شما ذخیره شده است";i['zenMode']="حالت ذن"})()