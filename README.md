# Deaths Are Items

Minecraft **1.20.1 / Forge 47 / Java 17** için ölümden sonra animasyonlu eşya ödülleri.

## Bu sürümün akışı

- Yalnızca **düşüş hasarından ölümden** sonra, oyuncunun yeniden doğduğu konumun merkezinden yatayda tam üç blok uzaklıkta üç sandık belirir. Ateş, lav, boğulma, savaş ve diğer ölümler sandık veya bildirim üretmez. İlk giriş ve End çıkış portalı da ödül üretmez.
- Renkli, dönen kâğıt konfetileri ve başarım görünümünde bir bildirim oynar.
- Sandıklar toprak parçacıkları ve çakıl/toprak adım sesleriyle yerin altından yükselir; sarı ve yeşil büyü parçacıkları eşlik eder.
- Sandıklar döner, sırayla titrer ve sandık açılma sesiyle yerleşir. Animasyonun sonunda merkeze bakan **gerçek vanilla sandık bloklarına** dönüşürler. Her yerleşmenin ardından dünyaya sabitlenmiş, koyu kenarlıklı sarı isim görünür; oyuncunun veya kameranın dönüşünü takip etmez. Tabela çizimi kullanılır; harfleri örtebilen yarı saydam isimlik arka planı yoktur.
- Sandık sunumu ilk sürüme göre yaklaşık **%10 daha uzundur**. Yaklaşık 5,3 saniyelik animasyon tamamlanınca sahibi **Fall Damage Boots** sandığına sağ tıklar. Üç sandık birlikte kaybolur, bot ekranda vanilla totem animasyonuyla gösterilir ve **40 tick sonra** envantere eklenir. Dolu envanterde eşya sahibine ayrılmış şekilde yere bırakılır.
- Sandıklar gerçek blokların üstüne yazılmaz. Dar bir odada oyuncuya açık alana geçmesi söylenir ve konum tekrar denenir.
- Seçim başka oyunculara kapalıdır; erken veya tekrarlanan tıklamalar fazladan eşya vermez. Bekleyen seçim ve seçilmiş ödül dünya kaydında saklanır; çıkış, tekrar ölüm ve yeniden başlatma sırasında korunur.

Bu sürümün düşüş seçenekleri **Fall Damage Boots**, **Long Trip** ve **Slippery Banana Peel** şeklindedir. Yalnızca **Fall Damage Boots** seçilebilir ve bot verir. Diğer iki sandığa sağ tıklamak yalnızca önizleme mesajı gösterir; sandıkları kaldırmaz ve eşya vermez. Yeni ölüm türleri ve seçeneklerin vereceği eşyalar daha sonra ayrı tanımlarla eklenir. Tanımlanmamış ölüm türleri için varsayılan ödül yoktur.

## Fall Damage Boots

Botu **ayak zırhı yuvasına giy**. Çantada taşımak etkinleştirmez. Altın bot görünümünde, parıltılı, kırılmayan ve ateşe dayanıklı bir ödüldür.

- Düşüş hasarını engeller.
- Normal düşüşlerde, inişten hemen önce **su → slime → saman → su** sırasıyla **3×3** minder oluşturur. Sıra her bot çiftinin kendi NBT verisinde (`deathsareitems.NextCushion`) tutulur; kayıt, yeniden giriş ve başka oyuncuya aktarım sırasında korunur. Yeni bot suyla başlar. Hiçbir hücreye yerleştirilemeyen minder sırayı ilerletmez. Nether'daki su adımı buharlaşır, ardından slime gelir.
- Sadece boş hücrelere yerleşir; mevcut bloklar, sıvılar ve canlıların kapladığı alanlar korunur. Minderler dünyada kalır; su normal biçimde akar. Yerleştirme ve iniş sesleri ilgili bloğun ses türünden alınır; saman üzerinde slime sesi çalmaz.
- İnerken **Shift ile eğilmek**, siyah duman ve patlama efekti oluşturur. Ayak altındaki ve çevredeki en fazla dokuz blok sırayla yukarı fırlar ve döner.
- Fırlayan bloklar yere çarptığında parçalanır, oyuncu hasar sesi çıkarır ve ölüm mesajları açıksa `Stone fell from a high place` benzeri bir mesaj yayınlanır. Tekrar blok yerleştirmez ve eşya düşürmez.
- Kırılmaz bloklar, blok verisi taşıyan sandık/fırın gibi nesneler, sıvılar ve korunmuş bölgeler dışarıda bırakılır. Forge blok kırma/yerleştirme iptalleri dikkate alınır. Macera modunda arazi değiştirilmez.

