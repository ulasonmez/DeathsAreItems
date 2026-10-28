# Einfach içerik ve model listesi

`einfach` branchindeki işlevsel ekler ve tamamlanmış görseller. Model/texture
kaynakları ve aktarım komutları: [art/models/einfach/README.md](../art/models/einfach/README.md).
Düdük iki özgün mono OGG ıslık kullanır; diğer sesler vanilla kaynaklarındandır.
Bütün kimliklerde namespace `deathsareitems:`.

| Ad | Kimlik | Kayıt türü | Görsel |
|---|---|---|---|
| Huş Kıymıkları | `birch_splinters` | Normal item, 64'lü yığın | 16×16 özel 2D huş kıymıkları |
| Viny'nin Kovası | `vinys_bucket` | Yeniden kullanılabilir item, tekli | Vanilla kova üzerinde mavi aksolotl, 16×16 2D |
| Juky'nin Düdüğü | `jukys_whistle` | Yeniden kullanılabilir item, tekli | 27 parçalı özel 3D metal düdük, 16×16 atlas |
| Peynir Dilimi | `cheese_slice` | Yenebilir item, 64'lü yığın | Gönderilen 32×32 peynir PNG, 2D |
| Hamburger | `hamburger` | Yenebilir item, 64'lü yığın | EquipEnchantments einfach dalından 12 parçalı 3D model, 16×16 atlas |
| Gustaf'ın Müzik Kutusu | `gustafs_jukebox` | Yerleştirilebilir blok + BlockItem + block entity | Vanilla küp geometrisi, 16×16 koyu mavi/pirinç yan ve üst yüzler |
| Gustaf'ın Yeni Kitabı | `gustafs_new_book` | Yeniden kullanılabilir büyü item'i, tekli | Gönderilen 4 parçalı kitap modeli ve özgün kapak; adı about.txt doğrultusunda altı dile eklendi |
| Juky | `juky` | Özel mob, vanilla Wolf alt sınıfı | Vanilla kurt modeli/texture/animasyonları; 0,6 × 0,85 blok hitbox |
| Viny | `viny` | Özel mob, vanilla Axolotl alt sınıfı | Vanilla BLUE aksolotl modeli/texture/animasyonları; 0,75 × 0,42 blok hitbox |
| Düğme TNT'si | `button_tnt` | Özel primed TNT entity | Vanilla ateşlenmiş TNT; 0,98 × 0,98 blok, ayrı görsel gerekmiyor |

Item'lar modun yaratıcı sekmesinde bulunur. Moblar doğal doğmaz; ilgili eşya
üzerinden sahibiyle birlikte oluşturulur. Mob spawn egg eklenmedi. Juky ve Viny, `about.txt` doğrultusunda vanilla iskeletlerini ve animasyonlarını
kullanır. Viny yeni çağırmada ve eski kayıttan yüklemede BLUE varyantına ayarlanır.

## Oynanış

- Huş kütüğü, soyulmuş huş kütüğü ve iki huş odunu varyantı yalnız kıymık düşürür.
  Hayatta kalma modunda elle kırmak 1 kalp kıymık hasarı verir. Bu hasar kaktüs
  ölümü sayılmaz. Kıymık üretimde, yiyecek veya yakıt olarak kullanılmaz.
- Yerleştirilmiş düğmeyi elle kullanmak 60 tick fitilli tek TNT üretir. Sesli
  uyarı verir; varlıklara hasar verirken araziyi korur. Redstone/mermi tetiklemesi
  bu mekanizmayı çalıştırmaz. Aynı düğme, TNT yok edilene/patlayana kadar kilitlidir.
- İlk gerçek boğulma ödülü alınınca maskeye ek olarak Viny'nin Kovası gelir.
  Suya sağ tıklamak Viny'yi çağırır; eğilip sağ tıklamak geri çağırır. Maske
  takılıyken 8 blok içindeki bir düşmanı 4 saniye su balonunda tutar; bekleme
  süresi 15 saniyedir. Boss yalnız hasar alır. Viny suda takip eder; karaya
  çıkınca veya sahibinden 32 bloktan fazla uzaklaşınca kovaya döner.
- İlk gerçek golem ödülü alınınca kazmaya ek olarak Juky'nin Düdüğü gelir.
  Sağ tık çağırır, eğilip sağ tık geri çağırır. Juky'ye boş elle eğilip sağ
  tıklamak bulunduğu noktayı koruma/takip modunu değiştirir. Yakın düşmanlara
  saldırır, 20 saniyede bir 6 blok içindeki düşmanları uluyarak iter.
