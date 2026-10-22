# Deaths Are Items

Minecraft **1.20.1 / Forge 47 / Java 17**. On dört farklı ölüm, animasyonlu sandık seçimi ve her ölümün temasını moblara uygulayan özel ödüller.

## Ölüm ve ödül sistemi

| Ölüm / komut anahtarı | Seçilebilir ödül | Diğer iki sandık |
| --- | --- | --- |
| Düşüş / `fall` | Fall Damage Boots | Long Trip, Slippery Banana Peel |
| Suda boğulma / `drown` | Drowning Mask | Empty Scuba Tank, Tiny Gills |
| Duvar içinde boğulma / `suffocate` | Suffocation Boulder | Bigger Lung Capacity, Cave Diver Toolkit |
| İskelet oku / `skeleton` | SkeletonBowSkeletonBowSkeletonBow | BowBowBow, ArrowArrowArrow |
| Demir golem / `golem` | Mob Pickaxe | Mob on a Stick, Iron Fist |
| Açlık / `starve` | Hunger Staff | Hunger Employee, Hunger Manager |
| Düşen örs / `anvil` | Swarm of Anvils | Rusty Sharpened Anvil, Heavier Anvil |
| Mob sıkışması / `cram` | Entity Crammer | Crammer Error, Squish Squash |
| Kaktüs / `cactus` | Cactus Chestplate | Thorn in your side, Rusty Cactus |
| TNT patlaması / `tnt` | TNT Cannon | Tasty TNT, Blast from the Past |
| Lav / `lava` | Fire Pants | Firewall, Molten Socks |
| Yatak / respawn anchor patlaması / `bed` | Bedrock | Fluffy Pillow, Explosive Blankie |
| Ender pearl ışınlanma hasarı / `pearl` | Ender Pearl Shield | Ender Pearl Necklace, Ender Eyeballs |
| Wither efekti / `wither` | Soul Storm | Withering Staff, Bone Hand |

Her tür oyuncu başına bir kez keşfedilir. Tekrar eden veya tanınmayan ölümlerde sandık, konfeti ve ses oynatılmaz; yeniden doğunca beş saniye kırmızı **find a different death...** görünür. Keşif geçmişi, bekleyen ödüller ve seçilmiş eşyalar dünya kaydında saklanır. İlk giriş ve End çıkış portalı ölüm sayılmaz.

İlk keşiften sonra yeniden doğuşta ses, konfeti ve ödül bildirimi gelir. Sandıklar yükselirken sarı/yeşil dust çıkar ve yükseliş sonunda kesilir; yere yerleşince renkli campfire dumanı başlar. Sandık açılma sesi kapağın açılmaya başladığı anda çalar. **3,5 saniye sonra** oyuncunun o andaki konumundan üç blok uzakta üç sandık yükselir. Üç seçenekten yalnızca tablodaki ödül seçilebilir. Önizleme sandıkları eşya vermez. Seçimde sarı/yeşil kamp ateşi dumanı ve 40 tick totem gösterimi oynar, ardından eşya teslim edilir. Bildirim simgesi ilgili ödüldür.

Sandıklar gerçek blokların üstüne yazılmaz. Dar alanda açık yere geçilmesi istenir ve tekrar denenir. Sahiplik, uzaklık, animasyon süresi ve çift tıklama doğrulanır. Dolu envanterde ödül sahibine ayrılmış eşya olarak bırakılır.

## Ödüller

