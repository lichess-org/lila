"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.timeago)window.i18n.timeago={};let i=window.i18n.timeago;i['completed']="amaituta";i['inNbDays']=p({"one":"egun %sen","other":"%s egunetan"});i['inNbHours']=p({"one":"ordu %sen","other":"%s ordutan"});i['inNbMinutes']=p({"one":"minutu %sen","other":"%s minututan"});i['inNbMonths']=p({"one":"hilabete %sen","other":"%s hilabetetan"});i['inNbSeconds']=p({"one":"segundo %sen","other":"%s segundotan"});i['inNbWeeks']=p({"one":"aste %sen","other":"%s egunetan"});i['inNbYears']=p({"one":"urte %sen","other":"%s urtetan"});i['justNow']="orain";i['nbDaysAgo']=p({"one":"orain dela egun %s","other":"orain dela %s egun"});i['nbHoursAgo']=p({"one":"orain dela ordu %s","other":"orain dela %s ordu"});i['nbHoursRemaining']=p({"one":"Ordu %s falta da","other":"%s ordu falta dira"});i['nbMinutesAgo']=p({"one":"orain dela minutu %s","other":"orain dela %s minutu"});i['nbMinutesRemaining']=p({"one":"Minutu %s falta da","other":"%s minutu falta dira"});i['nbMonthsAgo']=p({"one":"orain dela hilabete %s","other":"orain dela %s hilabete"});i['nbWeeksAgo']=p({"one":"orain dela aste %s","other":"orain dela %s aste"});i['nbYearsAgo']=p({"one":"orain dela urte %s","other":"orain dela %s urte"});i['rightNow']="orain"})()