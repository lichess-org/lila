"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.patron)window.i18n.patron={};let i=window.i18n.patron;i['actOfCreation']="Evet, kuruluş tüzüğünü okuyabilirsiniz (Fransızca)";i['amount']="Tutar";i['bankTransfers']="Banka havalesini de kabul ediyoruz";i['becomePatron']="Lichess Destekçisi olun";i['cancelSupport']="desteğini iptal et";i['celebratedPatrons']="Lichess\\'i mümkün kılan değerli destekçilerimiz";i['changeCurrency']="Para birimini değiştir";i['changeMonthlyAmount']=s("Aylık ücreti değiştir (%s)");i['changeMonthlySupport']="Aylık desteğimi değiştirebilir veya iptal edebilir miyim?";i['changeOrContact']=s("Evet, bu sayfadan istediğiniz işlemi yapabilirsiniz.\nVeya %s.");i['checkOutProfile']="Profil sayfanıza bir bakın derim!";i['contactSupport']="Lichess destek masasıyla iletişime geçin";i['costBreakdown']="Ayrıntılı maliyet dökümüne göz atın";i['currentStatus']="Mevcut durum";i['date']="Tarih";i['decideHowMuch']="Lichess\\'in sizin için ne kadar değerli olduğuna karar verin:";i['donate']="Bağış yap";i['donateAsX']=s("%s olarak bağış yap");i['downgradeNextMonth']="Bir ay içinde sizden tekrar ücret talep edilmeyecek ve Lichess hesabınız ücretsiz hâline dönecek.";i['featuresComparison']="Detaylı ayrıcalık karşılaştırmasına göz atın";i['freeAccount']="Ücretsiz Hesap";i['freeChess']="Herkes için ücretsiz satranç,\nsonsuza kadar!";i['giftPatronWings']="Başka bir oyuncu adına Lichess\\'i destekle";i['giftPatronWingsShort']="Destekçi kanatları hediye et";i['ifNotRenewedThenAccountWillRevert']="Bağışınızı yenilemediğiniz takdirde hesabınız normal hesaba çevrilecektir.";i['lichessIsRegisteredWith']=s("Lichess %s programına kayıtlıdır.");i['lichessPatron']="Lichess Destekçisi";i['lifetime']="Ömür Boyu";i['lifetimePatron']="Bir Ömür Lichess Destekçisi";i['logInToDonate']="Bağış yapmak için kayıt olun";i['makeAdditionalDonation']="Bir bağış daha yap";i['monthly']="Aylık";i['newPatrons']="Yeni Destekçiler";i['nextPayment']="Sonraki ödeme";i['noAdsNoSubs']="Ne reklam var ne abonelik; kodumuz açık kaynaklı, tutkumuz epik.";i['noLongerSupport']="Artık Lichess\\'i desteklemiyor";i['noPatronFeatures']="Hayır çünkü Lichess herkes için tamamen ve sonsuza kadar ücretsiz bir platform olarak kalacaktır. Buna söz veriyoruz.\n\nAncak Destekçilerimiz, müthiş profil simgeleriyle sonuna kadar övünme hakkına sahiptirler.";i['nowLifetime']="Bu desteğiniz bize bir ömür yeter!";i['nowOneMonth']="Bir aylığına Lichess Destekçisi oldunuz!";i['officialNonProfit']="Lichess resmî olarak kâr amacı gütmeyen bir kuruluş mu?";i['onetime']="Tek sefer";i['onlyDonationFromAbove']="Lütfen yalnızca yukarıdaki bağış formunun Kullanıcı statüsünü vereceğini unutmayın.";i['otherAmount']="Diğer";i['otherMethods']="Farklı bir bağış yapma yolu var mı?";i['patronFeatures']="Destekçilere özel ayrıcalıklar var mı?";i['patronForMonths']=p({"one":"Bir aydır Lichess Destekçisi","other":"%s aydır Lichess Destekçisi"});i['patronUntil']=s("Destekçi hesabınızın süresi %s tarihinde sona erecek.");i['payLifetimeOnce']=s("Tek seferde %s ödeyin ve sonsuza değin Lichess destekçisi olun!");i['paymentDetails']="Ödeme detayları";i['permanentPatron']="Artık daimî bir Destekçi hesabınız var.";i['pleaseEnterAmountInX']=s("Lütfen %s cinsinden bir tutar girin");i['recurringBilling']="Tekrarlayan ödeme, Destekçi kanatlarınız her ay yenilenecek.";i['serversAndDeveloper']=s("Asıl önceliğimiz güçlü sunuculara sahip olmak.\nBununla birlikte, Lichess\\'in kurucusu ve geliştiricisi olan %s\\'a tam zamanlı ödeme yapıyoruz.");i['singleDonation']="Tek seferlik bir ödeme yaptığınız takdirde kullanıcı adınızın yanında beliren destekçi kanatlarına bir aylığına sahip olursunuz.";i['stopPayments']="Kredi kartınızı geri çekin ve ödemeleri durdurun:";i['stopPaymentsPayPal']="PayPal aboneliğini iptal et ve ödemeleri durdur:";i['stripeManageSub']="Aboneliğinizi yönetin ve faturalarınızı ve makbuzlarınızı indirin";i['thankYou']="Yardımınız için teşekkür ederiz!";i['transactionCompleted']="İşleminiz tamamlandı ve yaptığınız bağışın makbuzu e-posta adresinize gönderildi.";i['tyvm']="Desteğin için çok teşekkür ederiz. Harikasın!";i['update']="Güncelle";i['updatePaymentMethod']="Ödeme yöntemini değiştir";i['viewOthers']="Diğer Lichess Destekçilerini gör";i['weAreNonProfit']="Herkesin ücretsiz ve kaliteli bir satranç platformuna kolayca ulaşabilmesi gerektiğine gönülden inandığımız için hiçbir kâr amacı gütmüyoruz.";i['weAreSmallTeam']="Biz küçük bir ekibiz, dolayısıyla desteğiniz büyük bir fark yaratacak!";i['weRelyOnSupport']="Lichess sizler gibi insanların desteği sayesinde ayakta duruyor. Bu çorbada sizin de tuzunuz olsun istiyorsanız lütfen bizden desteğinizi esirgemeyin.";i['whereMoneyGoes']="Bağışlar nereye gidiyor?";i['withCreditCard']="Kredi Kartı";i['xBecamePatron']=s("%s Lichess Destekçisi oldu");i['xIsPatronForNbMonths']=p({"one":"%1$s %2$s aydır Lichess\\'i destekliyor","other":"%1$s %2$s aylığına Lichess\\'i destekledi"});i['xOrY']=s("%1$s veya %2$s");i['youHaveLifetime']="Artık Ömür Boyu Destekçi hesabınız var. Bu müthiş bir şey!";i['youSupportWith']=s("Ayda %s ile lichess.org\\'u destekliyorsunuz.");i['youWillBeChargedXOnY']=s("%2$s tarihinde %1$s ödemeniz olacak.")})()