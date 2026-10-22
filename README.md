# Deaths Are Items

Minecraft **1.20.1 / Forge 47 / Java 17**. Yedi farklı ölüm, animasyonlu sandık seçimi ve her ölümün temasını moblara uygulayan özel ödüller.

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

Her tür oyuncu başına bir kez keşfedilir. Tekrar eden veya tanınmayan ölümlerde sandık, konfeti ve ses oynatılmaz; yeniden doğunca beş saniye kırmızı **find a different death...** görünür. Keşif geçmişi, bekleyen ödüller ve seçilmiş eşyalar dünya kaydında saklanır. İlk giriş ve End çıkış portalı ölüm sayılmaz.

İlk keşiften sonra yeniden doğuşta ses, konfeti ve ödül bildirimi gelir. **3,5 saniye sonra** oyuncunun o andaki konumundan üç blok uzakta üç sandık yükselir. Üç seçenekten yalnızca tablodaki ödül seçilebilir. Önizleme sandıkları eşya vermez. Seçimde sarı/yeşil kamp ateşi dumanı ve 40 tick totem gösterimi oynar, ardından eşya teslim edilir. Bildirim simgesi ilgili ödüldür.

Sandıklar gerçek blokların üstüne yazılmaz. Dar alanda açık yere geçilmesi istenir ve tekrar denenir. Sahiplik, uzaklık, animasyon süresi ve çift tıklama doğrulanır. Dolu envanterde ödül sahibine ayrılmış eşya olarak bırakılır.

## Ödüller

- **Fall Damage Boots:** Ayak yuvasına giyilir, düşüş hasarını engeller. Normal inişte boş hücrelere 3×3 su → slime → saman minderleri koyar. Sıra botun NBT verisinde korunur. Eğilerek inişte zeminden gerçekten kaldırılan blokları sırayla fırlatır; her görsel blok bir kaldırılan bloğa karşılık gelir. Bloklar yere çarpınca parçalanır, tekrar yerleşmez ve eşya bırakmaz. Tooltip referanstaki kısa metindir; satırlar kelimeler bölünmeden otomatik ayrılır.
- **Drowning Mask:** Baş yuvasına takılır. Yalnızca su veya bubble column içindeyken Conduit Power III, Dolphin’s Grace III, Water Breathing III ve tam hava sağlar. Karaya çıkınca ya da maske çıkarılınca maskenin verdiği efektler anında kaldırılır; başka kaynaklardan gelen efektler korunur. İki el de boşken hedef gerekmeksizin yeni bir sağ tık basışı ve basılı tutmak, normalinin yarı boyunda parlayan bir trident oluşturur. Son bloğu koyarken tuş basılı kaldığında elin boşalması tridenti başlatmaz; tuşu bırakıp yeniden basmak gerekir. Hazırlanırken nişangâhın hemen altında, ortada ve üç ucu yatay durur; konumu bakışı anında, nişan yönü ise tick başına kalan açının %8’i kadar dönerek daha gecikmeli takip eder. Tam ters yöne bakınca da takılmadan döner. Vanilla sıfır-hız dönüşü uygulanmaz; sunucu düzeltmeleri eldeki görseli titreştirmez. Bırakınca normal tridentten hızlı (4 blok/tick) fırlar; toplanamaz ve normal çarpma hasarı vermez. Vurduğu mob beş saniye boyunca havada da görünen kabarcıklar ve boğulma sesleriyle boğulur; ölüm mesajı çıkar. Hedefleme ve engel kontrolü sunucuda yapılır.
- **Suffocation Boulder:** Bloğa sağ tık, üç saniyelik beyaz parlayan işaret ve ardından tüm küresel alanı aynı tick içinde tek malzemeye, tamamen kuma veya tamamen çakıla çevirir. İşaret blok rengini karartmaz. Dönüşümde %40 ses düzeyinde tek bir explosion sesi çalar; havai fişek sesi kullanılmaz. Moba vurmak veya sağ tıklamak boyuna uygun bir kafes kurar; mob hareket edemez ve boğulur. Kafesin bir parçasını kırmak kalan parçaları kaldırıp mobu serbest bırakır. Kafes de her kullanımda tek malzemedir. Oluşan tortu yaklaşınca, üstüne basınca veya zıplayınca kırılmaz; elle tek vuruşta kırılır.
- **Skeleton Bow:** Sağ tık basılıyken iki tickte bir sesle bir ok atar; ok başına 3 hasar verir (öncekinin yarısı). Ok tüketmez, kullanımda gerilme animasyonu oynamaz ve oklar toplanamaz. Ölümlerde `was killed by all the arrows` mesajı çıkar.
- **Mob Pickaxe:** Bekleme süresiz sağ tıkla dönüşümü aç/kapat; üç saniyelik yeşil/kırmızı action bar ve kalın perdeli netherite vuruş sesiyle durum bildirilir. Açıkken başarıyla kırılan bloklarda geçerli mob havuzundan rastgele bir mob doğar. Blok kırıldıktan sonraki tickte mob doğrudan kırılan bloğun yerinde oluşturulur; dar alan, bloklarla çakışma, su veya zemin eksikliği spawnı engellemez. Mob blokların içinde kalırsa normal suffocation hasarı alabilir. Havuz witch, guardian, elder guardian, husk, ghast, ravager ve diğer kara/su/uçan moblarla birlikte 74 tür içerir; Wither, Ender Dragon ve Warden seçilmez. Barışçıl modda düşman moblar seçilmez. Kazma normal demir kazma hızında kazar; tam dolu vuruşu zırh öncesinde 40 hasar verir ve özel ölüm mesajı kullanır.
- **Hunger Staff:** Bir moba sağ tıkla. Beş saniye boyunca kırmızı parlar; her saniye sesle özgün boyunun %10’u kadar küçülür. Gerçek çarpışma boyutu ve istemci görüntüsü birlikte değişir; küçülme mob NBT verisinde kalır. Hunger barın kendi ikonunu taşıyan beş parçacık, oyuncu hareket etse de akıcı bir yay çizerek ona uçar ve yeme sesiyle toplam 10 besin puanı getirir. Önce normal açlık barındaki eksik puanlar dolar; yalnızca taşan puanlar rezerve eklenir. Ek hunger ikonları kalplerin hemen üzerinde, zırh satırının altında yer alır; zırh satırı rezerv satırları arttıkça yukarı kayar. İkonlar her lokma için birer birer dolar, on ikon dolunca yeni satır açılır; sayısal +rezerv yazısı yoktur; yemek barı düşmeden önce rezerv tüketilir. Rezerv en fazla 100 puandır. Ek mod gerekmez.
- **Swarm of Anvils:** Bloğa veya moba sağ tıkla. On beş saniye boyunca eski boyutun iki katındaki örsler hedefin 3–5 blok yukarısından, her örs için değişen yatay yön ve eğimle aşağı doğru gelir. Yüzeyler sık ve hafif rastgele dağıtılmış noktalarla kaplanır; alt kısımlardaki örsler de yukarıdan aşağı hareket eder. Örsler geliş açısıyla saplanır, aynı açıyla kalır ve hedefi tamamen örter. Mobun yapay zekâsı, yerçekimi ve savrulması etki boyunca durdurulur; etki iptal edilirse eski durumu geri yüklenir. Sonunda blok kırılır; düşen eşya Infinity 255 alır. Mobun ganimetine rastgele 255. seviye büyüler eklenir.

