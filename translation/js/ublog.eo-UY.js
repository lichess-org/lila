"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.ublog)window.i18n.ublog={};let i=window.i18n.ublog;i['blogTips']="Niaj facilaj konsiloj por verki bonegajn blogafiŝojn";i['blogTopics']="Blogaj temoj";i['communityBlogs']="Komunumaj blogoj";i['continueReadingPost']="Daŭri legi ĉi tiun afiŝon";i['createBlogDiscussion']="Ebligi komentojn";i['createBlogDiscussionHelp']="Foruma temo estos kreita por homoj komenti vian afiŝon";i['deleteBlog']="Forigi ĉi tiun afiŝon definitive";i['discussThisBlogPostInTheForum']="Diskuti ĉi tiun blogafiŝojn en la forumo";i['drafts']="Malnetoj";i['editYourBlogPost']="Redakti vian blogafiŝon";i['friendBlogs']="Blogoj de amikoj";i['imageAlt']="Alternativa teksto de bildo";i['imageCredit']="Agnosko de bildo";i['inappropriateContentAccountClosed']="Io malkonvena povus fermigi vian konton.";i['latestBlogPosts']="Plej novaj blogafiŝoj";i['lichessBlogPostsFromXYear']=s("Lichess blogafiŝojn en %s");i['lichessOfficialBlog']="Lichess Oficiala Blogo";i['likedBlogs']="Ŝatitaj blogafiŝoj";i['moreBlogPostsBy']=s("Plu blogafiŝoj de %s");i['nbViews']=p({"one":"Unu vido","other":"%s vidoj"});i['newPost']="Nova afiŝo";i['noDrafts']="Neniuj malnetoj por montri.";i['noPostsInThisBlogYet']="Ankoraŭ, neniuj afiŝoj en ĉi tiu blogo.";i['postBody']="Afiŝa enhavo";i['postIntro']="Afiŝa enkonduko";i['postTitle']="Afiŝa titolo";i['previousBlogPosts']="Antaŭaj blogafiŝoj";i['published']="Eldonita";i['publishedNbBlogPosts']=p({"one":"Eldonigis blogafiŝon","other":"Eldonigis %s blogafiŝojn"});i['publishHelp']="Se markita, la afiŝo estos listigita en via blogo. Se ne, ĝi estos privata, en via malnetaj afiŝoj";i['publishOnYourBlog']="Eldoni en via blogo";i['safeAndRespectfulContent']="Bonvolu nur afiŝi sekuran kaj respektan enhavon. Ne kopiu enhavon de iu alia.";i['safeToUseImages']="Estas sekure uzi bildojn de la sekvaj retejoj:";i['saveDraft']="Konservi malneton";i['selectPostTopics']="Elektu temojn, ke via afiŝo temas";i['thisIsADraft']="Tiu estas malneto";i['thisPostIsPublished']="Tiu afiŝo estas eldonita";i['uploadAnImageForYourPost']="Alŝuti bildon por via afiŝo";i['useImagesYouMadeYourself']="Vi ankaŭ povas uzi bildojn, kiujn vi mem faris, bildojn vi fotis, ekrankopioj de Lichess... ion ajn, kio ne estas kopirajtita de alia iu.";i['viewAllNbPosts']=p({"one":"Vidi unu afiŝon","other":"Vidi ĉiujn %s afiŝojn"});i['xBlog']=s("Blogo de %s");i['xPublishedY']=s("%1$s eldonis %2$s")})()