"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.timeago)window.i18n.timeago={};let i=window.i18n.timeago;i['completed']="završeno";i['inNbDays']=p({"one":"za %s dan","few":"za %s dana","other":"za %s dana"});i['inNbHours']=p({"one":"za %s sat","few":"za %s sata","other":"za %s sati"});i['inNbMinutes']=p({"one":"za %s minutu","few":"za %s minute","other":"za %s minuta"});i['inNbMonths']=p({"one":"za %s mjesec","few":"za %s mjeseca","other":"za %s mjeseci"});i['inNbSeconds']=p({"one":"za %s sekunda","few":"za %s sekundi","other":"za %s sekunda"});i['inNbWeeks']=p({"one":"za %s tjedan","few":"za %s tjedna","other":"za %s tjedana"});i['inNbYears']=p({"one":"za %s godinu","few":"za %s godine","other":"za %s godina"});i['justNow']="upravo sada";i['nbDaysAgo']=p({"one":"prije %s dan","few":"prije %s dana","other":"prije %s dana"});i['nbHoursAgo']=p({"one":"prije %s sat","few":"prije %s sata","other":"prije %s sati"});i['nbMinutesAgo']=p({"one":"prije %s minutu","few":"prije %s minute","other":"prije %s minuta"});i['nbMonthsAgo']=p({"one":"prije %s mjesec","few":"prije %s mjeseca","other":"prije %s mjeseci"});i['nbWeeksAgo']=p({"one":"prije %s tjedan","few":"prije %s tjedna","other":"prije %s tjedana"});i['nbYearsAgo']=p({"one":"prije %s godinu","few":"prije %s godine","other":"prije %s godina"});i['rightNow']="upravo sada"})()