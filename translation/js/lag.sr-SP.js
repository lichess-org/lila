"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.lag)window.i18n.lag={};let i=window.i18n.lag;i['andNowTheLongAnswerLagComposedOfTwoValues']="А сада, дугачак одговор! Лаг у игри се састоји од две неповезане вредности (мање је боље):";i['isLichessLagging']="Да ли Личес лагује?";i['lagCompensation']="Компензација за лаг";i['lichessServerLatency']="Одзив Lichess сервера";i['lichessServerLatencyExplanation']="Време које је потребно за процесуирање потеза на серверу. Једнако је за све и зависи само од оптережености сервера. Што је више играча, то је веће, али Lichess програмери га покушавају смањити што више. Ретко кад прелази 10ms.";i['measurementInProgressThreeDot']="Мерења у току...";i['networkBetweenLichessAndYou']="Мрежа између Lichess и тебе";i['noAndYourNetworkIsBad']="Не. И твоја мрежа је лоша.";i['noAndYourNetworkIsGood']="Не. И твоја мрежа је добра.";i['yesItWillBeFixedSoon']="Да. Ово ће бити поправљено ускоро!";i['youCanFindTheseValuesAtAnyTimeByClickingOnYourUsername']="Можете наћи обе вредности у сваком тренутку, стиснући на ваше корисничко име на врху."})()