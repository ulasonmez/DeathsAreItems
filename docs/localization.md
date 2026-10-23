# Çeviri

Oyuncuya görünen moda ait metinler `src/main/resources/assets/deathsareitems/lang/en_us.json` dosyasındadır. Yeni bir dil için dosyayı aynı klasöre dil koduyla kopyalayın (örneğin `tr_tr.json`) ve yalnızca değerleri çevirin. Anahtarları değiştirmeyin. Resource pack içinde de `assets/deathsareitems/lang/<dil_kodu>.json` yolu kullanılır.

- Mob Pickaxe açma ve kapama bildirimleri ayrı, tam cümlelerdir. Cümleler Java içinde İngilizce parçalar birleştirilerek oluşturulmaz.
- `%s`, `%1$s`, `%2$s` gibi yer tutucuları koruyun. Numaralı yer tutucuların cümledeki sırasını değiştirebilirsiniz. `gathered_souls` için `%1$s` çevrilen `souls` metni, `%2$s` ruh sayısıdır. `preview_only` içindeki `%s` çevrilen ödül adıdır.
- Ölüm mesajlarında `%1$s` ölenin adıdır. Minecraft `.player` varyantına ayrıca saldırganı `%2$s`, `.item` varyantına saldırganı `%2$s` ve silahı `%3$s` olarak verir. Varsayılan metin bu ek bilgileri göstermese de çeviri bunları kullanabilir.
- Eşya açıklamalarını tek metin olarak çevirin; satırlara bölme, seçili dildeki metin üzerinden otomatik yapılır. Renkler Java tarafında korunur.
- Sunucudan gönderilen bildirimler çeviri anahtarı ve parametreleri taşır; her oyuncunun istemcisi kendi seçtiği dilde gösterir.

Cheat komutlarının özel başarı ve hata yanıtları kaldırılmıştır; bunlar için çeviri girdisi yoktur. Minecraft'ın kendi komut sözdizimi/yetki hataları ve normal ölüm mesajları kendi sistemleri üzerinden çalışır.
