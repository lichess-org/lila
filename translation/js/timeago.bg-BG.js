"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.timeago)window.i18n.timeago={};let i=window.i18n.timeago;i['completed']="завършено";i['inNbDays']=p({"one":"след %s ден","other":"след %s дни"});i['inNbHours']=p({"one":"след %s час","other":"след %s часа"});i['inNbMinutes']=p({"one":"след %s минута","other":"след %s минути"});i['inNbMonths']=p({"one":"след %s месец","other":"след %s месеца"});i['inNbSeconds']=p({"one":"след %s секунда","other":"след %s секунди"});i['inNbWeeks']=p({"one":"след %s седмица","other":"след %s седмици"});i['inNbYears']=p({"one":"след %s година","other":"след %s години"});i['justNow']="току що";i['nbDaysAgo']=p({"one":"преди %s ден","other":"Преди %s дни"});i['nbHoursAgo']=p({"one":"преди %s час","other":"Преди %s часа"});i['nbHoursRemaining']=p({"one":"остава %s час","other":"остават %s часа"});i['nbMinutesAgo']=p({"one":"преди %s минута","other":"преди %s минути"});i['nbMinutesRemaining']=p({"one":"остава %s минутa","other":"остават %s минути"});i['nbMonthsAgo']=p({"one":"преди %s месец","other":"преди %s месеца"});i['nbWeeksAgo']=p({"one":"преди %s седмица","other":"преди %s седмици"});i['nbYearsAgo']=p({"one":"преди %s година","other":"преди %s години"});i['rightNow']="точно сега"})()