"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.ublog)window.i18n.ublog={};let i=window.i18n.ublog;i['blogTips']="Blogda yaxshi postlar yozish uchun bizning oddiy maslahatlarimiz";i['createBlogDiscussion']="Izohlashga ruhsat berish";i['createBlogDiscussionHelp']="Forum mavzusi unda odamlar izoh qoldirish uchun yaratiladi";i['deleteBlog']="Ushbu blog postini butunlay o\\'chirib tashlash";i['drafts']="Qarolamalar";i['editYourBlogPost']="Blog postingizni tahrirlang";i['imageAlt']="Rasm o‘rniga matn";i['imageCredit']="Rasm uchun imkon";i['inappropriateContentAccountClosed']="Mumkin bo‘lmagan ma‘lumotlar joylanishi sizning hisobingizni yopilishiga sabab bo‘lishi mumkin.";i['latestBlogPosts']="Eng soʻnggi nlog postlar";i['moreBlogPostsBy']=s("%s\\'dan koʻproq blog postlar");i['nbViews']=p({"one":"Bir marta koʻrilgan","other":"%s marta ko\\'rilgan"});i['newPost']="Yangi post";i['noDrafts']="Ko‘rsatish uchun qoralamalar yo‘q.";i['noPostsInThisBlogYet']="Ushbu blogda hozircha postlar yoʻq.";i['postBody']="Post asosiy qismi";i['postIntro']="Post kirish qismi";i['postTitle']="Post sarlavhasi";i['published']="Chop etildi";i['publishedNbBlogPosts']=p({"one":"Blog xabari nashr etildi","other":"%s blog xabarlari nashr etildi"});i['publishHelp']="Agar belgilangan bo‘lsa, xabar sizni blogingizda namoyish etiladi. Agar yo‘q bo‘lsa, u yopiq bo‘lib, sizning qoralama xabarlaringiz ichida bo‘ladi";i['publishOnYourBlog']="O‘z blogingizda nashr eting";i['safeAndRespectfulContent']="Iltimos faqat xavfsiz va \\\"hurmat\\\"li ma‘qolalarni nashr eting. Boshqalarning ma‘lumotlarini nusxalamang.";i['safeToUseImages']="Quyidagi veb-saytlardagi rasmlardan foydalanish xavfsiz:";i['saveDraft']="Qoralamani saqlash";i['selectPostTopics']="Blog postingizda muhokama qiladigan mavzularni tanlang";i['thisIsADraft']="Bu qoralama";i['thisPostIsPublished']="Ushbu post chop etilgan";i['uploadAnImageForYourPost']="Postingiz uchun rasm yuklang";i['useImagesYouMadeYourself']="Siz shuningdek o‘zgalarning mualliflik huquqi bilan ximoyalanmagan barcha narsalardan tashqari yana o‘zingizning shaxsiy rasmlaringizni, sur‘atlaringizni va Lichess skrinshotlarini ishlatishingiz mumkin.";i['viewAllNbPosts']=p({"one":"Bitta postni ko‘rish","other":"Barcha %s ta postni koʻrish"});i['xBlog']=s("%s ning Blogi");i['xPublishedY']=s("%1$s %2$s\\'ni chop etdi")})()