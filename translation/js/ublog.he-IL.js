"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.ublog)window.i18n.ublog={};let i=window.i18n.ublog;i['blogTips']="הטיפים שלנו לכתיבת פוסטים מעולים";i['blogTopics']="נושאי הבלוגים";i['communityBlogs']="בלוגים מהקהילה";i['continueReadingPost']="המשך קריאה של הבלוג";i['createBlogDiscussion']="אפשר תגובות";i['createBlogDiscussionHelp']="יווצר נושא בפורום כדי שאנשים יוכלו להגיב על הפוסט שלך";i['deleteBlog']="מחק/י את הפוסט הזה לצמיתות";i['discussThisBlogPostInTheForum']="שיחה על הבלוג הזה בפורומים";i['drafts']="טיוטות";i['editYourBlogPost']="עריכת הפוסט";i['friendBlogs']="בלוגים של חברים";i['imageAlt']="תיאור התמונה";i['imageCredit']="קרדיטים ליוצרי התמונה";i['inappropriateContentAccountClosed']="כל דבר לא הולם עלול להוביל לסגירת חשבונך.";i['latestBlogPosts']="פוסטים אחרונים";i['lichessBlogPostsFromXYear']=s("בלוגים של Lichess ב־%s");i['lichessOfficialBlog']="הבלוג הרשמי של Lichess";i['likedBlogs']="בלוגים שאהבת";i['moreBlogPostsBy']=s("עוד פוסטים מאת %s");i['nbViews']=p({"one":"צפייה אחת","two":"%s צפיות","many":"%s צפיות","other":"%s צפיות"});i['newPost']="פוסט חדש";i['noDrafts']="אין טיוטות להצגה.";i['noPostsInThisBlogYet']="אין פוסטים בבלוג זה, לעת עתה...";i['postBody']="גוף הפוסט";i['postIntro']="מבוא לפוסט";i['postTitle']="כותרת הפוסט";i['previousBlogPosts']="בלוגים קודמים";i['published']="פורסם";i['publishedNbBlogPosts']=p({"one":"פרסם/ה פוסט","two":"פרסם/ה %s פוסטים","many":"פרסם/ה %s פוסטים","other":"פרסם/ה %s פוסטים"});i['publishHelp']="אם הפוסט נבדק, הוא יפורסם לבלוג שלך. אם לא, הוא יישאר בטיוטות שלך";i['publishOnYourBlog']="פרסם בבלוג שלך";i['safeAndRespectfulContent']="נא לפרסם רק תוכן מכבד ובטוח. אין להעתיק תוכן של מישהו אחר.";i['safeToUseImages']="מומלץ להשתמש בתמונות מהאתרים הבאים:";i['saveDraft']="שמירה כטיוטה";i['selectPostTopics']="בחר/י את הנושאים שהפוסט שלך עוסק בהם";i['thisIsADraft']="זוהי טיוטה";i['thisPostIsPublished']="הפוסט פורסם";i['uploadAnImageForYourPost']="העלו תמונה לפוסט שלכם";i['useImagesYouMadeYourself']="תוכל להשתמש בתמונות שצילמת או יצרת, צילומי מסך מתוך Lichess... כל מה שלא מוגן בזכויות יוצרים ושייך למישהו אחר.";i['viewAllNbPosts']=p({"one":"צפו בפוסט אחד","two":"צפו בכל הפוסטים על %s","many":"צפו בכל הפוסטים על %s","other":"צפו בכל הפוסטים על %s"});i['xBlog']=s("הבלוג של %s");i['xPublishedY']=s("%1$s פרסם/ה את הפוסט: \\\"%2$s\\\"");i['youBlockedByBlogAuthor']="נחסמת על־ידי כותב הבלוג."})()