# Modelleri bozmadan Blockbench'te duruş düzenleme

Bu klasörde 14 eşyanın tamamının `.bbmodel` projesi var. Dokular dosyaların içinde;
eksik PNG aramana gerek yok. Önizlemeler modelin oyunda çizilen gerçek yüzeylerinden
alındı. Kaynak modeller ve dokular değiştirilmedi.

## Bir kez kur

1. Blockbench → **File → Plugins**.
2. Üstteki **Load Plugin from File** simgesinden `deathsareitems_display.js` dosyasını seç.
3. `projects` klasöründen istediğin `.bbmodel` dosyasını **File → Open Model** ile aç.

Bu yerel eklenti projelere **Display** modu ekler ve her görünüm için doğru model
önizlemesini gösterir. Örneğin iskeletli yayın mevcut birinci ve üçüncü şahıs
pozları birbirinden farklıdır; ikisi de ayrı ayrı korunur.

## Duruşu değiştir

**Display** moduna geç ve düzenleyeceğin görünümü seç:

| Blockbench görünümü | Oyundaki karşılığı |
| --- | --- |
| First Person Right / Left | Kendi ekranında sağ / sol elde tutuş |
| Third Person Right / Left | F5'te ve diğer oyunculara görünen tutuş |
| GUI | Envanter |
| Ground | Yere atılmış eşya |
| Fixed / Frame | Eşya çerçevesi |
| Head | Eşya olarak kafada gösterim |

- **Rotation:** yön / açı.
- **Translation:** ileri, geri, yukarı, aşağı konum.
- **Scale:** o görünümdeki boyut.

Aradığın ayarlar bunlar. Önizleme parçaları yanlışlıkla seçilmesin diye kilitli.
Modelin yüzeylerini, dokularını veya parçalarını düzenlemene gerek yok.
Minecraft 1.20.1 için Scale eksen başına en fazla 4, Translation ±80 olabilir.
Pivot alanlarını değiştirme; bu sürümün display JSON'u onları desteklemez.

**File → Save Project** ile aynı `.bbmodel` dosyasının üzerine kaydet
(macOS'ta Option+Cmd+S; Windows'ta Ctrl+Alt+S). Proje zaten bir `.bbmodel` olarak
açıldığı için normal kaydetme de proje dosyasına yazabilir; File → Save Project
her zaman doğru seçenektir.
**Export Java Block/Item** ile oyundaki model JSON'unun üzerine yazma.

## Moda aktar

Kaydettikten sonra `Duruslari_Aktar.command` dosyasına çift tıkla.
Bu komut `projects` klasöründeki projelerden sadece değişmiş `display` ayarlarını
kaynak `_base.json` dosyalarına aktarır. Önce eski dosyaları `backups` içine yedekler.
Model geometrisini, Forge loader'larını, Java renderer'larını ve dokuları almaz.
Eklentide model parçaları değişse bile bunlar moda aktarılmaz.

Terminal alternatifi (proje ana klasöründe):

```sh
# Sadece değişiklikleri göster; hiçbir şeyi yazmaz:
python3 tools/blockbench/display_projects.py apply

# Değişmiş duruşları kaydet:
python3 tools/blockbench/display_projects.py apply --write

# Tek bir eşyanın duruşunu kaydet:
python3 tools/blockbench/display_projects.py apply blockbench/projects/soul_storm.bbmodel --write
```

Kaynak JSON başka bir işlemle değişmişse araç eski bir önizlemeyi aktarmayı durdurur.
Bu durumda güncel modellerden yeniden proje oluşturmak gerekir.

## Oyunda gör

Geliştirme istemcisi kapalıysa `./gradlew runClient` ile yeniden başlat.
Açıksa değişen kaynakların `build/resources/main` içine kopyalanması için
`./gradlew processResources` çalıştır, sonra oyunda **F3+T** ile kaynakları yenile.
Sadece F3+T, `src/main/resources` içindeki dosyaları derleme klasörüne kopyalamaz.
Dağıtılacak mod için `./gradlew build` gerekir.

Bu projeler **eşyanın tutuşu ve gösterim boyutu** içindir. Maske, pantolon,
bot veya göğüslüğün giyilmiş zırh modelinin vücuda oturuşu `display` ile
ayarlanmaz; o ayrı bir zırh modeli düzenlemesidir.

## Geliştirici: Önizlemeleri yeniden üret

İlk kurulumda buna gerek yok; projeler hazır.
Mevcut `projects` klasörünü önce başka bir yere taşı (çalışmalarını koru).
Ardından proje kökünde:

```sh
./gradlew -I tools/blockbench/export.init.gradle runClient
python3 tools/blockbench/display_projects.py build
```

Geliştirme istemcisi açılıp modelleri yakalar ve kendiliğinden kapanır.
`tools/blockbench/java` yalnızca bu özel Gradle çalıştırmasına dahil edilir;
normal mod derlemesine veya dağıtılan JAR'a eklenmez.