## Kullanım ve ayarlar

- Önizleme: `/deathsareitems preview` (yetki seviyesi 2).
- Botu doğrudan alma: `/give @s deathsareitems:fall_damage_boots`.
- Yaratıcı sekmesi: **Deaths Are Items**.
- Türkçe ve İngilizce eşya adları, açıklamalar ve bildirimler mevcut.
- `config/deathsareitems-common.toml`: `landingCushions`, `launchTerrain`, `maxLaunchedBlocks`. Arazi fırlatmayı kapatmak hasar bağışıklığını kaldırmaz.

## Kodun yerleşimi

| Bölüm | Sorumluluk |
| --- | --- |
| `reward/DeathRewards`, `RewardDefinition` ve `RewardOption` | Ölüm türü eşleşmesi, üç seçenek, seçenek adı, seçilebilirlik ve vereceği eşya. Yeni ölüm türü için buraya tanım eklenir. |
| `reward/RewardManager` | Yeniden doğuş, güvenli sandık konumu, sahiplik kontrolü ve gecikmeli teslim. |
| `reward/RewardSavedData` | Dünya ile saklanan bekleyen sunumlar ve seçilmiş ödüller. |
| `reward/SettledRewardChests` | Animasyondan vanilla sandık bloklarına geçiş, blok sahipliği ve eski zeminin geri yüklenmesi. |
| `reward/RewardTimings` | Sunucu ve istemci için ortak animasyon süreleri. |
| `entity/custom` ve `entity/client` | Sandık/blok varlıkları ve ayrı çizicileri. |
| `ability/FallBootsAbility` | Düşüş bağışıklığı, minder ve sırayla arazi fırlatma. |
| `ability/CushionCycle` | Her bot çiftinin NBT verisindeki su/slime/saman sırası. |
| `client` ve `particle` | Kâğıt konfeti, bildirim ve totem gösterimi. |
| `network/ModNetwork` | Yalnızca sunucudan istemciye görsel bildirimler. |
| `gametest/DeathRewardsGameTests` | Gerçek Forge ortamında davranış testleri. |

İstemciye özgü çizim ve parçacık sağlayıcıları `Dist.CLIENT` olaylarında kaydedilir. Ağ kodu Forge'un [SimpleImpl](https://docs.minecraftforge.net/en/1.20.1/networking/simpleimpl/) yapısını, konfeti ise [parçacık sağlayıcılarını](https://docs.minecraftforge.net/en/1.20.1/gameeffects/particles/) kullanır.

## Doğrulama

Java 17 seçildikten sonra:

```sh
./gradlew runData
./gradlew runGameTestServer
./gradlew build
./gradlew runClient
```

11 oyun testi; gerçek düşüş hasarını, doğrudan düşüş hasarını, dolu hücrelerin korunmasını, blok seçimini, kayıtların yeniden yüklenmesini, sandık sahipliğini/erken tıklamayı/çift seçimi, önizleme seçeneklerinin kilitli kalmasını, üç blokluk konumları ve sandık yönlerini, vanilla sandıkların kayıt sonrası temizlenmesini, yalnızca düşüş ölümünün eşleşmesini, botların kalıcı minder sırasını, iniş seslerini ve fırlayan blokların yaşam döngüsünü doğrular.

Elle görsel kontrol için açık alanda `/deathsareitems preview` kullan; ardından botla normal ve eğilerek yüksekten iniş dene. Bildirim, konfeti yoğunluğu, sandık sıralaması, sarı yazılar ve totem gösterimi bu akışta görülebilir.
