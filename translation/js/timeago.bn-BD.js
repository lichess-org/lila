"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.timeago)window.i18n.timeago={};let i=window.i18n.timeago;i['completed']="সম্পন্ন হয়েছে";i['inNbDays']=p({"one":"%s দিনের মধ্যে","other":"%s দিনের মধ্যে"});i['inNbHours']=p({"one":"%s ঘন্টার মধ্যে","other":"%s ঘন্টার মধ্যে"});i['inNbMinutes']=p({"one":"%s মিনিটের মধ্যে","other":"%s মিনিটের মধ্যে"});i['inNbMonths']=p({"one":"%s মাসের মধ্যে","other":"%s মাসের মধ্যে"});i['inNbSeconds']=p({"one":"%s সেকেন্ডের মধ্যে","other":"%s সেকেন্ডের মধ্যে"});i['inNbWeeks']=p({"one":"%s সপ্তাহের মধ্যে","other":"%s সপ্তাহের মধ্যে"});i['inNbYears']=p({"one":"%s বছরের মধ্যে","other":"%s বছরের মধ্যে"});i['justNow']="এখনই";i['nbDaysAgo']=p({"one":"%s দিন আগে","other":"%s দিন আগে"});i['nbHoursAgo']=p({"one":"%s ঘণ্টা আগে","other":"%s ঘন্টা আগে"});i['nbHoursRemaining']=p({"one":"%s ঘন্টা বাকি","other":"%s ঘন্টা বাকি"});i['nbMinutesAgo']=p({"one":"%s মিনিট আগে","other":"%s মিনিট আগে"});i['nbMinutesRemaining']=p({"one":"%s মিনিট বাকি","other":"%s মিনিট বাকি"});i['nbMonthsAgo']=p({"one":"%s মাস আগে","other":"%s মাস আগে"});i['nbWeeksAgo']=p({"one":"%s সপ্তাহ আগে","other":"%s সপ্তাহ আগে"});i['nbYearsAgo']=p({"one":"%s বছর আগে","other":"%s বছর আগে"});i['rightNow']="এই মুহূর্তে"})()