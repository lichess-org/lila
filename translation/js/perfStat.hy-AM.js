"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.perfStat)window.i18n.perfStat={};let i=window.i18n.perfStat;i['averageOpponent']="Մրցակիցների միջին վարկանիշը";i['berserkedGames']="Բերսերկով պարտիաներ";i['bestRated']="Հաղթանակներ ամենաբարձր վարկանիշ ունեցողների նկատմամբ";i['currentStreak']=s("Ընթացիկ շարքը` %s");i['defeats']="Պարտություններ";i['disconnections']="Անջատումներ";i['fromXToY']=s("%1$s-ից %2$s");i['gamesInARow']="Անընդմեջ խաղացված պարտիաներ";i['highestRating']=s("Բարձրագույն վարկանիշ` %s");i['lessThanOneHour']="Պարտիաների միջև դադարը` մեկ ժամից պակաս";i['longestStreak']=s("Ամենաերկար շարքը` %s");i['losingStreak']="Անընդմեջ պարտություններ";i['lowestRating']=s("Ցածրագույն վարկանիշ` %s");i['maxTimePlaying']="Առավելագույն խաղաժամանակ";i['notEnoughGames']="Խաղացված պարտիաների քանակն անբավարար է";i['notEnoughRatedGames']="Ճշգրիտ վարկանիշն իմանալու համար վարկանիշային պարտիաների քանակը բավարար չէ։";i['now']="հիմա";i['perfStats']=s("%s վիճակագրություն");i['progressOverLastXGames']=s("Աճը վերջին %s խաղերի ընթացքում.");i['provisional']="նախնական";i['ratedGames']="Վարկանիշային պարտիաներ";i['ratingDeviation']=s("Վարկանիշի շեղումը` %s։");i['ratingDeviationTooltip']=s("Ցածրագույն արժեքը նշանակում է, որ վարկանիշն ավելի կայուն է։ Եթե այդ ցուցանիշը գերազանցում է %1$s-ը, ապա վարկանիշը համարվում է մոտավոր։ Վարկանիշի ցանկերում ընդգրկվելու համար այդ ցուցանիշը պետք է փոքր լինի %2$s-ից (դասական շախմատ) կամ %3$s-ից (տարբերակներ)։");i['timeSpentPlaying']="Ընդհանուր խաղաժամանակ";i['totalGames']="Ընդամենը պարտիաներ";i['tournamentGames']="Մրցաշարային պարտիաներ";i['victories']="Հաղթանակներ";i['viewTheGames']="Դիտել պարտիաները";i['winningStreak']="Անընդմեջ հաղթանակներ"})()