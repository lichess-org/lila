"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.settings)window.i18n.settings={};let i=window.i18n.settings;i['cantOpenSimilarAccount']="شما نمی توانید حساب جدیدی با این نام کاربری باز کنید، حتی اگر با دستگاه دیگری وارد شوید.";i['changedMindDoNotCloseAccount']="نظرم عوض شد، حسابم را نبند";i['closeAccount']="بستن حساب";i['closeAccountExplanation']="آیا مطمئنید که می خواهید حساب خود را ببندید؟ بستن حساب یک تصمیم دائمی است. شما هرگز نمی توانید دوباره وارد حساب خود شوید.";i['closingIsDefinitive']="بعد از بستن حسابتان دیگر نمی توانید به آن دسترسی پیدا کنید. آیا مطمئن هستید؟";i['managedAccountCannotBeClosed']="حساب‌تان مدیریت می‌شود و نمی‌توان آن را بست.";i['settings']="تنظیمات";i['thisAccountIsClosed']="این حساب بسته شده است"})()