"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.storm)window.i18n.storm={};let i=window.i18n.storm;i['accuracy']="Độ chính xác";i['allTime']="Trước tới nay";i['bestRunOfDay']="Lần chạy tốt nhất của ngày";i['clickToReload']="Nhấn để tải lại";i['combo']="Chuỗi";i['createNewGame']="Tạo ván đấu mới";i['endRun']="Kết thúc (phím nhanh: Enter)";i['failedPuzzles']="Các thế cờ đã giải sai";i['getReady']="Chuẩn bị!";i['highestSolved']="Câu giải được khó nhất";i['highscores']="Điểm cao";i['highscoreX']=s("Điểm cao: %s");i['joinPublicRace']="Tham gia một cuộc đua công khai";i['joinRematch']="Tham gia đấu lại";i['joinTheRace']="Tham gia cuộc đua!";i['moves']="Các nước đi";i['moveToStart']="Đi quân để bắt đầu";i['newAllTimeHighscore']="Điểm cao mới!";i['newDailyHighscore']="Điểm cao hàng ngày mới!";i['newMonthlyHighscore']="Điểm cao hàng tháng mới!";i['newRun']="Chạy lượt mới (phím nhanh: Dấu cách)";i['newWeeklyHighscore']="Điểm cao hàng tuần mới!";i['nextRace']="Cuộc đua kế tiếp";i['playAgain']="Chơi lại";i['playedNbRunsOfPuzzleStorm']=p({"other":"Đã chơi %1$s lượt %2$s"});i['previousHighscoreWasX']=s("Điểm cao trước đây là %s");i['puzzlesPlayed']="Các thế cờ đã chơi";i['puzzlesSolved']="câu đố đã giải";i['raceComplete']="Cuộc đua kết thúc!";i['raceYourFriends']="Đua với bạn bè của bạn";i['runs']="Số lần chạy";i['score']="Điểm";i['skip']="bỏ qua";i['skipExplanation']="Bỏ qua nước đi này để giữ chuỗi của bạn! Chỉ dùng được một lần mỗi cuộc đua.";i['skipHelp']="Bạn có thể bỏ qua một nước đi mỗi ván chơi:";i['skippedPuzzle']="Câu đố đã bỏ qua";i['slowPuzzles']="Các thế cờ làm chậm";i['spectating']="Đang xem";i['startTheRace']="Bắt đầu cuộc đua";i['thisMonth']="Tháng này";i['thisRunHasExpired']="Lượt đua này đã quá hạn!";i['thisRunWasOpenedInAnotherTab']="Lượt đua này đang được mở trong một tab khác!";i['thisWeek']="Tuần này";i['time']="Thời gian";i['timePerMove']="Thời gian trên mỗi nước đi";i['viewBestRuns']="Xem các lần chạy tốt nhất";i['waitForRematch']="Đang đợi đấu lại";i['waitingForMorePlayers']="Đang đợi thêm người chơi tham gia...";i['waitingToStart']="Đang đợi bắt đầu";i['xRuns']=p({"other":"%s lần chơi"});i['youPlayTheBlackPiecesInAllPuzzles']="Bạn cầm quân đen ở tất cả các câu đố";i['youPlayTheWhitePiecesInAllPuzzles']="Bạn cầm quân trắng ở tất cả các câu đố";i['yourRankX']=s("Thứ hạng của bạn: %s")})()