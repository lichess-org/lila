"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.settings)window.i18n.settings={};let i=window.i18n.settings;i['cantOpenSimilarAccount']="No se te permitirá ubrir una nueva cuenta con o mesmo nombre, ni cambiando letras mayusclas y minusclas.";i['changedMindDoNotCloseAccount']="He cambiau d\\'opinión, no zarren la mía cuenta";i['closeAccount']="Zarrar cuenta";i['closeAccountExplanation']="Yes seguro que quiers zarrar la tuya cuenta? Zarrar la tuya cuenta ye una decisión permanent. Ya nunca mas podrás iniciar sesión dende la mesma.";i['closingIsDefinitive']="Lo zarre d\\'a cuenta será definitivo. No i hai tornada dezaga. Yes seguro?";i['managedAccountCannotBeClosed']="La tuya cuenta ye administrada, y no se puede zarrar.";i['settings']="Preferencias";i['thisAccountIsClosed']="Esta cuenta estió zarrada."})()