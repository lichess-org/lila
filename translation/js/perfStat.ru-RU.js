"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.perfStat)window.i18n.perfStat={};let i=window.i18n.perfStat;i['averageOpponent']="Средний рейтинг соперников";i['berserkedGames']="Партий с берсерком";i['bestRated']="Победы против лучших по рейтингу";i['currentStreak']=s("Текущая серия: %s");i['defeats']="Поражений";i['disconnections']="Отключений";i['fromXToY']=s("от %1$s до %2$s");i['gamesInARow']="Сыгранные подряд игры";i['highestRating']=s("Наивысший рейтинг: %s");i['lessThanOneHour']="Перерыв между играми менее часа";i['longestStreak']=s("Рекордная серия: %s");i['losingStreak']="Поражений подряд";i['lowestRating']=s("Наименьший рейтинг: %s");i['maxTimePlaying']="Максимальное время за игрой";i['notEnoughGames']="Недостаточно сыгранных партий";i['notEnoughRatedGames']="Недостаточно рейтинговых игр, чтобы узнать точный рейтинг.";i['now']="сейчас";i['perfStats']=s("Статистика %s");i['progressOverLastXGames']=s("Прогресс за последние игры (%s):");i['provisional']="предварительный";i['ratedGames']="Рейтинговые игры";i['ratingDeviation']=s("Отклонение рейтинга: %s.");i['ratingDeviationTooltip']=s("Меньшее значение означает, что рейтинг более стабилен. Если этот показатель превышает %1$s, то рейтинг считается примерным. Для включения в рейтинг-листы этот показатель должен быть ниже %2$s (стандартные шахматы) и %3$s (варианты).");i['timeSpentPlaying']="Времени за игрой";i['totalGames']="Всего партий";i['tournamentGames']="Турнирных партий";i['victories']="Побед";i['viewTheGames']="Посмотреть партии";i['winningStreak']="Побед подряд"})()