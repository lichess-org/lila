"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.timeago)window.i18n.timeago={};let i=window.i18n.timeago;i['completed']="afsluttet";i['inNbDays']=p({"one":"om %s dag","other":"om %s dage"});i['inNbHours']=p({"one":"om %s time","other":"om %s timer"});i['inNbMinutes']=p({"one":"om %s minut","other":"om %s minutter"});i['inNbMonths']=p({"one":"om %s måned","other":"om %s måneder"});i['inNbSeconds']=p({"one":"om %s sekund","other":"om %s sekunder"});i['inNbWeeks']=p({"one":"om %s uge","other":"om %s uger"});i['inNbYears']=p({"one":"om %s år","other":"om %s år"});i['justNow']="for lidt siden";i['nbDaysAgo']=p({"one":"%s dag siden","other":"%s dage siden"});i['nbHoursAgo']=p({"one":"%s time siden","other":"%s timer siden"});i['nbHoursRemaining']=p({"one":"%s time tilbage","other":"%s timer tilbage"});i['nbMinutesAgo']=p({"one":"%s minut siden","other":"%s minutter siden"});i['nbMinutesRemaining']=p({"one":"%s minut tilbage","other":"%s minutter tilbage"});i['nbMonthsAgo']=p({"one":"%s måned siden","other":"%s måneder siden"});i['nbWeeksAgo']=p({"one":"%s uge siden","other":"%s uger siden"});i['nbYearsAgo']=p({"one":"%s år siden","other":"%s år siden"});i['rightNow']="netop nu"})()