"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.preferences)window.i18n.preferences={};let i=window.i18n.preferences;i['boardCoordinates']="বোর্ডের স্থানাংকর (A-H, 1-8)";i['boardHighlights']="বোর্ডের বৈশিষ্ট সমূহ (শেষ চাল এবং পরীক্ষা)";i['bothClicksAndDrag']="উভয়ের";i['castleByMovingOntoTheRook']="দাবার নৌকার উপর দিয়ে রাজাকে সরান";i['castleByMovingTheKingTwoSquaresOrOntoTheRook']="কাস্টলিং পদ্বতি";i['castleByMovingTwoSquares']="দুই বর্গে রাজা সরান";i['chessClock']="দাবার ঘড়ি";i['chessPieceSymbol']="দাবার টুকরোর ধরন";i['claimDrawOnThreefoldRepetitionAutomatically']="সংক্রিয়ভাবে দাবি আঁকা বিষয়ে তিনগুন অনুবর্তন";i['clickTwoSquares']="দুই স্থান নির্বাচন করুন";i['confirmResignationAndDrawOffers']="পদত্যাগ নিশ্চিত করুন এবং অফার লুটে নিন";i['correspondenceAndUnlimited']="সাদৃশ্য এবং সীমাহীন";i['display']="ডিসপ্লে";i['dragPiece']="এক টুকরো স্থান্তর করুন";i['giveMoreTime']="আরো কিছু সময় দিন";i['horizontalGreenProgressBars']="সমতল সবুজ অগ্রগতি বার";i['howDoYouMovePieces']="কিভাবে আপনি টুকরোগুলো সরাবেন?";i['inCasualGamesOnly']="শুধু অনিয়মিত খেলার মধ্যে";i['inCorrespondenceGames']="সাদৃশ্য খেলাগুলো";i['inputMovesWithTheKeyboard']="কী-বোর্ডের সাথে নিবেশ সরান";i['materialDifference']="গুটির মধ্যে ব্যবধান";i['moveConfirmation']="অনুমোদন সরান";i['moveListWhilePlaying']="খেলার সময় তালিকা সরান";i['notifications']="নোটিফিকেশন";i['pgnLetter']="বর্ণ (কে, কিউ, আর, বি, এন)";i['pgnPieceNotation']="প্রতীক চিহ্ন সরান";i['pieceAnimation']="গুটির অ্যানিমেশন";i['pieceDestinations']="টুকরো গন্তব্যস্থল (বৈধ চাল ও আবার চাল)";i['preferences']="পছন্দসমূহ";i['premovesPlayingDuringOpponentTurn']="পূনারয়চালন (প্রতিপক্ষের মোর নেওয়ার সময়)";i['privacy']="গোপনীয়তা";i['promoteToQueenAutomatically']="সংক্রিয়ভাবে রানীর জন্য প্রচার";i['soundWhenTimeGetsCritical']="যখন সময় জটিল হয়ে যায় শব্দ";i['takebacksWithOpponentApproval']="পিছনে নেও (প্রতিদ্বন্দ্বীর অনুমদনের সাথে)";i['tenthsOfSeconds']="সেকেন্ডের কাটা";i['whenPremoving']="যখন পূনার‍য়সারান";i['whenTimeRemainingLessThanTenSeconds']="যখন সময় বাকি < 10 সেকেন্ড";i['whenTimeRemainingLessThanThirtySeconds']="যখন সময় বাকি < 30 সেকেন্ড";i['yourPreferencesHaveBeenSaved']="আপনার পচ্ছন্দগুলি সংরক্ষিত করা হয়েছে.";i['zenMode']="জেন মোড"})()