"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.ublog)window.i18n.ublog={};let i=window.i18n.ublog;i['blogTips']="Eis einfach Tuyaue fir flott Blog-Bäiträg ze schreiwen";i['blogTopics']="Blog-Theemen";i['communityBlogs']="Blogge vun der Communautéit";i['continueReadingPost']="Virufuere mam Liese vun dësem Bäitrag";i['createBlogDiscussion']="Kommentaren erlaben";i['createBlogDiscussionHelp']="Et gëtt ee Forentheema ugeluecht, fir datt d\\'Leit däi Bäitrag kommentéiere kënnen";i['deleteBlog']="Dëse Blog-Bäitrag endgülteg läschen";i['discussThisBlogPostInTheForum']="Iwwert dëse Blog-Bäitrag am Forum diskutéieren";i['drafts']="Entwërf";i['editYourBlogPost']="Beaarbecht däi Blog-Bäitrag";i['friendBlogs']="Blogge vun de Kolleegen";i['imageAlt']="Alternativen Bildtext";i['imageCredit']="Bildquell";i['inappropriateContentAccountClosed']="Alles, wat net ubruecht ass, kann dozou féieren, datt däi Kont zougemaach gëtt.";i['latestBlogPosts']="Neiste Blog-Bäiträg";i['lichessBlogPostsFromXYear']=s("Lichess-Blog-Bäiträg vun %s");i['lichessOfficialBlog']="Offizielle Lichess-Blog";i['likedBlogs']="Geliket Blog-Bäiträg";i['moreBlogPostsBy']=s("Méi Blog-Bäiträg vum %s");i['nbViews']=p({"one":"Emol ugesinn","other":"%s mol ugesinn"});i['newPost']="Neie Bäitrag";i['noDrafts']="Keng Entwërf unzeweisen.";i['noPostsInThisBlogYet']="Nach keng Bäiträg an dësen Blog.";i['postBody']="Bäitragsinhalt";i['postIntro']="Bäitragsaleedung";i['postTitle']="Bäitragstitel";i['previousBlogPosts']="Virege Blog-Bäitrag";i['published']="Verëffentlecht";i['publishedNbBlogPosts']=p({"one":"Huet e Blog-Bäitrag verëffentlecht","other":"Huet %s Blog-Bäiträg verëffentlecht"});i['publishHelp']="Wann aktivéiert, wäert de Bäitrag an engem Blog opgelëscht ginn. Wann net, wäert e privat sinn, an dengen Entwërf";i['publishOnYourBlog']="Verëffentlechen an dengem Blog";i['safeAndRespectfulContent']="Wgl. post just sécher a respektvoll Inhalter. Kopéier net d\\'Inhalter vun enger anerer Persoun.";i['safeToUseImages']="Et kann een ouni Problemer Biller vun de follgende Websäiten ze benotzen:";i['saveDraft']="Entworf späicheren";i['selectPostTopics']="Wiel d\\'Theeme vun dengem Bäitrag";i['thisIsADraft']="Dëst ass en Entworf";i['thisPostIsPublished']="Dësen Bäitrag ass verëffentlecht";i['uploadAnImageForYourPost']="E Bild fir däin Bäitrag eroplueden";i['useImagesYouMadeYourself']="Du kanns och Biller benotzen, déi s du selwer gemaach hues, Fotoen, déi s du gemaach hues, Screenshotte vu Liches... Alles, wourop keen aneren Urheberrechter huet.";i['viewAllNbPosts']=p({"one":"E Bäitrag uweisen","other":"%s Bäiträg uweisen"});i['xBlog']=s("%s säi Blog");i['xPublishedY']=s("%1$s huet %2$s verëffentlecht");i['youBlockedByBlogAuthor']="Du goufs vum Auteur vum Blog blockéiert."})()