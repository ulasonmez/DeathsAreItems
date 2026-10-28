# Einfach görsel kaynakları

Minecraft Java **1.20.1**, Forge **47.1.3**, namespace `deathsareitems`.

| Varlık | Son görünüm | Üretim texture boyutu |
|---|---|---|
| Huş Kıymıkları | İki parçalı, kabuk lekeli 2D kıymık | 16×16 |
| Viny'nin Kovası | Gerçek vanilla aksolotl kovası üzerinde BLUE paleti | 16×16 |
| Juky'nin Düdüğü | Girintili hava kanalı, içi boş ağızlık, J amblemleri ve pirinç halka; 27 cuboid | 16×16 |
| Peynir Dilimi | Verilen PNG, byte düzeyinde korunmuş, `item/generated` 2D | 32×32 |
| Hamburger | EquipEnchantments `einfach` dalındaki beğenilen 12 cuboid model | 16×16 |
| Gustaf'ın Müzik Kutusu | Tam vanilla küp; koyu mavi gövde, pirinç ızgara; huş alt yüz | 16×16 yüzler |
| Kitap | Verilen 4 cuboid model, kapak, UV ve tutuşlar | Kapak 870×634; gövde 32×32 |
| Juky | Vanilla WolfRenderer, vanilla kurt ve animasyonları | Oyunun texture'ı |
| Viny | Vanilla AxolotlRenderer, BLUE varyantı ve animasyonları | Oyunun texture'ı |
| Düğme TNT'si | Vanilla TNT; ayrı asset yok | Oyunun texture'ı |

`references/about.txt` kullanıcı tarafından okunması istenen içerik notudur. Kitap adı son istek doğrultusunda bütün altı dilde alt çizgisiz `EinfachGustaf und die Wächter der Stille` oldu. Oyuncu skini oyunda kullanılmadı.

`provenance.json` kaynak dal/commit ve dosya SHA-256 değerlerini içerir. Vanilla tabanları yerel ForgeGradle'ın Mojang 1.20.1 `downloadClient/client.jar` dosyasından alındı; oyun dosyaları değiştirilmedi. Yeni tasarımlar için harici referans verilmedi: kıymık silüeti, düdük geometrisi, kovanın mavi paleti ve kutunun renkleri tasarım kararlarıdır. Kitabın ve hamburgerin mevcut geometri, UV ve Display ayarları korundu; texture namespace'i uyarlandı, kitabın bozuk particle bağlantısı gövde texture'ına bağlandı. Kullanıcının büyük/asimetrik kapak resmi küçültülmedi; Minecraft bu boyut için mip seviyesini sınırlar.

## Düzenleme ve aktarım

`projects/hamburger.bbmodel`, `projects/gustafs_new_book.bbmodel` ve `projects/jukys_whistle.bbmodel` tam statik geometri kaynaklarıdır; snapshot değildir. Texture'lar gömülüdür. Geometri, UV, texture ve GUI/iki el birinci/üçüncü şahıs Display düzenlemelerini aynı dosyaya kaydet. Mevcut dosyalar yeniden oluşturulmaz.

Repo kökünden:

```sh
python3 tools/einfach/export_models.py                  # yalnız farklar
python3 tools/einfach/export_models.py --write          # kaynakları oyuna aktar
python3 tools/einfach/export_models.py --check          # eşleşmeyi doğrula
# Tek model: python3 tools/einfach/export_models.py --write jukys_whistle
python3 tools/einfach/draw_assets.py                    # 2D ve blok kaynaklarını üret
python3 tools/einfach/validate_assets.py
python3 tools/einfach/preview_models.py                 # Pillow + numpy
```

3D modellerin tek kaynağı `.bbmodel`; `runtime.json` parent/render metadata'sını tutar. 2D ve blok tasarımlarının kaynağı `draw_assets.py`, peynirinki `references/cheese.png` dosyasıdır. Eski `tools/build_*` script'leri bu yeni asset'lere yazmaz. Exporter önce tüm girdileri doğrular, desteklenmeyen animasyon/grup dönüşlerini reddeder ve yalnız değişen çıktıları yazar. Java animasyon ve oyun davranışını değiştirmez. Standard Blockbench `front_gui_light` ayarı aktarılır.

Java 17 ile `./gradlew processResources`, ardından açık geliştirme istemcisinde F3+T kullan. Viny'nin BLUE ayarı gibi Java değişikliklerinde istemci yeniden başlatılmalıdır. Dağıtım için `./gradlew build --offline`; JAR `build/libs/deathsareitems-1.0.0.jar` konumundadır. Eski 14 ölüm eşyası, mevcut `blockbench/` Display iş akışını kullanmaya devam eder.

## Kontroller ve sınırlar

`build` ve `runData` başarılı. Yedi itemin parent/texture yolları, blok bağlantıları, PNG boyutları/alpha, altı dil adı, verilen peynir/kitap dosyalarının byte eşitliği, hamburger geometri/UV/tutuş eşitliği, kitap geometri/UV/tutuş eşitliği ve zip bütünlüğü `validate_assets.py` ile kontrol edildi. 2D üretiminin tekrarında hiçbir asset değişmedi. 3D exporter değişikliksiz tekrarında fark üretmedi. Tek Display yuvası düzenlemesinin diğer yuvaları, geometriyi ve texture'ları değiştirmediği de kontrol edildi.

`previews/models.png`, `*-display.png` ve `*-checker.png` gerçek runtime JSON/UV'den üretilmiş çevrimdışı incelemelerdir. Kamera ölçeği modeller arasında sabittir. Display görüntüleri Minecraft oyuncusu, kolu, perspektif kamerası veya shader'ı değildir. Checker görüntüleri UV örneklemesini incelemek içindir; miras alınan eski hamburger UV'si yeniden paketlenmedi.

Gerçek Minecraft GPU kontrolü:

```sh
./gradlew -I tools/einfach/visualqa.init.gradle runClient --offline
```

Ayrı test istemcisi yedi modeli normal/enchanted geçişlerinde çizip resource reload sonrasında tekrar çizer; kayıt dünyası yüklemez ve kendiliğinden kapanır. `minecraft-before-reload.png` ve `minecraft-after-reload.png` gerçek ekran yakalamalarıdır. Bu kontrol başarılı; eksik model/texture yok. QA kaynakları ayrı source set'tedir, release JAR'ına girmez. İki elin gerçek oyuncu kamerasında kullanımı, mob hareketleri, takas ve recipe book ekranları görsel olarak denenmedi. Blockbench UI kullanılmadı.

## İstenen hamburger paketi

`delivery/Hamburger/model/` orijinal EquipEnchantments JSON + `.bbmodel`, `delivery/Hamburger/texture/` orijinal 16×16 PNG içerir. `delivery/Einfach_textures.zip`, kullanıcının zipindeki mevcut girdiler korunarak bu klasörün eklendiği sürümdür. Hamburger paketindeki kaynaklar EquipEnchantments namespace'ini korur; bu moddaki runtime sürümü `deathsareitems` kullanır.

## Düdük revizyonu

27 cuboid model mevcut Display ayarlarını korur; atlas hâlâ gerçek 16×16'dır.
Gömülü PNG ve geometri için esas kaynak `projects/jukys_whistle.bbmodel` dosyasıdır.
Çağırma ve geri çağırma sesleri özgün sentezlenmiş mono Vorbis OGG'dir (44,1 kHz).
İki sesi yeniden üretmek için numpy ve soundfile kurulu Python ile:

```sh
python3 tools/einfach/build_whistle_sounds.py
```

Sesler `src/main/resources/assets/deathsareitems/sounds/` dizinine yazılır.
