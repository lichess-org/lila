"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.onboarding)window.i18n.onboarding={};let i=window.i18n.onboarding;i['configureLichess']="لیچس را طبق سلیقه خود تنظیم کنید.";i['enabledKidModeSuggestion']=s("آیا یک کودک قرار است از این حساب استفاده کند؟ شاید بخواهید %s را فعال کنید.");i['exploreTheSiteAndHaveFun']="سایت رو بگردید و لذت ببرید :)";i['followYourFriendsOnLichess']="دوستان‌تان را در لیچس بدنبالید.";i['improveWithChessTacticsPuzzles']="با معماهای تاکتیکی شطرنج، بازیتان را بهتر کنید.";i['learnChessRules']="قوانین شطرنج را یاد بگیرید";i['learnFromXAndY']=s("از %1$s و %2$s یادبگیرید.");i['playInTournaments']="در مسابقات شرکت کنید.";i['playOpponentsFromAroundTheWorld']="با حریف‌هایی از سراسر دنیا بازی کنید.";i['playTheArtificialIntelligence']="با هوش مصنوعی بازی کنید.";i['thisIsYourProfilePage']="این صفحه رُخ‌نمای‌تان است.";i['welcome']="خوش آمدید!";i['welcomeToLichess']="به لیچس خوش آمدید!";i['whatNowSuggestions']="حالا چی؟ این‌ها پیشنهاد ماست:"})()