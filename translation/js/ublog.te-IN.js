"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.ublog)window.i18n.ublog={};let i=window.i18n.ublog;i['drafts']="డ్రాఫ్ట్ లు";i['editYourBlogPost']="మీ బ్లాగ్ పోస్ట్ సవరించు";i['imageAlt']="చిత్రం ప్రత్యామ్నాయ టెక్స్ట్";i['imageCredit']="చిత్ర క్రెడిట్";i['latestBlogPosts']="తాజా బ్లాగ్ పోస్ట్‌లు";i['moreBlogPostsBy']=s("%s ద్వారా మరిన్ని బ్లాగ్ పోస్ట్‌లు");i['nbViews']=p({"one":"ఒక దృశ్యం","other":"%s వీక్షణాలు"});i['newPost']="కొత్త పోస్ట్";i['noDrafts']="చూపించడానికి డ్రాఫ్ట్ లు లేవు.";i['noPostsInThisBlogYet']="ఈ బ్లాగ్‌లో ఇంకా పోస్ట్‌లు లేవు.";i['postBody']="పోస్ట్ బాడీ";i['postIntro']="ఇంట్రో పోస్ట్";i['postTitle']="పోస్ట్ శీర్షిక";i['published']="ప్రచురణ";i['publishedNbBlogPosts']=p({"one":"ఒక బ్లాగ్ పోస్ట్ ప్రచురించబడింది","other":"%s ప్రచురించబడిన బ్లాగ్ పోస్ట్ లు"});i['publishHelp']="ఒకవేళ చెక్ చేయబడినట్లయితే, పోస్ట్ మీ బ్లాగ్ లో జాబితా చేయబడుతుంది. కాకపోతే, మీ డ్రాఫ్ట్ పోస్ట్ లలో ఇది ప్రైవేట్ గా ఉంటుంది";i['publishOnYourBlog']="మీ బ్లాగులో ప్రచురించండి";i['saveDraft']="డ్రాఫ్ట్ సేవ్ చేయండి";i['thisIsADraft']="ఇది డ్రాఫ్ట్";i['thisPostIsPublished']="ఈ పోస్ట్ ప్రచురించబడింది";i['uploadAnImageForYourPost']="మీ పోస్ట్ కోసం చిత్రాన్ని అప్‌లోడ్ చేయండి";i['viewAllNbPosts']=p({"one":"ఒక పోస్ట్‌ని వీక్షించండి","other":"మొత్తం %s పోస్ట్‌లను చూడండి"});i['xBlog']=s("%s\\'s బ్లాగు");i['xPublishedY']=s("%1$s %2$s ప్రచురించారు")})()