"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.perfStat)window.i18n.perfStat={};let i=window.i18n.perfStat;i['averageOpponent']="Ortalama rəqib";i['berserkedGames']="Berserk oyunları";i['bestRated']="Ən yüksək reytinqli qələbələr";i['currentStreak']=s("Hazırkı seriya: %s");i['defeats']="Məğlubiyyətlər";i['disconnections']="Bağlantı kəsilmələri";i['fromXToY']=s("%1$s tarixindən %2$s tarixinə qədər");i['gamesInARow']="Ardıcıl oynanılan oyunlar";i['highestRating']=s("Ən yüksək reytinq: %s");i['lessThanOneHour']="Oyunlar arasında bir saatdan az ara olmalıdır";i['longestStreak']=s("Ən uzun seriya: %s");i['losingStreak']="Məğlubiyyət seriyası";i['lowestRating']=s("Ən aşağı reytinq: %s");i['maxTimePlaying']="Oyun üçün xərclənən ən uzun vaxt";i['notEnoughGames']="Yetərli sayda oyun oynanılmayıb";i['notEnoughRatedGames']="Etibarlı reytinq yaratmaq üçün kifayət qədər reytinqli oyun oynanılmayıb.";i['now']="indi";i['perfStats']=s("%s statistikası");i['progressOverLastXGames']=s("Son %s oyundakı irəliləyiş:");i['provisional']="müvəqqəti";i['ratedGames']="Reytinqli oyunlar";i['ratingDeviation']=s("Xal yayınma dəyəri: %s.");i['ratingDeviationTooltip']=s("Aşağı dəyər reytinqin daha sabit olduğunu bildirir. %1$s-dan yuxarı olan reytinq müvəqqəti hesab olunur. Reytinqlərə daxil olmaq üçün bu dəyər %2$s-dan (standart şahmat) və ya %3$s-dan (variantlardan) aşağı olmalıdır.");i['timeSpentPlaying']="Cəmi oynama müddəti";i['totalGames']="Cəmi oyunlar";i['tournamentGames']="Turnir oyunları";i['victories']="Qələbələr";i['viewTheGames']="Oyunlara bax";i['winningStreak']="Qalibiyyət seriyası"})()