"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.coordinates)window.i18n.coordinates={};let i=window.i18n.coordinates;i['aCoordinateAppears']="ကစားခုံပေါ်တွင် အညွှန်းသင်္ကေတ တစ်ခု ပေါ်လာမည် ဖြစ်ပြီး သင်က သက်ဆိုင်ရာ ကစားခုံကွက်ကို ကလစ် လုပ်ရမည်။";i['aSquareIsHighlightedExplanation']="ကစားခုံပေါ်တွင် အကွက် တစ်ကွက်ကို မီးမောင်းထိုးပြမည် ဖြစ်ပြီး သင်က သက်ဆိုင်ရာ အညွှန်းသင်္ကေတကို ရိုက်ထည့်ရမည်။";i['findSquare']="ပေးထားသည့် ကစားခုံကွက်ကို ရှာပါ";i['goAsLongAsYouWant']="သွားချင်သလောက် ကြာကြာ သွားပါ၊ အချိန် ကန့်သတ် မထားပါ။";i['nameSquare']="ပေးထားသည့် ကစားခုံကွက်၏ အမည်ကို ဖော်ပြပါ";i['showCoordinates']="ကစားခုံကွက် အညွှန်းများ ပြပါ";i['showPieces']="ကစားရုပ်များ ပြပါ";i['youHaveThirtySeconds']="ကစားခုံကွက် တတ်နိုင်သမျှ အများဆုံး ရှာဖို့ အတွက် စက္ကန့် ၃၀ ရပါမည်။"})()