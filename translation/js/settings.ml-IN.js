"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.settings)window.i18n.settings={};let i=window.i18n.settings;i['changedMindDoNotCloseAccount']="മനംമാറ്റം, അക്കൗണ്ട് അവസാനിപ്പിക്കേണ്ടതില്ല";i['closeAccount']="അക്കൗണ്ട് അവസാനിപ്പിക്കാം";i['closeAccountExplanation']="താങ്ങൾക്കു് അക്കൌണ്ടു് അവസാനിപ്പക്കാൻ ഉറപ്പുണ്ടോ? ഇതൊരു സുസ്ഥിരമായ തീരമാനമാണു്. താങ്ങൾ ഒരിക്കലും ഈ ഉപയോക്തൃപേരായി പ്രവേശിക്കാൻ പറ്റത്തില്ല.";i['settings']="ക്രമീകരണങ്ങള്‍";i['thisAccountIsClosed']="ഈ അക്കൗണ്ട് അവസാനിപ്പിച്ചിരിക്കുന്നു."})()