- **Fall Damage Boots:** Ayak yuvasına giyilir, düşüş hasarını engeller. Normal inişte boş hücrelere 3×3 su → slime → saman minderleri koyar. Sıra botun NBT verisinde korunur. Eğilerek inişte zeminden gerçekten kaldırılan blokları sırayla fırlatır; her görsel blok bir kaldırılan bloğa karşılık gelir. Bloklar yere çarpınca parçalanır, tekrar yerleşmez ve eşya bırakmaz. Tooltip referanstaki kısa metindir; satırlar kelimeler bölünmeden otomatik ayrılır.
- **Drowning Mask:** Baş yuvasına takılır. Giyiliyken karada ve suda sonsuz Conduit Power I, Dolphin’s Grace I, Water Breathing I ve tam hava sağlar. Maske çıkarılınca kendi efektleri anında kaldırılır; geçici olarak yer değiştiren iksirler geçen süreleri düşülerek geri gelir. Daha güçlü iksirlerin altına gizlenmiş maske efektleri de temizlenir. İki el de boşken hedef gerekmeksizin yeni bir sağ tık basışı ve basılı tutmak, normalinin yarı boyunda parlayan bir trident oluşturur. Son bloğu koyarken tuş basılı kaldığında elin boşalması tridenti başlatmaz; tuşu bırakıp yeniden basmak gerekir. Hazırlanırken nişangâhın hemen altında, ortada ve üç ucu yatay durur; konumu bakışı anında, nişan yönü ise tick başına kalan açının %8’i kadar dönerek daha gecikmeli takip eder. Tam ters yöne bakınca da takılmadan döner. Vanilla sıfır-hız dönüşü uygulanmaz; sunucu düzeltmeleri eldeki görseli titreştirmez. Bırakınca normal tridentten hızlı (4 blok/tick) fırlar; toplanamaz ve normal çarpma hasarı vermez. Vurduğu mob beş saniye boyunca havada da görünen kabarcıklar ve boğulma sesleriyle boğulur; ölüm mesajı çıkar. Hedefleme ve engel kontrolü sunucuda yapılır.
- **Suffocation Boulder:** Bloğa sağ tık, üç saniyelik beyaz parlayan işaret ve ardından tüm küresel alanı aynı tick içinde tek malzemeye, tamamen kuma veya tamamen çakıla çevirir. İşaret blok rengini karartmaz. Dönüşümde %40 ses düzeyinde tek bir explosion sesi çalar; havai fişek sesi kullanılmaz. Moba vurmak veya sağ tıklamak boyuna uygun bir kafes kurar; mob hareket edemez ve boğulur. Kafesin bir parçasını kırmak kalan parçaları kaldırıp mobu serbest bırakır. Kafes de her kullanımda tek malzemedir. Oluşan tortu yaklaşınca, üstüne basınca veya zıplayınca kırılmaz; elle tek vuruşta kırılır.
- **Skeleton Bow:** Sağ tık basılıyken iki tickte bir sesle bir ok atar; ok başına 3 hasar verir (öncekinin yarısı). Ok tüketmez, kullanımda gerilme animasyonu oynamaz ve oklar toplanamaz. Ölümlerde `was killed by all the arrows` mesajı çıkar.
- **Mob Pickaxe:** Bekleme süresiz sağ tıkla dönüşümü aç/kapat; üç saniyelik yeşil/kırmızı action bar ve kalın perdeli netherite vuruş sesiyle durum bildirilir. Açıkken başarıyla kırılan bloklarda geçerli mob havuzundan rastgele bir mob doğar. Blok kırıldıktan sonraki tickte mob doğrudan kırılan bloğun yerinde oluşturulur; dar alan, bloklarla çakışma, su veya zemin eksikliği spawnı engellemez. Mob blokların içinde kalırsa normal suffocation hasarı alabilir. Havuz witch, guardian, elder guardian, husk, ghast, ravager ve diğer kara/su/uçan moblarla birlikte 74 tür içerir; Wither, Ender Dragon ve Warden seçilmez. Barışçıl modda düşman moblar seçilmez. Kazma normal demir kazma hızında kazar; tam dolu vuruşu zırh öncesinde 40 hasar verir ve özel ölüm mesajı kullanır.
- **Hunger Staff:** Bir moba sağ tıkla. Beş saniye boyunca kırmızı parlar; her saniye sesle özgün boyunun %10’u kadar küçülür. Gerçek çarpışma boyutu ve istemci görüntüsü birlikte değişir; küçülme mob NBT verisinde kalır. Hunger barın kendi ikonunu taşıyan beş parçacık, oyuncu hareket etse de akıcı bir yay çizerek ona uçar ve yeme sesiyle toplam 10 besin puanı getirir. Önce normal açlık barındaki eksik puanlar dolar; yalnızca taşan puanlar rezerve eklenir. Ek hunger ikonları kalplerin hemen üzerinde, zırh satırının altında yer alır; zırh satırı rezerv satırları arttıkça yukarı kayar. İkonlar her lokma için birer birer dolar, on ikon dolunca yeni satır açılır; sayısal +rezerv yazısı yoktur; yemek barı düşmeden önce rezerv tüketilir. Rezerv en fazla 100 puandır. Ek mod gerekmez.
- **Swarm of Anvils:** Bloğa veya moba sağ tıkla. On beş saniye boyunca eski boyutun iki katındaki örsler hedefin 3–5 blok yukarısından, her örs için değişen yatay yön ve eğimle aşağı doğru gelir. Yüzeyler sık ve hafif rastgele dağıtılmış noktalarla kaplanır; alt kısımlardaki örsler de yukarıdan aşağı hareket eder. Örsler geliş açısıyla saplanır, aynı açıyla kalır ve hedefi tamamen örter. Mobun yapay zekâsı, yerçekimi ve savrulması etki boyunca durdurulur; etki iptal edilirse eski durumu geri yüklenir. Sonunda blok kırılır; düşen eşya Infinity 255 alır. Mobun ganimetine rastgele 255. seviye büyüler eklenir.

