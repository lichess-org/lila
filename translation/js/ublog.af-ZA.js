"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.ublog)window.i18n.ublog={};let i=window.i18n.ublog;i['blogTips']="Ons eenvoudige wenke vir goeie plasings";i['blogTopics']="Blog-onderwerpe";i['communityBlogs']="Gemeenskapsblogs";i['createBlogDiscussion']="Aktiveer kommentaar";i['createBlogDiscussionHelp']="\\'n Forumgespreksonderwerp sal geskep word sodat ander kommentaar kan lewer op jou plasing";i['deleteBlog']="Vee hierdie plasing volledig uit";i['drafts']="Konsepte";i['editYourBlogPost']="Bewerk jou webjoernaal se inskrywing";i['friendBlogs']="Vriendblogs";i['imageAlt']="Beeld se alternatiewe beskrywing";i['imageCredit']="Beeld akkreditasie";i['inappropriateContentAccountClosed']="Enigiets onvanpas kan lei tot die sluiting van jou rekening.";i['latestBlogPosts']="Jongste webjoernaal inskrywings";i['lichessOfficialBlog']="Amptelike Lichess-Blog";i['moreBlogPostsBy']=s("Meer webjoernaal inskrywings van %s");i['nbViews']=p({"one":"Een kyk","other":"%s kyke"});i['newPost']="Nuwe inskrywing";i['noDrafts']="Geen konsepte om te wys nie.";i['noPostsInThisBlogYet']="Nog geen inskrywings in hierdie webjoernal nie.";i['postBody']="Inskrywing se lyf";i['postIntro']="Inskrywing se inleiding";i['postTitle']="Inskrywing se titel";i['previousBlogPosts']="Vorige blog-plasings";i['published']="Gepubliseerd";i['publishedNbBlogPosts']=p({"one":"\\'n Joernaal inskrywing was gepubliseerd","other":"%s joernaal inskrywings was gepubliseerd"});i['publishHelp']="As die gemerk is sal dit op jou webjoernaal verskyn. Indien nie so nie, sal dit privaat wees, in jou konsepte";i['publishOnYourBlog']="Publiseer op jou webjoernaal";i['safeAndRespectfulContent']="Plaas slegs veilige en bedagsame inhoud. Moenie iemand anders se inhoud kopieer nie.";i['safeToUseImages']="Dit is veilig om prente van die volgende webtuistes te gebruik:";i['saveDraft']="Stoor konsep";i['selectPostTopics']="Kies die onderwerpe van jou plasing";i['thisIsADraft']="Hierdie is \\'n konsep";i['thisPostIsPublished']="Hierdie inskrywing is gepubliseerd";i['uploadAnImageForYourPost']="Laai \\'n beeld vir jou inskywing op";i['useImagesYouMadeYourself']="Jy kan ook prente gebruik wat jy self geskep het, foto\\'s wat jy geneem het, skermskote van Lichess; enigiets waarop niemand anders kopiereg het nie.";i['viewAllNbPosts']=p({"one":"Beloer een inskrywing","other":"Beloer %s inskrywings"});i['xBlog']=s("%s se webjoernaal");i['xPublishedY']=s("%1$s publiseer %2$s");i['youBlockedByBlogAuthor']="Jy word deur die blog-outeur geblokkeer."})()