"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.search)window.i18n.search={};let i=window.i18n.search;i['advancedSearch']="進階搜尋";i['aiLevel']="A.I等級";i['analysis']="分析";i['ascending']="升序";i['color']="顏色";i['date']="日期";i['descending']="降序";i['evaluation']="評估";i['from']="從";i['gamesFound']=p({"other":"找到 %s 個對局"});i['humanOrComputer']="不管您的對手是電腦或是其他玩家";i['include']="包含";i['loser']="輸家";i['maxNumber']="最大值";i['maxNumberExplanation']="回傳的最大場數";i['nbTurns']="回合數";i['onlyAnalysed']="僅可進行有電腦分析的遊戲";i['opponentName']="對手名稱";i['ratingExplanation']="兩位玩家的平均評分";i['result']="結果";i['search']="搜尋";i['searchInXGames']=p({"other":"在%s場棋局中尋找"});i['sortBy']="排序條件";i['source']="來源";i['to']="到";i['winnerColor']="獲勝方棋子顏色";i['xGamesFound']=p({"other":"找到了%s盤棋局"})})()