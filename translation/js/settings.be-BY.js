"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.settings)window.i18n.settings={};let i=window.i18n.settings;i['cantOpenSimilarAccount']="Вам не будзе дазволена стварыць новы ўліковы запіс з тым жа імем, нават калі рэгістр сімвалаў адрозніваецца.";i['changedMindDoNotCloseAccount']="Я перадумаў, не выдаляйце мой уліковы запіс";i['closeAccount']="Выдаліць уліковы запіс";i['closeAccountExplanation']="Вы сапраўды хочаце выдаліць свой уліковы запіс? Гэта неадваротнае дзеянне: увайсці ў яго будзе немагчыма.";i['closingIsDefinitive']="Зачыненне немагчыма будзе адмяніць. Не будзе шляху назад. Вы ўпэўнены?";i['settings']="Налады";i['thisAccountIsClosed']="Гэты ўліковы запіс зачынены."})()