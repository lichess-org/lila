"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.ublog)window.i18n.ublog={};let i=window.i18n.ublog;i['blogTips']="sona pona mi tawa pali lipu";i['createBlogDiscussion']="o ken e toki pi jan ante";i['createBlogDiscussionHelp']="jan o ken toki lon lipu sina. tawa ni la toki pi ma toki li kama lon";i['deleteBlog']="o weka e lipu ni lon tenpo ale";i['discussThisBlogPostInTheForum']="o toki lon lipu ni lon ma toki";i['drafts']="lipu pi pana ala";i['editYourBlogPost']="o ante e lipu sina";i['imageAlt']="ijo pi sitelen ni";i['imageCredit']="tan sitelen";i['inappropriateContentAccountClosed']="sina sitelen anu pana e ijo jaki la sina ken kama ken ala kepeken ilo Lichess";i['latestBlogPosts']="lipu sin";i['moreBlogPostsBy']=s("lipu ante pi jan %s");i['nbViews']=p({"one":"mute lukin li wan","other":"lukin %s"});i['newPost']="lipu sin";i['noDrafts']="lipu pi pana ala li lon ala";i['noPostsInThisBlogYet']="lipu li lon ala";i['postBody']="sijelo lipu";i['postIntro']="open lipu";i['postTitle']="nimi lipu";i['published']="mi pana a";i['publishedNbBlogPosts']=p({"one":"sina pana e lipu","other":"sina pana e lipu %s"});i['publishHelp']="ni li kule la jan ale li ken lukin e lipu. ona li kule ala la jan ala li ken lukin";i['publishOnYourBlog']="o pana tawa jan ale";i['safeAndRespectfulContent']="o sitelen ala e ijo jaki e ijo ike. o lanpan ala e lipu pi jan ante";i['safeToUseImages']="sina ken kepeken sitelen ni:";i['saveDraft']="o awen e lipu";i['selectPostTopics']="lipu sina li toki lon seme? o pana e nimi";i['thisIsADraft']="sina pana ala e lipu ni";i['thisPostIsPublished']="sina pana e lipu ni";i['uploadAnImageForYourPost']="o pana e sitelen tawa lipu sina";i['useImagesYouMadeYourself']="sina ken kepeken e sitelen pi pali sina e sitelen pi ilo Lichess. sina ken ala kepeken sitelen pi jo sina ala";i['viewAllNbPosts']=p({"one":"o lukin e lipu wan","other":"o lukin e lipu ale pi jan %s"});i['xBlog']=s("lipu pi jan %s");i['xPublishedY']=s("jan %1$s li pana e lipu %2$s")})()