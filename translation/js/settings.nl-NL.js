"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.settings)window.i18n.settings={};let i=window.i18n.settings;i['cantOpenSimilarAccount']="Het is niet toegestaan om een nieuw account met dezelfde naam aan te maken, ook al is het hoofdlettergebruik anders.";i['changedMindDoNotCloseAccount']="Ik ben van gedachten veranderd; sluit mijn account niet";i['closeAccount']="Verwijder account";i['closeAccountExplanation']="Weet je zeker dat je het account wilt verwijderen? Het verwijderen van je account is een permanente beslissing. Je kunt NOOIT meer op dit account inloggen.";i['closingIsDefinitive']="Verwijderen is definitief. Er is geen weg terug. Weet je het zeker?";i['managedAccountCannotBeClosed']="Je account wordt beheerd, en kan niet verwijderd worden.";i['settings']="Instellingen";i['thisAccountIsClosed']="Dit account is gesloten."})()