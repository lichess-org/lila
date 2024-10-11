"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.patron)window.i18n.patron={};let i=window.i18n.patron;i['actOfCreation']="بله، مصوبه تاسیس (به زبان فرانسوی) این‌جا است";i['amount']="مقدار";i['bankTransfers']="ما انتقالهای بانکی را هم قبول می‌کنیم";i['becomePatron']="یاور Lichess بشوید";i['cancelSupport']="پشتیبانی خود را متوقف کنید";i['celebratedPatrons']="پشتبان‌های ارجمند که Lichess را ممکن می‌سازند";i['changeCurrency']="تغییر واحد پول";i['changeMonthlyAmount']=s("مقدار ماهانه (%s) را تغییر دهید");i['changeMonthlySupport']="آیا می‌توانم پشتیبانی ماهانه خود را تغییر یا حذف نمایم؟";i['changeOrContact']=s("بله، هر لحظه، از این صفحه. \nیا شما می‌توانید %s.");i['checkOutProfile']="به صفحه رُخ‌نمای‌تان سَر بزنید!";i['contactSupport']="با پشتیبانیِ Lichess تماس بگیرید";i['costBreakdown']="ساختار ریز هزینه را ببینید";i['currentStatus']="وضعیت کنونی";i['date']="تاریخ";i['decideHowMuch']="تصمیم بگیرید که Lichess چه میزان برای شما ارزش دارد:";i['donate']="کمک مالی";i['donateAsX']=s("به عنوان %s اهدا کنید");i['downgradeNextMonth']="طی یک ماه، دوباره برای شما بدهکاری ثبت نخواهد شد، و حساب کاربری Lichess شما به یک حساب کاربری معمولی برگردانده خواهد شد.";i['featuresComparison']="همسنجی ریز آرنگ‌ها را ببینید";i['freeAccount']="حساب کاربری رایگان";i['freeChess']="شطرنج رایگان برای همه، برای همیشه!";i['giftPatronWings']="به یک بازیکن بالهای پشتیبان هدیه بدهید";i['giftPatronWingsShort']="بال‌های پشتیبان هدیه دهید";i['ifNotRenewedThenAccountWillRevert']="اگر تمدید نشود، حساب شما به حالت عادی باز خواهد گشت.";i['lichessIsRegisteredWith']=s("لیچس در %s ثبت شده است.");i['lichessPatron']="یاورِ Lichess";i['lifetime']="مادام‌العمر";i['lifetimePatron']="یاور همیشگی Lichess";i['logInToDonate']="برای اهدای کمک به حساب کاربری خود وارد شوید";i['makeAdditionalDonation']="یک کمک مالی اضافه انجام دهید";i['monthly']="ماهانه";i['newPatrons']="یاوران نو";i['nextPayment']="پرداخت بعدی";i['noAdsNoSubs']="بدون تبلیغات، بدون اشتراکها، فقط متن‌باز و لذت‌بخش.";i['noLongerSupport']="بیش از این Lichess را پشتیبانی نکنید";i['noPatronFeatures']="نه، زیرا Lichess برای همیشه و برای همه، کاملا رایگان است. قول می‌دهیم.\nبا این حال، حامیان با بال‌های معرکه‌ای که در رُخ‌نمای‌شان نشان داده می‌شود، حق پُز دادن دارند.";i['nowLifetime']="شما هم‌اکنون یک پشتیبانِ مادام‌العمرِ Lichess هستید!";i['nowOneMonth']="شما هم‌اکنون برای یک ماه یک پشتیبانِ Lichess هستید!";i['officialNonProfit']="آیا Lichess به صورت رسمی غیرانتفاعی است؟";i['onetime']="یک بار";i['onlyDonationFromAbove']="لطفاً توجه داشته باشید که فقط برگه کمک مالی بالا، جایگاه حامی را به ارمغان می‌آورد.";i['otherAmount']="دیگر";i['otherMethods']="روشهای دیگر برای کمک مالی؟";i['patronFeatures']="آیا برخی ویژگیها فقط برای پشتیبان‌ها قابل دسترس است؟";i['patronForMonths']=p({"one":"پشتیبانِ Lichess برای یک ماه","other":"پشتیبانِ Lichess برای %s ماه"});i['patronUntil']=s("شما تا %s حساب یاور دارید.");i['payLifetimeOnce']=s("یکبار %s بپردازید. برای همیشه پشتیبانِ Lichess باشید!");i['paymentDetails']="جزئیات پرداخت";i['permanentPatron']="شما هم‌اکنون یک حساب دائمی پشتیبان دارید.";i['pleaseEnterAmountInX']=s("لطفا مقدار را در %s وارد کنید");i['recurringBilling']="تکرارِ صورت‌حساب، تجدیدِ بالهای پشتیبانِ شما در هر ماه.";i['serversAndDeveloper']=s("اول از همه، کارسازهای قدرتمند.\nسپس به یک توسعه‌دهنده تمام‌وقت پرداخت می‌کنیم: %s، بنیان‌گذارِ Lichess.");i['singleDonation']="یک کمک مالی که به شما بالهای پشتیبان را برای یک ماه اعطا می‌کند.";i['stopPayments']="کارت اعتباری خود را خارج و پرداختها را متوقف نمایید:";i['stopPaymentsPayPal']="لغو اشتراک PayPal و توقف پرداخت:";i['stripeManageSub']="اشتراک خود را مدیریت کنید و صورت‌حساب‌ها و رسیدهای خود را بارگیری کنید";i['thankYou']="از کمک مالی شما سپاسگزاریم!";i['transactionCompleted']="تراکنشِ شما تکمیل شد، و یک رسید برای کمک مالی شما برایتان ایمیل شد.";i['tyvm']="از کمک شما بسیار سپاسگزاریم. شما تاثیرگذارید!";i['update']="به روز رسانی";i['updatePaymentMethod']="به‌روزرسانی روش پرداخت";i['viewOthers']="پشتیبانهای دیگرِ Lichess را ببینید";i['weAreNonProfit']="ما یک انجمن غیرانتفاعی هستیم چون معتقدیم هر کسی باید به یک برنامه رایگان و در سطح جهانیِ شطرنج دسترسی داشته باشد.";i['weAreSmallTeam']="ما یک تیم کوچک هستیم، بنابراین پشتیبانی شما تغییر بزرگی ایجاد می‌کند!";i['weRelyOnSupport']="ما بر روی کمک از سوی کسانی چون شما برای تحقق آن تکیه می‌کنیم. اگر از استفاده از Lichess لذت می‌برید، لطفاً با کمک مالی و پشتیبان شدن، ما را یاری برسانید!";i['whereMoneyGoes']="پول به کجا می‌رود؟";i['withCreditCard']="کارت اعتباری";i['xBecamePatron']=s("%s یاور Lichess شد");i['xIsPatronForNbMonths']=p({"one":"%1$s یک پشتیبان Lichess به مدت %2$s ماه می‌باشد","other":"%1$s یک پشتیبان Lichess به مدت %2$s ماه می‌باشد"});i['xOrY']=s("%1$s یا %2$s");i['youHaveLifetime']="شما دارای یک حساب پشنیبان مادام‌العمر هستید. واقعاً خیلی عالی است!";i['youSupportWith']=s("شما با %s در هر ماه Lichess را پشتیبانی می‌کنید.");i['youWillBeChargedXOnY']=s("برای شما در تاریخ %2$s به مقدار %1$s بدهکاری ثبت خواهد شد.")})()