"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.emails)window.i18n.emails={};let i=window.i18n.emails;i['common_contact']=s("आमच्याशी संपर्क करण्यासाठी कृपया %s चा वापर करा.");i['common_note']=s("हा एक %s शी संबंधित सर्विस ईमेल आहे.");i['common_orPaste']="(क्लिक काम करत नाही? आपल्या ब्राऊजर मध्ये पेस्ट करून बघा!)";i['emailChange_click']="आपल्याला या ईमेल ला अॅक्सेस आहे ह्याची खात्री करण्यासाठी खालील लिंक वर क्लिक करा:";i['emailChange_intro']="आपण आपला ईमेल अॅड्रेस बदलण्यासाठी विनंती केली आहे.";i['emailChange_subject']=s("आपला नवीन ईमेल अॅड्रेस ची खात्री करून घ्या, %s");i['emailConfirm_click']="आपले lichess खाते कार्यान्वीत करण्यासाठी लिंकवर क्लिक करा:";i['emailConfirm_ignore']="जर आपण lichess येते नोंदणी केली नसेल तर आपण या संदेशाकडे दुर्लक्ष करू शकता.";i['emailConfirm_subject']=s("आपल्या lichess.org खात्याची खात्री करा, %s");i['logInToLichess']=s("lichess. org वरती लॉग इन करा, %s");i['passwordReset_clickOrIgnore']="जर आपण ही विनंती केली असेल तर खालील लिंक वर क्लिक करा. जर आपण असे केले नसेल तर या मेल कडे दुर्लक्ष करा.";i['passwordReset_intro']="आपल्या खात्यासाठी पासवर्ड रिसेट ची विनंती आम्हाला मिळाली आहे.";i['passwordReset_subject']=s("आपला lichess. org चा पासवर्ड रिसेट करा, %s");i['welcome_subject']=s("Lichess.org वर स्वागत, %s");i['welcome_text']=s("आपण https://lichess.org इथे आपले खाते यशस्वीरीत्या तयार केले आहे. \n\n%1$s हे आपले प्रोफाइल पेज आहे. आपण ते %2$s इथे पर्सनलाईज करू शकता.\n\nमजा करा, आणि आम्ही अशी आशा करतो की आपल्या सोंगट्या नेहमी समोरच्या राजापर्यंत मार्ग हुडकतील!")})()