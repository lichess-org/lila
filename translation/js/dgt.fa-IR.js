"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.dgt)window.i18n.dgt={};let i=window.i18n.dgt;i['clickToGenerateOne']="برای تولید یکی، اینجا کلیک کنید";i['configurationSection']="بخش پیکربندی";i['configure']="پیکربندی";i['debug']="اشکال‌یابی";i['dgtBoard']="تخته DGT";i['dgtBoardLimitations']="ملزومات تخته DGT";i['dgtBoardRequirements']="پیش‌نیازهای تخته DGT";i['dgtConfigure']="DGT - پیکربندی";i['dgtPlayMenuEntryAdded']=s("دَرآیه %s به نام‌چین پخش‌تان در بالا افزوده شد.");i['downloadHere']=s("شما می‌توانید نرم‌افزار را از اینجا بارگیری کنید: %s");i['ifLiveChessRunningElsewhere']=s("اگر %1$s روی دستگاه یا پورت دیگری در حال اجرا است، باید آدرس IP و پورت را در اینجا در %2$s تنظیم کنید.");i['ifLiveChessRunningOnThisComputer']=s("اگر %1$s در این رایانه در حال اجرا است، می‌توانید با %2$s اتصال خود را با آن بررسی کنید.");i['keywords']="کلیدواژه‌ها";i['lichessAndDgt']="لیچس و DGT";i['lichessConnectivity']="اتصال به لیچس";i['openingThisLink']="باز کردن این پیوند";i['playWithDgtBoard']="بازی با صفحه و مهره ی دیجیتالی";i['reloadThisPage']="بارگذاری مجدد صفحه";i['textToSpeech']="متن به گفتار";i['thisPageAllowsConnectingDgtBoard']="این صفحه به شما اجازه می‌دهد تخته DGT خود را به لیچس متصل کنید و از آن برای انجام بازی‌ها استفاده کنید.";i['toConnectTheDgtBoard']=s("برای متصل شدن به تخته الکترونیکی DGT لازم است %s را نصب کنید.");i['useWebSocketUrl']=s("از «%1$s» استفاده کنید، مگر اینکه %2$s روی دستگاه یا درگاه دیگری اجرا می‌شود.");i['verboseLogging']="گزارش‌دهی مفصل"})()