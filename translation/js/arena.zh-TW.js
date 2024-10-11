"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.arena)window.i18n.arena={};let i=window.i18n.arena;i['allAveragesAreX']=s("所有平均為%s。");i['allowBerserk']="允許啟用狂暴模式";i['allowBerserkHelp']="讓玩家將時間減半以獲得額外積分";i['allowChatHelp']="讓玩家在聊天室討論";i['arena']="競技場";i['arenaStreaks']="競技場連勝";i['arenaStreaksHelp']="2次勝利後，連勝將會取得4分，而不是2分。";i['arenaTournaments']="錦標賽";i['averagePerformance']="平均表現";i['averageScore']="平均分數";i['berserk']="狂暴選項";i['berserkAnswer']="狂暴模式若開啟，玩家時間將會砍半，不過在獲勝時將會多 1 積分\n\n在狂暴模式下，加時制將會被停用（1+2 是一個例外，它將會改為 1+0）。\n\n狂暴模式將不會被允許在零初始時間模式時開啟（例如：0+1，0+2）。\n\n狂暴模式只會在您下了 7 步棋以上才會得到加成分數。";i['bestResults']="最佳紀錄";i['created']="已建立";i['customStartDate']="自定義開始日期";i['customStartDateHelp']="在您的當地時區，這將會覆蓋在 \\\"比賽準備時間\\\" 選項";i['defender']="防守者";i['drawingWithinNbMoves']=p({"other":"在%s步內平局的棋局將不會得到積分"});i['drawStreakStandard']=s("連和場數：當棋手在競技場中連續和棋時，只有第一次和棋或者超過 %s 步的和局才會計分。 連和場數只能被勝利打破，不會被輸棋或和棋打破。");i['drawStreakVariants']="計分的和局的最小步數由不同的變體而變化。 下面的表格列出了每個變體的閥值。";i['editTeamBattle']="編輯團隊比賽";i['editTournament']="編輯錦標賽";i['history']="先前的重要錦標賽";i['howAreScoresCalculated']="分數是如何計算的？";i['howAreScoresCalculatedAnswer']="贏家將會得到2積分，平局1積分，輸家0分。\n如果您連續贏了2場以上的棋局，您將會開始以火焰符號標示的連勝次數。\n在連勝後您將會收到兩倍的積分直到輸棋。\n換句話說：贏得 4 積分，平局得 2 積分，但是輸家依舊沒有積分。\n\n例如：連贏兩場加上一局平局，您將會得到 2 + 2 + ( 2 x 1 ) = 6 積分";i['howDoesItEnd']="錦標賽何時會結束？";i['howDoesItEndAnswer']="每場錦標賽都會有一個倒數計時器，當它歸零時，錦標賽的排名就會固定，排名將會被顯示。如果有比賽在錦標賽結束後還沒完成，您還是得完成它，但是積分將不會被算進錦標賽的積分裡。";i['howDoesPairingWork']="對手是如何配對的?";i['howDoesPairingWorkAnswer']="在錦標賽開始時，系統將會以您的模式評分為基礎分配對手。\n在您完成了第一場棋局後，系統將會分配與您排名相近的玩家作為對手，這會需要一點時間做分配。\n迅速完成棋局並獲勝就可以得到更多積分。";i['howIsTheWinnerDecided']="贏家是怎麼決定的？";i['howIsTheWinnerDecidedAnswer']="在錦標賽中獲得最多積分的人將獲勝，當兩位(或以上)玩家擁有相同積分時，這場錦標賽將會被視為平手";i['isItRated']="這一局棋會被評分嗎？";i['isNotRated']="這場錦標賽不會評分，並且它不會影響到您的評分。";i['isRated']="這場錦標賽將會評分，並且影響到您的評分。";i['medians']="中位數";i['minimumGameLength']="最少對局步數";i['myTournaments']="我的錦標賽";i['newTeamBattle']="新團隊戰";i['noArenaStreaks']="無競技場連勝";i['noBerserkAllowed']="禁止「神速」模式";i['onlyTitled']="僅限頭銜玩家";i['onlyTitledHelp']="需要官方頭銜才能加入比賽";i['otherRules']="其他重要的規則";i['pickYourTeam']="選擇隊伍";i['pointsAvg']="平均分數";i['pointsSum']="總計分數";i['rankAvg']="平均等地";i['rankAvgHelp']="平均等地表示你的等地百分比。越低越好。\n\n舉例而言，在 100 人中被評等地 3 表示 %3；在 1000 人中被評等地 10 表示 %1";i['recentlyPlayed']="最近玩過";i['shareUrl']=s("分享這個網址讓其他人加入這場錦標賽：%s");i['someRated']="一些錦標賽將會評分，並且影響到您的評分。";i['stats']="目前狀態";i['thereIsACountdown']="在您開始您的第一步棋前將會有一個倒數計時器，在倒數結束前如果沒有下出您的第一步，您的對手將會直接獲勝";i['thisIsPrivate']="這是一個私人的錦標賽";i['total']="總計";i['tournamentShields']="錦標賽徽章";i['tournamentStats']="錦標賽得分";i['tournamentWinners']="錦標賽贏家";i['variant']="變體";i['viewAllXTeams']=p({"other":"查看所有 %s 團隊"});i['whichTeamWillYouRepresentInThisBattle']="你會為哪一個隊伍代表比賽？";i['willBeNotified']="您將會在錦標賽開始時收到通知，所以您可以放心的在錦標賽開始前下其他的棋局。";i['youMustJoinOneOfTheseTeamsToParticipate']="你必須參加某個隊伍以比賽！"})()