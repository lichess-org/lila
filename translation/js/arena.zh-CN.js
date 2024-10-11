"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.arena)window.i18n.arena={};let i=window.i18n.arena;i['allAveragesAreX']=s("此页面上的所有平均值是 %s。");i['allowBerserk']="允许神速模式";i['allowBerserkHelp']="允许玩家将自己的时间减半以获得额外得分";i['allowChatHelp']="允许玩家在聊天室中讨论";i['arena']="竞技场";i['arenaStreaks']="连胜奖励";i['arenaStreaksHelp']="连胜两场后，获得4分而不是2分。";i['arenaTournaments']="竞技场";i['averagePerformance']="平均表现";i['averageScore']="平均分数";i['berserk']="神速锦标赛";i['berserkAnswer']="如果棋手在棋局开始时点击“神速”按钮，将会损失一半的时间，但战胜该局将多得1分。\n\n如果棋局时限有加秒，那么启动神速会导致神速方加秒被取消。(唯一的例外是1+2, 在这个情况下启动神速会把神速方的时限变为1+0)。\n\n在无基础局时的棋局 (如0+1、0+2等) ，神速功能被禁用。\n\n只有走过7步棋以上的棋局会有神速加分。";i['bestResults']="最佳结果";i['created']="创建于";i['customStartDate']="自定义开始日期";i['customStartDateHelp']="在你的本地时区，这将覆盖 \\\"距比赛开始时间\\\" 选项";i['defender']="防守方";i['drawingWithinNbMoves']=p({"other":"前 %s 次移动玩家并不会获得任何积分。"});i['drawStreakStandard']=s("连和场数：当棋手在竞技场中连续和棋时，只有第一次和棋或者超过 %s 步的和局才会计分。连和场数只能被胜利打破，不会被输棋或和棋打破。");i['drawStreakVariants']="计分的和局的最小步数由不同的变体而变化。下面的表格列出了每个变体的阀值。";i['editTeamBattle']="编辑团队战斗";i['editTournament']="编辑锦标赛";i['history']="竞技场历史";i['howAreScoresCalculated']="分数是如何计算的？";i['howAreScoresCalculatedAnswer']="赢局的基础分数是2分，平局1分，输局不得分。如果你连胜两局将开始双倍积分，以火焰图标表示。接下来的对局将继续获得双倍积分，直到你输棋为止。也就是说，在双倍积分的情况下赢局值4分，平局2分。输局仍然不得分。 \n\n例如两场胜利紧接着一场平局得6分： 2 + 2 + (2 x 1)。";i['howDoesItEnd']="锦标赛如何结束？";i['howDoesItEndAnswer']="锦标赛有倒计时。当它到零的时候锦标赛名次不再变动，赢家就确定了。此时未下完的棋局必须下完但这些棋局不计入锦标赛分数。";i['howDoesPairingWork']="棋手是怎么配对的？";i['howDoesPairingWorkAnswer']="在锦标赛开始时，系统会根据棋手的等级分进行配对。一局结束后，回到锦标赛主页等待。此后你会和与你锦标赛名次接近的棋手配对，这尽可能降低了等待时间。然而，你不一定会和锦标赛所有其他棋手下棋。\n\n为了下更多的棋局以得到更多的锦标赛积分，尽量快地下完每一盘棋并回到主页。";i['howIsTheWinnerDecided']="赢家是如何决定的？";i['howIsTheWinnerDecidedAnswer']="在锦标赛设定时间限制结束时积分最多的玩家将被宣布为获胜者。\n\n当两个或两个以上的玩家拥有相同分数时，锦标赛的表现是決勝。";i['isItRated']="会影响等级分吗？";i['isNotRated']="本锦标赛为休闲赛，不会影响你的等级分。";i['isRated']="本锦标赛为排位赛，会影响你的等级分。";i['medians']="中位数";i['minimumGameLength']="最少棋局步数";i['myTournaments']="我的锦标赛";i['newTeamBattle']="新团队战";i['noArenaStreaks']="取消连胜积分奖励";i['noBerserkAllowed']="禁止“神速”模式";i['onlyTitled']="仅限有头衔玩家";i['onlyTitledHelp']="需要官方头衔才能加入比赛";i['otherRules']="其他重要规则";i['pickYourTeam']="请选择您的团队";i['pointsAvg']="平均分数";i['pointsSum']="总分数";i['rankAvg']="平均等级";i['rankAvgHelp']="等级平均值是您排名的百分比。较低的排名越好。\n\n例如，在100个玩家的锦标赛中排名第三名 = 3%。在1000个玩家的锦标赛中排名第10名 = 1%。";i['recentlyPlayed']="最近结束";i['shareUrl']=s("分享本链接让其他人加入本锦标赛：%s");i['someRated']="部分锦标赛是排位赛，会影响你的等级分。";i['stats']="统计";i['thereIsACountdown']="第一次移动有时间限制。规定时间内未能采取行动将会转由对手操作。";i['thisIsPrivate']="这是一个私人锦标赛";i['total']="总计";i['tournamentShields']="锦标赛盾牌";i['tournamentStats']="锦标赛统计";i['tournamentWinners']="锦标赛赢家";i['variant']="变体";i['viewAllXTeams']=p({"other":"查看所有%s团队"});i['whichTeamWillYouRepresentInThisBattle']="你将代表哪个团队出战？";i['willBeNotified']="当锦标赛开始时，你将收到通知，所以在其他标签页中下棋是无碍的。";i['youMustJoinOneOfTheseTeamsToParticipate']="您必须加入这些团队之一才能参与！"})()