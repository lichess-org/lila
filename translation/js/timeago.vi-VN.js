"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.timeago)window.i18n.timeago={};let i=window.i18n.timeago;i['completed']="đã hoàn thành";i['inNbDays']=p({"other":"trong %s ngày"});i['inNbHours']=p({"other":"trong %s giờ"});i['inNbMinutes']=p({"other":"trong %s phút"});i['inNbMonths']=p({"other":"trong %s tháng"});i['inNbSeconds']=p({"other":"trong %s giây"});i['inNbWeeks']=p({"other":"trong %s tuần"});i['inNbYears']=p({"other":"trong %s năm"});i['justNow']="vừa mới đây";i['nbDaysAgo']=p({"other":"%s ngày trước"});i['nbHoursAgo']=p({"other":"%s giờ trước"});i['nbHoursRemaining']=p({"other":"còn %s giờ"});i['nbMinutesAgo']=p({"other":"%s phút trước"});i['nbMinutesRemaining']=p({"other":"còn %s phút"});i['nbMonthsAgo']=p({"other":"%s tháng trước"});i['nbWeeksAgo']=p({"other":"%s tuần trước"});i['nbYearsAgo']=p({"other":"%s năm trước"});i['rightNow']="ngay bây giờ"})()