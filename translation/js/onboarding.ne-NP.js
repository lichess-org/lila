"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.onboarding)window.i18n.onboarding={};let i=window.i18n.onboarding;i['configureLichess']="आफ्नो चहाना अनुसार lichess कन्फिगर गर्नुहोस्";i['enabledKidModeSuggestion']=s("के यो खाता बच्चाले चलाउँछ? तपाईँका लागी %s  उपयुक्त हुन सक्छ।");i['exploreTheSiteAndHaveFun']="साईट घुम्नुहोस् र मोज गर्नुहोस् ।";i['followYourFriendsOnLichess']="आफ्ना साथीहरूलाई फलो गर्नुहोस् !";i['improveWithChessTacticsPuzzles']="चेसका कार्यानैतिक-पजलहरू द्वारा खेल सुधार्नुहोस्";i['learnChessRules']="चेसका नियम हरू सिक्नुहोस्";i['learnFromXAndY']=s("%1$s र %2$s बाट सिक्नुहोस्");i['playInTournaments']="प्रतियोगिताहरूमा खेल्नुहोस्।";i['playOpponentsFromAroundTheWorld']="संसारभरका विपक्षी विरूद्ध.";i['playTheArtificialIntelligence']="AI सङ्ग खेल्नुहोस्";i['thisIsYourProfilePage']="यो तपाईँको प्रोफाईल पेज हो।";i['welcome']="स्वागतम् !";i['welcomeToLichess']="lichess.orgमा तपाईँलाई स्वागत छ।";i['whatNowSuggestions']="अब के? यहाँ केही सुझावहरू छन्!"})()