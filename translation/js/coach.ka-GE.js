"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.coach)window.i18n.coach={};let i=window.i18n.coach;i['aboutMe']="ჩემს შესახებ";i['accepting']="სუდენტების მიღება";i['areYouCoach']=s("ხარ შენ ლიჩესის დიდი მჭვრთნელი %s?");i['availability']="შესაძლებობა";i['bestSkills']="საუკეთესო უნარები";i['confirmTitle']="დაადასტურე შენი წოდება აქ და ჩვენ განვაახლებტ თქვენ აპლიკაციას.";i['hourlyRate']="საათობრივი განაკვეთი";i['languages']="ენები";i['lichessCoach']="ლიჩესის მწვრთნელი";i['lichessCoaches']="ლიჩესის მწვრთნელები";i['location']="მდებარეობა";i['nmOrFideTitle']="ეროვნული ოსტატი თუ ფიდეს წოდება";i['notAccepting']="ამ მომენტისთვის სტუდენთთა მიღება არ ხდება";i['otherExperiences']="სხვა გამოცდილებები";i['playingExperience']="სატამაშო გამოცდილება";i['publicStudies']="საჯარო გაკვეთილები";i['rating']="რეიტინგი";i['sendApplication']=s("Გამოგვიგზავნეთ ელფოსტა %s-ზე და ჩვენ განვიხილავთ თქვენს განაცხადს.");i['sendPM']="გააგზავნი პირადი შეტყობინება";i['teachingExperience']="სწავლების გამოცდილება";i['teachingMethod']="სწავლების მეთოდიკა";i['viewXProfile']=s("მიჩვენე %s ლიჩესის პროფილი");i['xCoachesStudents']=s("ჭდრაკის სტუდენთთა %s წვრთნა");i['youtubeVideos']="იუთუბის ვიდეოები"})()