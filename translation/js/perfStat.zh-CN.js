"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.perfStat)window.i18n.perfStat={};let i=window.i18n.perfStat;i['averageOpponent']="对手平均等级分";i['berserkedGames']="神速局数";i['bestRated']="最佳胜利";i['currentStreak']=s("当前纪录: %s");i['defeats']="负";i['disconnections']="断线";i['fromXToY']=s("自 %1$s 至 %2$s");i['gamesInARow']="连续对局";i['highestRating']=s("最高等级分: %s");i['lessThanOneHour']="棋局间隔一个小时以内";i['longestStreak']=s("最长纪录: %s");i['losingStreak']="连败";i['lowestRating']=s("最低等级分: %s");i['maxTimePlaying']="最长连续对局时间";i['notEnoughGames']="棋局不足";i['notEnoughRatedGames']="因排位赛数量不足而无法测定可靠的等级分";i['now']="现在";i['perfStats']=s("%s 战绩");i['progressOverLastXGames']=s("最近 %s 局后的变化:");i['provisional']="暂定";i['ratedGames']="排位赛局数";i['ratingDeviation']=s("等级分偏差: %s");i['ratingDeviationTooltip']=s("偏差值越低，等级分越稳定。若偏差值大于 %1$s，则等级分是暂时的。只有当偏差低于 %2$s（标准局）/ %3$s（变种）时，才能进入排名。");i['timeSpentPlaying']="弈棋总时间";i['totalGames']="棋局总数";i['tournamentGames']="锦标赛局数";i['victories']="胜";i['viewTheGames']="查看棋局";i['winningStreak']="连胜"})()