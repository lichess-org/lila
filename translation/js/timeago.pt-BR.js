"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.timeago)window.i18n.timeago={};let i=window.i18n.timeago;i['completed']="concluído";i['inNbDays']=p({"one":"em %s dia","other":"em %s dias"});i['inNbHours']=p({"one":"em %s hora","other":"em %s horas"});i['inNbMinutes']=p({"one":"em %s minuto","other":"em %s minutos"});i['inNbMonths']=p({"one":"em %s mês","other":"em %s meses"});i['inNbSeconds']=p({"one":"em %s segundo","other":"em %s segundos"});i['inNbWeeks']=p({"one":"em %s semana","other":"em %s semanas"});i['inNbYears']=p({"one":"em %s ano","other":"em %s anos"});i['justNow']="agora há pouco";i['nbDaysAgo']=p({"one":"%s dia atrás","other":"%s dias atrás"});i['nbHoursAgo']=p({"one":"%s hora atrás","other":"%s horas atrás"});i['nbHoursRemaining']=p({"one":"%s hora restante","other":"%s horas restantes"});i['nbMinutesAgo']=p({"one":"%s minuto atrás","other":"%s minutos atrás"});i['nbMinutesRemaining']=p({"one":"%s minuto restante","other":"%s minutos restantes"});i['nbMonthsAgo']=p({"one":"%s mês atrás","other":"%s meses atrás"});i['nbWeeksAgo']=p({"one":"%s semana atrás","other":"%s semanas atrás"});i['nbYearsAgo']=p({"one":"%s ano atrás","other":"%s anos atrás"});i['rightNow']="agora mesmo"})()