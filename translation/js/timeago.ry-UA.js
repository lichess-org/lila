"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.timeago)window.i18n.timeago={};let i=window.i18n.timeago;i['inNbDays']=p({"one":"через %s динь","few":"через %s дні","other":"через %s днюв"});i['inNbHours']=p({"one":"через %s годину","few":"через %s годины","other":"через %s годин"});i['inNbMinutes']=p({"one":"через %s минуту","few":"через %s минуты","other":"через %s минут"});i['inNbMonths']=p({"one":"через %s мїсяць","few":"через %s мїсяця","other":"через %s мїсяцюв"});i['inNbSeconds']=p({"one":"через %s секунду","few":"через %s секунды","other":"через %s секунд"});i['inNbWeeks']=p({"one":"через %s тыждень","few":"через %s тыждні","other":"через %s тыжднюв"});i['inNbYears']=p({"one":"через %s гуд","few":"через %s гуда","other":"через %s годув"});i['justNow']="лемшто";i['nbDaysAgo']=p({"one":"%s динь тепирь","few":"%s дні тепирь","other":"%s днюв тепирь"});i['nbHoursAgo']=p({"one":"%s годину тепирь","few":"%s годины тепирь","other":"%s годин тепирь"});i['nbMinutesAgo']=p({"one":"%s минута тепирь","few":"%s минуты тепирь","other":"%s минут тепирь"});i['nbMonthsAgo']=p({"one":"%s мїсяць тепирь","few":"%s мїсяця тепирь","other":"%s мїсяцюв тепирь"});i['nbWeeksAgo']=p({"one":"%s тыждень тепирь","few":"%s тыждня тепирь","other":"%s тыжднюв тепирь"});i['nbYearsAgo']=p({"one":"%s гуд тепирь","few":"%s гуды тепирь","other":"%s гуд тепирь"});i['rightNow']="такой тепирь"})()