- **Entity Crammer:** Moba sağ tık, içi bir blok genişliğinde ve üç blok yüksekliğinde ince cam kafes kurar. Mevcut sıradan bloklar gerekirse camla değiştirilir; konteyner, kırılmaz blok ve koruma izinleri gözetilir. Üstü açık kafeste cam paneller komşularına bağlanır; köylülere sağ tık ticaret yerine kafesi başlatır. Her 5 tickte aynı mobun tam NBT kopyası, zırhı ve elindeki eşyalarıyla oluşturulur. Kalabalık, dünyanın cramming sınırını aşınca (kural kapalıysa en çok 25 mob) cam kırılma sesi ve parçacıklarıyla patlar. Moblar önceki vektörün yarı şiddetiyle farklı yönlere fırlar ve 10 tick sonra ayrı ayrı gerçek hasarla ölür: `was squished too much`. Cloud parçacıkları çıkar. Cam kırılarak da bu aşama tetiklenebilir. Kafes kaldırılınca değiştirdiği bloklar geri gelir. Oyuncu ayrılırsa kopyalar temizlenir; kayıt/yükleme donmuş mob veya kalıcı cam bırakmaz.
- **Cactus Chestplate:** Gerçek göğüslüktür ve +6 zırh sağlar. Giyen oyuncuya vuran mobun üzerine üç farklı açıdan PDF’ye uygun gri-yeşil gövdeli, açık renk kenarlı ve sivri ucu hedefe dönük dikenler uçar ve saplanır. Mobun hareketi, yapay zekâsı ve savrulması durur; her yarım saniyede hasar alır ve `was pricked to death` mesajıyla ölür. İptalde özgün AI ve yerçekimi geri gelir.
- **TNT Cannon:** Sağ tık, bakış yönüne yerçekimli gerçek primed TNT fırlatır. Firework fırlatma sesi ve uçuş boyunca firework parçacıkları vardır. TNT 60 tick sonra veya yere çarptığı tickte patlar. Yakından oyuncunun altında patlayan kendi TNT’si sahibini yaralamaz. Aşağı ateş etmek oyuncuyu yukarı fırlatır; düşüş hasarı devam eder ve yalnızca altında patlayan kendi TNT atışından korunur. Kullanım ve el sallama animasyonu başlamaz. Arka arkaya atışlar arasında 12 tick vardır.
- **Fire Pants:** Gerçek pantolondur, +5 zırh ve giyiliyken sonsuz Fire Resistance I sağlar. Yerde koşarken hareket tickler arasında ölçülür; zıplamadan da alevler ve hızlanma başlar. Üç saniye boyunca hızlanır, sonra ek hız %150’de sabitlenir. Alev, kül ve büyük duman parçacıkları ayaklardan yükselir. Tam hızda bir blok yüksekliğe otomatik çıkar ve bastığı sıradan zemini obsidyene çevirir; 30 tick sonra aynı hücre gerçek lava dönüşür. Hız kesilince hız ve basamak bonusu kalkar. Lav yüzeyindeki geçici obsidyen gerçek çarpışmayla yürümeyi sağlar; ayrılınca lava geri döner. Konteynerler ve korumalı arazi değiştirilmez.
- **Bedrock:** Normal yatak gibi iki hücreye yerleşir. Örtüsü bedrock dokuludur. Her boyutta sağ tıkla 8 güçte yangınlı gerçek patlama üretir; yatak yerinde kalır, kullanan oyuncu patlama hasarı veya savrulma almaz. Yatağın yüksek patlama direnci kendi patlamasını yutmaz; çevredeki arazi gerçekten patlar. Eşyası ateşte/lavda yok olmaz. Kullanımlar arasında 30 tick vardır.
- **Ender Pearl Shield:** Ana veya yan elde sağ tık, özel pearl ve ametist sesiyle gerçek ender pearl atar. Çarpışmada hasarsız ışınlanılır; hedef dünya sınırı, yüklü chunk ve blok çarpışması açısından kontrol edilir. Kalkan eldeyken vuran mobun içinden 60 farklı yöne gerçek pearl fışkırır. İnişler sıraya alınarak iki tickte bir mobu farklı konumlara ışınlar; her seferinde ses, portal parçacıkları ve hasar vardır. Son darbe hedefi öldürür. Yalnızca kalkan eldeyken normal pearl hasarı da engellenir. Kalkan ve göğüslük birlikte kullanılıyorsa kalkan tepkisi önceliklidir.

