"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.perfStat)window.i18n.perfStat={};let i=window.i18n.perfStat;i['averageOpponent']="Орташа қарсылас";i['berserkedGames']="Берсерк ойындары";i['bestRated']="Ең бағалы жеңістер";i['currentStreak']=s("Қазіргі тізбек: %s");i['defeats']="Жеңілістер";i['disconnections']="Байланыс үзілуі";i['fromXToY']=s("%1$s - %2$s аралығында");i['gamesInARow']="Қатарынан ойналған ойындар";i['highestRating']=s("Ең үлкен рейтинг: %s");i['lessThanOneHour']="Ойындар арасы бір сағаттан кем";i['longestStreak']=s("Ең ұзақ тізбек: %s");i['losingStreak']="Жеңілістер тізбегі";i['lowestRating']=s("Ең төменгі рейтинг: %s");i['maxTimePlaying']="Ойынмен өткен ең ұзақ уақыт";i['notEnoughGames']="Ойындар саны жеткіліксіз";i['notEnoughRatedGames']="Тұрақты рейтингке ие болу үшін бағалы ойын саны жеткіліксіз.";i['now']="қазір";i['perfStats']=s("%s статистикасы");i['progressOverLastXGames']=s("Кейінгі %s ойынның қорытындысы:");i['provisional']="болжамалы";i['ratedGames']="Бағалы ойындар";i['ratingDeviation']=s("Рейтинг ауытқуы: %s.");i['ratingDeviationTooltip']=s("Мөлшері аз болса, рейтингтің тұрақты екенін білдіреді. %1$s-дан артық болса – болжамалы. Орынға ие болу үшін бұл шама классикалық шахматта %2$s-дан, ал басқа шахмат түрлерінде %3$s-дан кем болуы керек.");i['timeSpentPlaying']="Ойынмен өткен уақыт";i['totalGames']="Барлық ойындар";i['tournamentGames']="Жарыс ойындары";i['victories']="Жеңістер";i['viewTheGames']="Ойындарын көру";i['winningStreak']="Жеңістер тізбегі"})()