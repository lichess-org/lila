"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.timeago)window.i18n.timeago={};let i=window.i18n.timeago;i['completed']="slutfört";i['inNbDays']=p({"one":"om %s dag","other":"om %s dagar"});i['inNbHours']=p({"one":"om %s timme","other":"om %s timmar"});i['inNbMinutes']=p({"one":"om %s minut","other":"om %s minuter"});i['inNbMonths']=p({"one":"om %s månad","other":"om %s månader"});i['inNbSeconds']=p({"one":"om %s sekund","other":"om %s sekunder"});i['inNbWeeks']=p({"one":"om %s vecka","other":"om %s veckor"});i['inNbYears']=p({"one":"om %s år","other":"om %s år"});i['justNow']="just nu";i['nbDaysAgo']=p({"one":"%s dag sedan","other":"%s dagar sedan"});i['nbHoursAgo']=p({"one":"%s timme sedan","other":"%s timmar sedan"});i['nbHoursRemaining']=p({"one":"%s timme återstår","other":"%s timmar återstår"});i['nbMinutesAgo']=p({"one":"%s minut sedan","other":"%s minuter sedan"});i['nbMinutesRemaining']=p({"one":"%s minut återstår","other":"%s minuter återstår"});i['nbMonthsAgo']=p({"one":"%s månad sedan","other":"%s månader sedan"});i['nbWeeksAgo']=p({"one":"%s vecka sedan","other":"%s veckor sedan"});i['nbYearsAgo']=p({"one":"%s år sedan","other":"%s år sedan"});i['rightNow']="just nu"})()