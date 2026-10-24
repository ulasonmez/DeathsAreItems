# Çeviri

Oyuncuya görünen moda ait metinler `src/main/resources/assets/deathsareitems/lang/en_us.json` dosyasındadır. Yeni bir dil için dosyayı aynı klasöre dil koduyla kopyalayın (örneğin `tr_tr.json`) ve yalnızca değerleri çevirin. Anahtarları değiştirmeyin. Resource pack içinde de `assets/deathsareitems/lang/<dil_kodu>.json` yolu kullanılır.

- Mob Pickaxe açma ve kapama bildirimleri ayrı, tam cümlelerdir. Cümleler Java içinde İngilizce parçalar birleştirilerek oluşturulmaz.
- `%s`, `%1$s`, `%2$s` gibi yer tutucuları koruyun. Numaralı yer tutucuların cümledeki sırasını değiştirebilirsiniz. `gathered_souls` için `%1$s` çevrilen `souls` metni, `%2$s` ruh sayısıdır. `preview_only` içindeki `%s` çevrilen ödül adıdır.
- Ölüm mesajlarında `%1$s` ölenin adıdır. Minecraft `.player` varyantına ayrıca saldırganı `%2$s`, `.item` varyantına saldırganı `%2$s` ve silahı `%3$s` olarak verir. Varsayılan metin bu ek bilgileri göstermese de çeviri bunları kullanabilir.
- Eşya açıklamalarını tek metin olarak çevirin; satırlara bölme, seçili dildeki metin üzerinden otomatik yapılır. Renkler Java tarafında korunur.
- Sunucudan gönderilen bildirimler çeviri anahtarı ve parametreleri taşır; her oyuncunun istemcisi kendi seçtiği dilde gösterir.

## Kelime oyunları ve ödül seçenekleri

Ödül adlarını kelime kelime çevirmek yerine ölüm nedeni, eşyanın görünüşü ve yeteneğiyle ilişkisini koruyun. Her ölümün üç seçeneğini `DeathRewards.java` içindeki grup olarak değerlendirin. Hedef dilde aynı kelime oyunu yoksa doğal bir deyim, ses oyunu veya aynı temada yeni bir espri kullanın. Tema zaten anlaşılıyorsa adı değiştirmek gerekmez.

Örneğin İngilizce `Bedrock`, hem katman kayasını hem `bed` (yatak) sözcüğünü çağrıştırır. Bu moddaki eşya gerçekten taş bir yataktır. Yalnızca Minecraft'ın normal katman kayası adını kullanmak diğer dillerde bu bağlantıyı kaybettirir:

| Dil | Yatak ölümü: yastık / taş yatak / patlayıcı örtü |
| --- | --- |
| İngilizce | Fluffy Pillow / Bedrock / Explosive Blankie |
| Türkçe | Pofuduk Yastık / Taş Gibi Yatak / Bomba Battaniye |
| Almanca | Flauschiges Kissen / Steinhartes Bett / Knallige Kuscheldecke |
| Korece | 폭신한 베개 / 돌침대 / 이불 밖은 폭발 |
| Brezilya Portekizcesi | Travesseiro Fofinho / Cama de Pedra / Cobertor Bombástico |
| Portekiz Portekizcesi | Almofada Fofinha / Cama de Pedra / Manta Bombástica |

`Hunger Employee / Hunger Manager / Hunger Staff` grubunda `staff` hem personel hem asa anlamındadır. Türkçede bu iki anlam tek sözcükte birleşmediği için grup `Açlık Stajyeri / Açlık Müdürü / Açlığın Patronu` olarak uyarlanır; ödül yine aynı asadır. Almancadaki `Hungerstab` hem asa hem yönetim kadrosu çağrışımını korur. Korecede `허기 사장의 지팡이`, Portekizcede `Cajado do Chefão` ve `Cajado do Patrão`, asayı şirketin patronuyla ilişkilendirir. Asa açıklamaları da terfi ve yemek temasıyla bu bağlantıyı tamamlar.

`Thorn in your side`, `Blast from the Past`, `Firewall` ve yanan pantolon açıklaması gibi diğer esprilerde de aynı yaklaşımı kullanın. Örneğin `Diken Üstünde` kaktüsle bağlantıyı korur; `Patlayan Hatıralar` patlama ve geçmiş temasını birleştirir; `Alevli Güvenlik Duvarı` hem bilgisayar terimini hem lavı çağrıştırır. İngilizce özgün metin referans olarak korunur.

- `item.deathsareitems.bedrock` ve `block.deathsareitems.bedrock` değerlerini her dilde aynı tutun. Seçim sandığı da eşyanın adını kullandığından seçim, envanter ve blok adı tutarlı kalır.
- Açıklamayı adla birlikte uyarlayın; espri, eşyanın gerçek davranışıyla uyumlu olmalıdır.
- Yalnızca dil dosyasındaki değerleri değiştirin. Eşya kimlikleri, komut parametreleri, çeviri anahtarları ve hangi seçeneğin ödül verdiği dilden bağımsızdır.
- Yeni dil eklerken bütün anahtarların bulunduğunu, JSON içinde yinelenen anahtar olmadığını ve yer tutucuların korunduğunu doğrulayın. Özellikle yatak ve açlık gruplarını üç seçenek bir arada okuyarak kontrol edin.

Cheat komutlarının özel başarı ve hata yanıtları kaldırılmıştır; bunlar için çeviri girdisi yoktur. Minecraft'ın kendi komut sözdizimi/yetki hataları ve normal ölüm mesajları kendi sistemleri üzerinden çalışır.