- Oyuncu başına her yoldaştan tek tane bulunabilir. Kopya eşya çoğaltamaz.
  Oyunculara, pasif hayvanlara ve sahipli hayvanlara saldırmazlar; ölüm
  eşyalarının alan hasarları onları hedeflemez. Yenilirlerse 30 saniye sonra
  yeniden çağrılabilirler. Ölümde, çıkışta veya boyut değişiminde geri dönerler.
  Beceri ve yenilgi bekleme süreleri kopyalar, yeniden çağırma ve sunucu
  yeniden başlatmaları arasında ortaktır.
- İlk açlık ödülüne ek olarak 8 peynir dilimi ve 2 hamburger gelir; tarifler
  tarif kitabına açılır. Bir süt kovası fırında 200 tick sürede 4 peynir olur.
  Boş kova fırının giriş yuvasında kalır. Şekilsiz hamburger tarifi:
  2 ekmek + 1 pişmiş biftek + 1 peynir dilimi = 1 hamburger.
- İki yiyecek de tokken yenir. Peynir 4, hamburger 12 açlık puanı sağlar;
  fazlası mevcut Açlık Asası rezervine, 100 puan sınırıyla eklenir. Peynir
  ortak 10 saniyelik süreyle 1 kalp iyileştirir. Hamburger 5 saniye Yenilenme II
  sağlar; tekrar yemek süreyi yeniler.
- 5 farklı gerçek ölüm ödülü sandıktan alınınca müzik kutusu ve vanilla Cat
  diski gelir. Kutuyu yerleştiren oyuncu sahibi olur. Disk çalarken 16 blok
  içinde sahibi Kuvvet II ve Direnç II alır; her 5 saniyede bir düşmanlara
  16 hasar ve geri itme uygular. Bosslar itilmez. Birden fazla kutu hasarı
  çoğaltmaz. Diski çıkarmak/durdurmak etkiyi bitirir. Kutu kırılınca taşınabilir.
- 10 farklı gerçek ölüm ödülünde kitap gelir. Eğilip sağ tık yalnız keşfedilmiş
  bölümler arasında geçer; sağ tık 24 blok içinde görüş hattındaki moba
  büyü yapar. Bölümler: düşme (yere çarpma), boğulma (su balonu), örs (yoğun
  yağmur), kaktüs (diken halkası), wither (ruh patlaması). Kitabın
  bekleme süresi yoktur. Su/diken kafesleri blok değiştirmeden vanilla
  parçacıklarla gösterilir. Bosslar kafese alınmaz ve zorla hareket ettirilmez.
  Kitabın adı `EinfachGustaf und die Wächter der Stille`; tooltip diğer ölüm eşyaları gibi
  kısa ve esprili tek açıklama gösterir; kitabın açıklaması açık mordur. Bölüm geçişi ve büyü bildirimi bölüm rengindedir.
  Kendi aktif büyüsünü yeni bölümle değiştirebilir; diğer oyuncunun/Viny'nin
  hapsetmesini bozamaz. Her başarılı büyü 8 saniyelik bölüm desteği verir:
  düşme → Yavaş Düşüş, boğulma → Suda Nefes, örs → Direnç, kaktüs → Hız,
  wither → Yenilenme. Aynı hedefe 8 saniyede üç farklı bölüm Sessiz Final'i
  tetikler: 6 blokta görüş hattındaki moblara 16 hasar ve boss olmayan moblara
  3 saniye Yavaşlık IV. Tekrar edilen bölüm mısra sayısını artırmaz veya
  süreyi uzatmaz; hedef değişimi/sürenin dolması birleşimi sıfırlar.
  Kitap iki elden birindeyken 12 blok içindeki moblar Parlama ile görünür olur.
  Kitabın tüm özellikleri pasif, nötr ve evcil moblarda da çalışır; Juky ve Viny
  bu kapsama dahildir. Yoldaşların diğer ölüm eşyalarına karşı koruması sürer.
  Müzik kutusunun tooltipi ve ilgili altı çeviri anahtarı kaldırılmıştır.

Önizleme komutunun oluşturduğu sandığı almak, aynı ölümü tekrar etmek, `/give`,
üretim ve yoldaş çağırmak milestone sayacını ilerletmez. Bonuslar ve milestone
geçmişi dünya SavedData'sında tutulur; keşifleri sıfırlamak bonusları yeniden
vermez. Temel ödül sandıklarının üçlü sunumu ve ana ödülleri korunmuştur.
Kitap veya yoldaşlar End Hardcore ölüm kilidini kaldırmaz.

Yeni adlar, tooltip'ler, bölüm adları, bildirimler ve kıymık ölüm mesajları
`en_us`, `tr_tr`, `de_de`, `ko_kr`, `pt_br`, `pt_pt` dosyalarında bulunur.