Bütün eşya tooltipleri dil başına tek lang metninden gelir; PDF görsellerindeki açıklamalar korunur, kullanım talimatı eklenmez ve kelimeler satır ortasında bölünmez.

Boulder, yay, kazma, asa ve örs ödülleri envanterde taşındıkları sürece kendi ölüm türlerine karşı bağışıklık sağlar. Bot ve maske ise giyilmelidir. Mob hedefleyen özel yetenekler oyuncuları, evcilleştirilmiş hayvanları, Ender Dragon ve Wither’ı hedeflemez.

## İskelet ölümü

İskelet okundan ilk ölümde normal ölüm ve yeniden doğma akışı kullanılır. Yeniden doğunca doğrudan yay ödülünün sandık sunumu başlar. Diğer ödüllerde olduğu gibi sandıklar 3,5 saniye sonra yükselir.

## Test komutları

Yetki seviyesi 2 gerekir. Türler: `fall`, `drown`, `suffocate`, `skeleton`, `golem`, `starve`, `anvil`.

```text
/deathsareitems die <tür>
/deathsareitems preview <tür>
/deathsareitems reset
/give @s deathsareitems:drowning_mask
```

`die` bağışıklık ve ekipmanı atlayarak belirtilen ölüm akışını test eder; gerçek ölümdür ve normal ölüm kuralları geçerlidir. `preview` keşif geçmişini değiştirmeden sandıkları açar. `reset` yalnızca komutu kullanan oyuncunun keşif geçmişini sıfırlar; yeniden ödül kazanılabilmesini sağlar. Parametresiz `preview` düşüş ödülünü gösterir. Bütün ödüller yaratıcı sekmesindedir.

## Kaynaklar ve koruma

Özel bitmiş eşya modelleri yerine **vanilla doku/model placeholder’ları** kullanılır: altın bot, kaplumbağa kaskı, çakmaktaşı, yay, demir kazma, blaze çubuğu ve örs. Uçan yiyecekler ve küçük örsler vanilla display varlıklarıdır. Sonradan görünümü değiştirmek için davranış koduna dokunmak gerekmez. Türkçe/İngilizce metinler ve renkli kamp ateşi dumanı mevcuttur.

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

GameTest testleri gerçek Forge ortamında ödül eşleşmeleri, sahiplik, kayıt, tekrar ölüm, bağışıklık, açlık rezervi, gerçek küçülme, süreli dönüşüm/örs yağmurunu kapsar.
