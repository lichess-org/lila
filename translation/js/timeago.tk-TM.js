"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.timeago)window.i18n.timeago={};let i=window.i18n.timeago;i['inNbDays']=p({"one":"%s günden","other":"%s günden"});i['inNbHours']=p({"one":"%s sagatdan","other":"%s sagatdan"});i['inNbMinutes']=p({"one":"%s minutdan","other":"%s minutdan"});i['inNbMonths']=p({"one":"%s aýdan","other":"%s aýdan"});i['inNbSeconds']=p({"one":"%s sekuntdan","other":"%s sekuntdan"});i['inNbWeeks']=p({"one":"%s hepdeden","other":"%s hepdeden"});i['inNbYears']=p({"one":"%s ýyldan","other":"%s ýyldan"});i['justNow']="edil şu wagt";i['nbDaysAgo']=p({"one":"%s gün öň","other":"%s gün öň"});i['nbHoursAgo']=p({"one":"%s sagat öň","other":"%s sagat öň"});i['nbMinutesAgo']=p({"one":"%s minut öň","other":"%s minut öň"});i['nbMonthsAgo']=p({"one":"%s aý öň","other":"%s aý öň"});i['nbWeeksAgo']=p({"one":"%s hepde öň","other":"%s aý öň"});i['nbYearsAgo']=p({"one":"%s ýyl öň","other":"%s ýyl öň"});i['rightNow']="şu wagt"})()