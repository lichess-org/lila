"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.team)window.i18n.team={};let i=window.i18n.team;i['closeTeam']="အသင်းကို ဖျက်ပါ";i['closeTeamDescription']="အသင်းကို ထာဝရ ဖျက်သည်။";i['declinedRequests']="ငြင်းပယ်ထားသော တောင်းဆိုမှုများ";i['entryCode']="အသင်းသို့ ဝင်ရန် စကားဝှက်။";i['incorrectEntryCode']="စကားဝှက် မမှန်ပါ။";i['joinLichessVariantTeam']=s("သတင်းနှင့် ပွဲများ အတွက် lichess ရဲ့ တရားဝင် %s အသင်းကို ဝင်ပါ");i['leadersChat']="ခေါင်းဆောင်တို့ စကားပြောရန်";i['messageAllMembersLongDescription']="အသင်းဝင် အားလုံးထံ လျှို့ဝှက် စာတို ပို့ရန်။\nပြိုင်ပွဲ သို့မဟုတ် အသင်းအချင်းချင်း ကစားပွဲ တစ်ခုတွင် ပါဝင်ကြရေး နိုးဆော်ရန် အသုံးပြုနိုင်ပါသည်။\nသင့်ထံမှ စာတို မရယူလိုသော အသင်းဝင်တို့ အသင်းမှ ထွက်ကောင်း ထွက်သွားနိုင်ပါသည်။";i['onlyLeaderLeavesTeam']="သင် မထွက်မီ အသင်းခေါင်းဆောင် တစ်ဦး ထည့်ပေးပါ၊ သို့မဟုတ်လျှင် အသင်းကို ဖျက်သိမ်းလိုက်ပါ။";i['requestDeclined']="အသင်း ဝင်ရန် လူကြီးမင်း၏ တောင်းဆိုမှုအား အသင်း ခေါင်းဆောင်က ငြင်းပယ်ထားပါသည်။";i['teamLeaders']=p({"other":"အသင်းခေါင်းဆောင်"});i['teamsIlead']="မိမိ ဦးဆောင်သော အသင်းများ";i['youWayWantToLinkOneOfTheseTournaments']="ကျင်းပရန် စီစဉ်ထားသော ဤပြိုင်ပွဲတို့သို့ ချိတ်ဆက်လိုပါသလား။"})()