"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.broadcast)window.i18n.broadcast={};let i=window.i18n.broadcast;i['aboutBroadcasts']="关于转播";i['addRound']="添加一轮";i['ageThisYear']="今年的年龄";i['broadcastCalendar']="转播日程表";i['broadcasts']="转播";i['completed']="已完成";i['completedHelp']="Lichess基于源游戏检测游戏的完成状态。如果没有源，请使用此选项。";i['credits']="信任来源";i['currentGameUrl']="当前棋局链接";i['definitivelyDeleteRound']="确定删除该回合及其游戏。";i['definitivelyDeleteTournament']="确定删除整个锦标赛、所有轮次和其中所有比赛。";i['deleteAllGamesOfThisRound']="删除此回合的所有游戏。源需要激活才能重新创建。";i['deleteRound']="删除此轮";i['deleteTournament']="删除该锦标赛";i['downloadAllRounds']="下载所有棋局";i['editRoundStudy']="编辑该轮次的棋局研究";i['federation']="棋联";i['fideFederations']="FIDE 成员国";i['fidePlayerNotFound']="未找到 FIDE 棋手";i['fidePlayers']="FIDE 棋手";i['fideProfile']="FIDE个人资料";i['fullDescription']="赛事详情";i['fullDescriptionHelp']=s("转播内容的详细描述 (可选）。可以使用 %1$s，字数少于 %2$s 个。");i['howToUseLichessBroadcasts']="如何使用Lichess转播";i['liveBroadcasts']="赛事转播";i['myBroadcasts']="我的直播";i['nbBroadcasts']=p({"other":"%s 直播"});i['newBroadcast']="新建实况转播";i['ongoing']="进行中";i['periodInSeconds']="时长（秒）";i['periodInSecondsHelp']="可选项，请求之间等待多长时间。最小2秒，最大60秒。默认值为基于观众数量的自动值。";i['recentTournaments']="最近的比赛";i['replacePlayerTags']="可选项：替换选手的名字、等级分和头衔";i['resetRound']="重置此轮";i['roundName']="轮次名称";i['roundNumber']="轮数";i['showScores']="根据比赛结果显示棋手分数";i['sourceGameIds']="多达64个 Lichess 棋局Id，用空格隔开。";i['sourceSingleUrl']="PGN的URL源";i['sourceUrlHelp']="Lichess 将从该网址搜查 PGN 的更新。它必须是公开的。";i['startDateHelp']="如果你知道比赛开始时间 (可选)";i['subscribedBroadcasts']="已订阅的转播";i['theNewRoundHelp']="新一轮的成员和贡献者将与前一轮相同。";i['top10Rating']="前10名等级分";i['tournamentDescription']="锦标赛简短描述";i['tournamentName']="锦标赛名称";i['unrated']="未评级";i['upcoming']="即将举行"})()