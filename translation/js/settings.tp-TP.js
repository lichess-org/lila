"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.settings)window.i18n.settings={};let i=window.i18n.settings;i['cantOpenSimilarAccount']="tenpo kama la sina ken ala open e lipu sin kepeken nimi sama. sina ken ala pali e ni kepeken sitelen lili pi suli ante.";i['changedMindDoNotCloseAccount']="mi ante e lawa. o pini ala e lipu jan mi";i['closeAccount']="o pini e lipu jan";i['closeAccountExplanation']="sina wile ala wile pini e lipu sina? pali ni li awen. tenpo kama ALA la sina ken open e lipu sina.";i['closingIsDefinitive']="sina weka e lipu sina la, ona li weka lon tenpo ale. sina wile ala wile pali e ni?";i['settings']="ante";i['thisAccountIsClosed']="lipu jan ni li pini."})()