PDF görsellerindeki dört yeni eşyanın tooltipleri ve belirtilen Swarm of Anvils cümlesi Türkçe ve İngilizce dil dosyalarında **orijinal İngilizce metinleriyle aynen** saklanır. TNT Cannon ve Bedrock açıklamaları konsepte uygun yazılmıştır.

Bütün eşya tooltipleri dil başına tek lang metninden gelir; PDF görsellerindeki açıklamalar korunur, kullanım talimatı eklenmez ve kelimeler satır ortasında bölünmez. Satırlar yazı genişliğine göre dengeli bölünür; son satırda birkaç kelime bırakılmaz.

Boulder, yay, kazma, asa ve örs ödülleri envanterde taşındıkları sürece kendi ölüm türlerine karşı bağışıklık sağlar. Bot, maske, kaktüs göğüslüğü ve ateş pantolonu giyilmelidir. Entity Crammer ve Bedrock taşınırken kendi ölüm türlerine karşı bağışıklık verir. TNT Cannon yalnızca yakından altında patlayan kendi atışından korur; başka TNT ve düşüş hasarına bağışıklık vermez. Ender Pearl Shield ise elde tutulmalıdır. Mob hedefleyen özel yetenekler oyuncuları, evcilleştirilmiş hayvanları, Ender Dragon ve Wither’ı hedeflemez.

## Soul Storm

Wither **efektinin hasarından** ölünce seçenekler sırasıyla **Withering Staff / Soul Storm / Bone Hand** olur; yalnızca ortadaki Soul Storm seçilir. Normal sandık sunumu, keşif kaydı ve tekrar ölüm kuralları geçerlidir. `/deathsareitems preview wither`, `/deathsareitems die wither` ve `/give @s deathsareitems:soul_storm` ile denenebilir. Envanterde taşımak wither hasarına bağışıklık verir.

