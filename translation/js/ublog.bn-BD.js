"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.ublog)window.i18n.ublog={};let i=window.i18n.ublog;i['createBlogDiscussion']="মন্তব্য সক্ষম করুন";i['createBlogDiscussionHelp']="লোকেরা আপনার পোস্টে মন্তব্য করার জন্য একটি ফোরাম তৈরি করা হবে";i['editYourBlogPost']="আপনার ব্লগপোস্টটি সম্পাদনা করুন";i['newPost']="নতুন পোস্ট";i['postTitle']="শিরোনাম লিপিবদ্ধ করুন";i['published']="প্রকাশিত";i['publishHelp']="যদি পোস্টটি চেক করা হয় তবেই পোস্টটি আপনার ব্লগে তালিকাভুক্ত করা হবে। যদি তা না হয় তবে আপনার পোস্টটি ব্যক্তিগত খসড়াতে রূপান্তরিত হবে";i['publishOnYourBlog']="ব্লগে প্রকাশিত করুন";i['saveDraft']="খসড়া সংরক্ষণ করুন";i['thisIsADraft']="এটি একটি খসড়া";i['thisPostIsPublished']="এই পোস্টটি প্রকাশিত হয়েছে"})()