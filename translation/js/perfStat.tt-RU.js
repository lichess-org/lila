"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.perfStat)window.i18n.perfStat={};let i=window.i18n.perfStat;i['averageOpponent']="Уртача ярышучы";i['berserkedGames']="Яман уеннар";i['bestRated']="Иң яхшы рейтинглы җиңүләр";i['currentStreak']=s("Хәзерге бер-артлы: %s");i['defeats']="Җиңелүләр";i['disconnections']="Өзелгән";i['fromXToY']=s("%1$s дан %2$s га");i['gamesInARow']="Уеннар рәттән уйналды";i['highestRating']=s("Иң югары рейтинг %s");i['lessThanOneHour']="Уеннар арасында бер сәгатьтән дә азрак";i['longestStreak']=s("Иң озын бер-артлы: %s");i['losingStreak']="Бер-артлы җиңүлеләр";i['lowestRating']=s("Иң кече рейтинг %s");i['maxTimePlaying']="Бер-артлы уйнаган максималь вакыт";i['notEnoughGames']="Җитми уйналган уеннар";i['notEnoughRatedGames']="Ышанычлы рейтинг булдыру өчен җитәрлек бәяләнгән уеннар уйнамады.";i['now']="хәзер";i['perfStats']=s("%s статистикасы");i['progressOverLastXGames']=s("Соңгы %s уеннарында алга китеш:");i['provisional']="вакытлыча";i['ratedGames']="Рейтинглы уеннар";i['ratingDeviation']=s("Рейтингның тайпылышы:%s.");i['timeSpentPlaying']="Уенда уздырылган вакыт";i['totalGames']="Бөтен уеннар";i['tournamentGames']="Бәйге уеннары";i['victories']="Җиңүләр";i['viewTheGames']="Карарга уеннарны";i['winningStreak']="Бер-артлы җиңүләр"})()