- Eğilip sağ tık basılı tutulduğunda, başlangıçta yerden başın iki blok üstüne yükselirken genişleyen iki mor spiral açılır. Tutmaya devam etmek, 50 blok içindeki görülebilen en fazla 24 uygun mobu her 4 tickte 0,5 gerçek hasarla özgün boyunun %10’u kadar küçültür. Görüntü, göz yüksekliği ve çarpışma boyutu birlikte değişir; çok küçük kalan mob normal hasar/ölüm olayları üzerinden bitirilir. Tuşu veya eğilmeyi bırakınca toplama durur. Ender Dragon, Wither, oyuncular ve evcil hayvanlar toplanmaz. İngilizce ölüm bildirimi `withered away`, Türkçesi `solup gitti` olur.
- Her başarılı toplama darbesinde animasyonlu beyaz ruhlar aynı yay üzerinden mobdan oyuncuya akar. Her üçüncü darbede aralarına küçük beyaz stained glass görseli katılır. Ruhlar mesafeye göre 10–16 tickte oyuncuya ulaşır; cam varsa kaybolur, hafif pickup sesi çıkar ve her gelen ruh sayıyı bir artırır. Gerçek eşya oluşturulmadığı için cam alınamaz. Oyuncu hareket ederse uçuş onu takip eder. Eşya bırakılırsa, oyuncu ölürse/ayrılırsa veya boyut değişirse bekleyen toplama iptal edilir.
- Sayaç eşya eldeyken ve ruh sayısı sıfırdan büyükken action barda görünür; sıfır ruhla kullanım uyarı yazmaz: gri **Gathered**, açık mor **SOULS**, altın sayı. Türkçe **Toplanan RUHLAR** aynı renklerle gösterilir. Ruhlar eşyanın NBT verisinde saklanır; normal kayıt, yeniden giriş ve eşya aktarımıyla korunur.
- Eğilmeden sağ tık, wither atış sesiyle siyah duman, iri pikselli mor alev ve siyah-mor renklendirilmiş animasyonlu dalga halkaları taşıyan bir saldırı fırlatır. Saldırı duvar ve mob çarpışmalarını küçük aralıklarda tarar; maksimum 96 blok gider ve uçtuğu her 4 blok için bir ruh harcar. Yakın isabet bir ruh harcar; kaynak bittiğinde menzil sonundaki patlama oluşur. Sıfır ruhla atış yapılamaz. Vurulan moba patlamaya ek olarak 24 hasar verir; patlama gücü 4’tür. Obsidyenin bu saldırıya özel direnci düşüktür, böylece End kuleleri gerçekten parçalanır. Kırılmaz bloklar, konteynerler, spawn koruması ve Forge izinleri korunur. Kullanan oyuncu kendi patlamasından hasar ve savrulma almaz. Atışlar arasında 12 tick vardır.

Beyaz ruhlar vanilla soul animasyonunun 11 karesini, siyah-mor dalgalar sonic boom animasyonunun 16 karesini özel renk shader’ıyla kullanır. Mor alev dokusu 16×16 pikseldir. Üç boyutlu asa modeli obsidyen, netherite ve ametist dokularını kullanır. Görsel üretim yöntemi, tam promptlar ve dosya yolları `docs/soul-storm-assets.md` içindedir.

## End sonrası hardcore

End’e ilk giriş oyuncunun kalıcı son can aşamasını başlatır. Bu kural End’den çıkınca ve yeniden bağlanınca korunur. Sonraki gerçek ölüm ödül sandığı oluşturmaz; yeniden doğuş spectator modundadır ve yeniden girişle atlatılamaz. Kural oyuncu bazında uygulanır; diğer oyuncuların dünyasını veya dünya dosyasının global hardcore ayarını değiştirmez. End çıkış portalı ölüm sayılmaz.

Vanilla ender pearl hasarı normal düşüşle aynı `fall` kaynağını kullanır. Mod gerçek pearl inişini aynı tickte işaretleyerek bu ölüm için ayrı kalkan ödülünü seçer; sonraki normal düşüşler bot ödülü olmaya devam eder. Creeper gibi diğer patlamalar TNT ödülü vermez.

## İskelet ölümü

İskelet okundan ilk ölümde normal ölüm ve yeniden doğma akışı kullanılır. Yeniden doğunca doğrudan yay ödülünün sandık sunumu başlar. Diğer ödüllerde olduğu gibi sandıklar 3,5 saniye sonra yükselir.

## Test komutları

Yetki seviyesi 2 gerekir. Türler: `fall`, `drown`, `suffocate`, `skeleton`, `golem`, `starve`, `anvil`, `cram`, `cactus`, `tnt`, `lava`, `bed`, `pearl`, `wither`.

```text
/deathsareitems die <tür>
/deathsareitems preview <tür>
/deathsareitems reset
/give @s deathsareitems:drowning_mask
```

`die` bağışıklık ve ekipmanı atlayarak belirtilen ölüm akışını test eder; gerçek ölümdür ve normal ölüm kuralları geçerlidir. `preview` keşif geçmişini değiştirmeden sandıkları açar. `reset` yalnızca komutu kullanan oyuncunun keşif geçmişini sıfırlar; yeniden ödül kazanılabilmesini sağlar. Parametresiz `preview` düşüş ödülünü gösterir. Bütün ödüller yaratıcı sekmesindedir.

## Kaynaklar ve koruma

İlk on üç ödülde **vanilla doku/model placeholder’ları** kullanılır: altın bot, kaplumbağa kaskı, çakmaktaşı, yay, demir kazma, blaze çubuğu, örs, slime ball, demir zırh, crossbow ve ender pearl. Bedrock yatağı ve saplanan dikenler küçük JSON blok modelleri kullanır; yeni bitmap doku gerektirmez. Uçan yiyecekler ve küçük örsler vanilla display varlıklarıdır. Sonradan görünümü değiştirmek için davranış koduna dokunmak gerekmez. Türkçe/İngilizce metinler ve renkli kamp ateşi dumanı mevcuttur.

Arazi yetenekleri konteynerleri, kırılmaz blokları, sıvıları, dünya sınırını, spawn korumasını ve Forge kırma/yerleştirme iptallerini kontrol eder. Gecikmeli işlemlerde özgün blok ve izinler yeniden doğrulanır. Macera modunda arazi değiştirilmez. `config/deathsareitems-common.toml` içindeki `landingCushions`, `launchTerrain`, `maxLaunchedBlocks` ayarları korunmuştur.

## Kod ve doğrulama

`reward/` ölüm eşleşmeleri, keşif geçmişi, sandık sunumu ve teslimi; `ability/` her ödülün davranışı; `client/` giriş, efektler ve HUD; `item/custom/` eşya kullanımlarını içerir.

Java 17 ile:

```sh
./gradlew runData
./gradlew runGameTestServer
./gradlew build
./gradlew runClient
```

GameTest testleri gerçek Forge ortamında 14 ölümün eşleşmeleri, sahiplik, kayıt, tekrar ölüm, bağışıklık, açlık rezervi, gerçek küçülme, süreli dönüşüm/örs yağmuru, orijinal tooltipler, sonsuz/gizli efekt temizliği, crammer kopyaları ve gecikmeli ölüm, kaktüs tepkisi, TNT çarpışması ve güvenli fırlatma, lav yürüyüşü, hız limiti, kalıcı arazi zamanlaması, 60 pearl, gerçek yatak patlaması ve End sonrası kalıcı ölümü kapsar. Soul Storm için ayrıca wither eşleşmesi, tam seçenek sırası, kayıt, 0,5 hasar, gerçek küçülme, cam uçuşu ve teslim zamanı, tuş bırakma, evcil hayvan/dragon ayrımı, mesafeye göre ruh harcama, kaynak yetersizliği ve obsidyen patlaması kontrolleri bulunur.
