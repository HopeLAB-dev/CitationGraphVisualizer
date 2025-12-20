GEMINI.md - Makale Graf Analiz Uygulamasi Sistem Promptu
Sana profesyonel bir yapay zeka yardımcısı olarak, Kocaeli Üniversitesi Bilgisayar Mühendisliği bölümü için Makale Graf Analiz Uygulaması projesini baştan sona geliştirme görevi veriyorum. Bu prompt, projenin tüm gereksinimlerini, adım adım yapılacakları ve teknik detayları içerir.
📋 PROJE GENEL BİLGİLERİ
Proje Adı: Makale Graf Analiz Uygulaması
Teslim Tarihi: 29.12.2025
Programlama Dili: C++, C# veya Java (kullanıcı seçecek)
Kısıtlamalar: Görselleştirme dışında HAZIR KÜTÜPHANE KULLANILMAYACAK
Projenin Amacı
Bilimsel makaleler arasındaki atıf ilişkilerini yönlü graf yapısında modellemek, graf analiz metriklerini hesaplamak ve sonuçları görsel olarak sunmak.

🎯 ANA HEDEFLER

✅ Graf yapılarını sıfırdan kodlayarak modellemek
✅ H-index, Betweenness Centrality, K-core Decomposition algoritmalarını uygulamak
✅ İnteraktif graf görselleştirme yapmak
✅ JSON dosyasından veri okuma ve işleme
✅ Kullanıcı dostu arayüz geliştirmek


📊 VERİ YAPISI VE MODELLEME
JSON Veri Formatı
json{
  "id": "W2022157187",
  "authors": ["Author Name"],
  "title": "Article Title",
  "year": 2020,
  "referenced_works": ["W123456", "W789012", ...]
}
```

### Graf Veri Yapısı Gereksinimleri

**Düğüm (Node) Sınıfı İçermeli:**
- `string id` - Makale benzersiz kimliği
- `vector<string> authors` - Yazar listesi
- `string title` - Makale başlığı
- `int year` - Yayın yılı
- `vector<Node*> outgoingEdges` - Verdiği referanslar (giden kenarlar)
- `vector<Node*> incomingEdges` - Aldığı referanslar (gelen kenarlar)
- `int citationCount` - Aldığı toplam atıf sayısı

**Graf (Graph) Sınıfı İçermeli:**
- `map<string, Node*> nodes` - ID ile düğüm eşleştirmesi
- `int totalNodes` - Toplam düğüm sayısı
- `int totalEdges` - Toplam kenar sayısı
- Ekleme, silme, arama fonksiyonları

---

## 🔨 ADIM ADIM GELİŞTİRME PLANI

## AŞAMA 1: TEMEL ALTYAPI (Mandatory - Kritik)

### 1.1. Proje Dosya Yapısını Oluştur
```
project/
├── src/
│   ├── main.cpp/java/cs
│   ├── Graph.cpp/java/cs
│   ├── Node.cpp/java/cs
│   ├── HIndex.cpp/java/cs
│   ├── GraphMetrics.cpp/java/cs
│   ├── Visualization.cpp/java/cs
│   └── JSONParser.cpp/java/cs
├── include/
│   ├── Graph.h
│   ├── Node.h
│   └── ...
├── data/
│   └── articles.json
└── README.md
1.2. JSON Parser Geliştir
Görevler:

 JSON dosyasını satır satır oku
 Her makale için id, authors, title, year, referenced_works alanlarını parse et
 Hata kontrolü yap (eksik alan, geçersiz format)
 Parse edilen verileri Node nesnelerine dönüştür

Kritik Notlar:

Hazır JSON kütüphanesi KULLANAMAZSIN - kendi parser'ını yaz
String manipülasyonu ile { } [ ] " karakterlerini manuel işle
Nested yapıları (authors array) düzgün parse et

1.3. Graf Veri Yapısını Kodla
Node Sınıfı:
cppclass Node {
public:
    string id;
    vector<string> authors;
    string title;
    int year;
    vector<Node*> referencedWorks;  // Giden kenarlar
    vector<Node*> citedBy;          // Gelen kenarlar
    
    // Constructor
    Node(string id, vector<string> authors, string title, int year);
    
    // Referans ekleme
    void addReference(Node* targetNode);
    
    // Atıf sayısını hesapla
    int getCitationCount();
};
Graph Sınıfı:
cppclass Graph {
private:
    map<string, Node*> nodes;
    
public:
    // Düğüm ekleme
    void addNode(Node* node);
    
    // Kenar ekleme
    void addEdge(string fromId, string toId);
    
    // Düğüm getirme
    Node* getNode(string id);
    
    // İstatistikler
    int getTotalNodes();
    int getTotalEdges();
    int getTotalOutgoingEdges();
    int getTotalIncomingEdges();
    pair<string, int> getMostCitedArticle();
    pair<string, int> getMostReferencingArticle();
};
```

---

## AŞAMA 2: GRAF OLUŞTURMA (Adım 2.1)

### 2.1. JSON'dan Graf Oluşturma

**Algoritma:**
```
1. JSON dosyasını aç ve tüm makaleleri oku
2. İLK GEÇİŞ: Her makale için Node nesnesi oluştur ve Graph'a ekle
   - Sadece id, authors, title, year bilgilerini set et
   - referenced_works'ü henüz işleme
3. İKİNCİ GEÇİŞ: Her makale için referenced_works array'ini işle
   - Her referans için addEdge() çağır
   - Hem fromNode'un outgoing'ine ekle
   - Hem toNode'un incoming'ine ekle
4. Her düğüm için citationCount'u hesapla (incoming edges sayısı)
```

### 2.2. İstatistik Hesaplama

**Hesaplanacaklar:**
- [ ] Toplam makale (düğüm) sayısı: `graph.getTotalNodes()`
- [ ] Toplam referans (kenar) sayısı: Tüm kenarlar
- [ ] Toplam verilen referans sayısı: Tüm outgoing edges
- [ ] Toplam alınan referans sayısı: Tüm incoming edges
- [ ] En çok referans alan makale: Max citationCount bulma
- [ ] En çok referans veren makale: Max outgoing edges bulma

### 2.3. Arayüzde Gösterim

**Görsel Bileşenler:**
1. Graf çizim alanı (Canvas/Panel)
2. İstatistik paneli (solda veya üstte)
3. Kullanıcı giriş alanları

**İstatistik Paneli İçeriği:**
```
══════════════════════════════════
         GRAF İSTATİSTİKLERİ
══════════════════════════════════
Toplam Makale: 1234
Toplam Referans: 5678
Verilen Referans: 5678
Alınan Referans: 5678
─────────────────────────────────
En Çok Atıf Alan:
  ID: W2022157187
  Atıf: 45
─────────────────────────────────
En Çok Referans Veren:
  ID: W2023456789
  Referans: 38
══════════════════════════════════
```

---

## AŞAMA 3: H-INDEX HESAPLAMA (Adım 2.2)

### 3.1. H-Index Algoritması

**Tanım:** Bir makaleye atıf yapan makaleler içinde en az h atıfa sahip minimum h makale bulunan en büyük h sayısı.

**Algoritma:**
```
FUNCTION calculateHIndex(Node* article):
    1. citingArticles = article.citedBy listesini al
    2. Eğer citingArticles boş ise return 0
    
    3. Her citing article için kendi citationCount'unu al
       citationCounts = []
       FOR each citingArticle in citingArticles:
           citationCounts.add(citingArticle.getCitationCount())
    
    4. citationCounts'u BÜYÜKTEN KÜÇÜĞE sırala
    
    5. h = 0
       FOR i = 0 to citationCounts.length - 1:
           IF citationCounts[i] >= (i + 1):
               h = i + 1
           ELSE:
               BREAK
    
    6. return h
```

**Örnek:**
```
Makale A'ya atıf yapan makaleler: B, C, D, E, F
Bu makalelerin atıf sayıları: [17, 9, 6, 3, 2]

i=0: 17 >= 1 ✓ → h=1
i=1: 9 >= 2 ✓ → h=2
i=2: 6 >= 3 ✓ → h=3
i=3: 3 >= 4 ✗ → BREAK

H-Index = 3
```

### 3.2. H-Core Bulma

**Algoritma:**
```
FUNCTION getHCore(Node* article, int hIndex):
    1. citingArticles = article.citedBy listesini al
    2. Her article için (article, citationCount) pair oluştur
    3. citationCount'a göre BÜYÜKTEN KÜÇÜĞE sırala
    4. İlk hIndex kadar makaleyi döndür
    
    return citingArticles[0 ... hIndex-1]
```

### 3.3. H-Median Hesaplama

**Algoritma:**
```
FUNCTION calculateHMedian(vector<Node*> hCore):
    1. hCore'daki her makale için citationCount al
    2. Sayıları sırala
    3. Medyan hesapla:
       - Eğer hCore.size() TEK ise:
         median = citationCounts[hCore.size() / 2]
       - Eğer hCore.size() ÇİFT ise:
         median = (citationCounts[n/2 - 1] + citationCounts[n/2]) / 2.0
    
    return median
```

### 3.4. İnteraktif Graf Görselleştirme

**Gereksinimler:**

#### A. İlk Görselleştirme
- [ ] Kullanıcıdan makale ID al (text input)
- [ ] H-index, h-median hesapla ve göster
- [ ] H-core makalelerini graf olarak çiz
- [ ] Her düğüm: Daire şeklinde, ID ile etiketli
- [ ] Kenarlar: Oklu çizgiler (yönlü)

#### B. Hover (Mouse Üzerine Gelme) Özelliği
```
Düğüm üzerine gelindiğinde göster:
┌─────────────────────────────┐
│ ID: W2022157187             │
│ Authors: John Doe, Jane...  │
│ Title: Deep Learning in...  │
│ Year: 2020                  │
│ Citations: 23               │
└─────────────────────────────┘
```

#### C. Tıklama (Click) Özelliği

**Algoritma:**
```
FUNCTION onNodeClick(Node* clickedNode):
    1. clickedNode için h-index, h-core, h-median hesapla
    2. Yeni h-core makalelerini belirle
    3. Mevcut graf üzerinde kontrol et:
       - Yeni düğüm ise: Grafa ekle (FARKLI RENK)
       - Zaten varsa: Renk değiştirme
    4. Yeni düğümler ile mevcut düğümler arasındaki referans ilişkilerini kontrol et
    5. Gerekli kenarları ekle
    6. Grafiği yeniden çiz
    7. İstatistikleri güncelle
    8. Tıklanan düğümü VURGULA (özel renk)
```

**Renk Kodlaması:**
- İlk sorgulanan makale: KIRMIZI
- İlk h-core: MAVİ
- 1. tıklama sonrası eklenen: YEŞİL
- 2. tıklama sonrası eklenen: SARI
- 3. tıklama sonrası eklenen: TURUNCU
- ... (renk paleti döngüsü)

#### D. Graf Güncellemesi

**Kritik Kurallar:**
1. Yeni eklenen düğümler ESKİ düğümlerden AYIRT EDİLEBİLMELİ
2. Düğümler arasındaki TÜM referans ilişkileri gösterilmeli
3. Graf her tıklamada ARTAN şekilde büyümeli
4. Önceki düğümler kaybolmamalı

### 3.5. Dinamik İstatistikler

Her tıklama sonrası GÜNCELLENMELI:
- Grafta toplam düğüm sayısı
- Grafta toplam kenar sayısı
- Diğer tüm istatistikler

---

## AŞAMA 4: GRAF ANALİZ METRİKLERİ (Adım 2.3)

### 4.1. Yönlü Grafı Yönsüz Grafa Çevirme

**Algoritma:**
```
FUNCTION convertToUndirected(Graph directedGraph):
    1. Yeni bir undirectedGraph oluştur
    2. Tüm düğümleri kopyala
    3. Her directed edge (A → B) için:
       - Eğer A-B kenarı yoksa ekle
       - Yön önemsiz (A-B == B-A)
    4. Tekrar eden kenarları filtrele
    
    return undirectedGraph
```

### 4.2. Betweenness Centrality

**Tanım:** Bir düğümün diğer tüm düğüm çiftleri arasındaki en kısa yollarda kaç kez bulunduğunu ölçer.

**Algoritma (Brandes Algorithm Basitleştirilmiş):**
```
FUNCTION calculateBetweenness(Graph g):
    1. Her düğüm için betweenness değerini 0 olarak başlat
    
    2. FOR her kaynak düğüm s:
        a. BFS ile s'den tüm düğümlere en kısa yolları bul
        b. Her hedef düğüm t için:
           - s'den t'ye kaç farklı en kısa yol var (sigma)
           - Her ara düğüm v için:
             * v üzerinden geçen yol sayısı = ?
        c. Her ara düğüm için betweenness'ı güncelle
    
    3. return betweenness değerleri
```

**Basit Yaklaşım (Daha Anlaşılır):**
```
FUNCTION calculateBetweennessSimple(Graph g):
    betweenness = {}
    FOR her düğüm v: betweenness[v] = 0
    
    FOR her kaynak düğüm s:
        FOR her hedef düğüm t (s ≠ t):
            # s'den t'ye tüm en kısa yolları bul (BFS)
            shortestPaths = findAllShortestPaths(s, t)
            
            # Her ara düğüm için say
            FOR her path in shortestPaths:
                FOR her düğüm v in path (s ve t hariç):
                    betweenness[v] += 1 / len(shortestPaths)
    
    return betweenness
```

**BFS ile En Kısa Yol Bulma:**
```
FUNCTION BFS(start, end, graph):
    queue = [start]
    visited = {start}
    parent = {start: None}
    
    WHILE queue not empty:
        current = queue.dequeue()
        
        IF current == end:
            BREAK
        
        FOR each neighbor of current:
            IF neighbor not visited:
                visited.add(neighbor)
                parent[neighbor] = current
                queue.enqueue(neighbor)
    
    # Yolu geri izle
    path = []
    current = end
    WHILE current is not None:
        path.prepend(current)
        current = parent[current]
    
    return path
```

### 4.3. K-Core Decomposition

**Tanım:** Her düğümün en az k derecesine sahip olduğu maksimal alt graf.

**Algoritma:**
```
FUNCTION kCoreDecomposition(Graph g, int k):
    1. subgraph = g'nin kopyasını oluştur
    
    2. REPEAT:
        removed = false
        
        FOR her düğüm v in subgraph:
            degree = v'nin komşu sayısı
            
            IF degree < k:
                subgraph'tan v'yi çıkar
                removed = true
        
    3. UNTIL removed == false
    
    4. return subgraph
```

**Detaylı Adımlar:**
```
INPUT: Yönsüz graf G, integer k

1. Mevcut grafın tüm düğümlerini kopyala → workingGraph
2. changed = true

3. WHILE changed:
    changed = false
    nodesToRemove = []
    
    FOR each node in workingGraph:
        degree = count(node.neighbors)
        IF degree < k:
            nodesToRemove.add(node)
    
    IF nodesToRemove not empty:
        FOR each node in nodesToRemove:
            workingGraph.removeNode(node)
        changed = true

4. return workingGraph
```

### 4.4. Sonuçları Görselleştirme

#### Betweenness Centrality Gösterimi
```
═══════════════════════════════════════
     BETWEENNESS CENTRALITY SONUÇLARI
═══════════════════════════════════════
ID               | Betweenness Score
─────────────────┼──────────────────────
W2022157187      | 145.67
W2023456789      | 98.23
W2021234567      | 87.45
...
═══════════════════════════════════════
```

- Betweenness değerlerini düğümlerin renk yoğunluğu veya boyutu ile göster
- En yüksek betweenness: En koyu renk veya en büyük düğüm

#### K-Core Gösterimi
- K-core'a ait düğümleri FARKLI RENK (örn: PEMBE)
- K-core'a ait kenarları KALIN ÇİZGİ ile göster
- K-core dışındaki düğümleri SOLUK renk

---

## 🎨 GÖRSELLEŞTIRME REHBERİ

### Graf Çizim Algoritması (Force-Directed Layout)
```
FUNCTION layoutGraph(nodes, edges):
    1. Her düğüme rastgele (x, y) pozisyonu ata
    2. iterations = 1000
    
    3. FOR i = 1 to iterations:
        # İtme kuvveti (düğümler birbirini iter)
        FOR her düğüm çifti (n1, n2):
            distance = calculateDistance(n1, n2)
            IF distance < minDistance:
                repulsionForce = k / distance
                applyForce(n1, -repulsionForce)
                applyForce(n2, repulsionForce)
        
        # Çekme kuvveti (kenarlar düğümleri çeker)
        FOR her kenar (n1, n2):
            distance = calculateDistance(n1, n2)
            attractionForce = distance * k
            applyForce(n1, attractionForce)
            applyForce(n2, -attractionForce)
        
        # Pozisyonları güncelle
        FOR her düğüm n:
            n.x += n.velocityX * damping
            n.y += n.velocityY * damping
    
    4. return positions
```

### Düğüm Çizimi
```
FUNCTION drawNode(node, x, y, color):
    1. Çember çiz (x, y, radius=20)
    2. Çemberi color ile doldur
    3. ID etiketini çemberin içine yaz
```

### Kenar Çizimi
```
FUNCTION drawEdge(from, to, directed):
    1. Çizgi çiz (from.x, from.y) → (to.x, to.y)
    2. IF directed:
        Ok ucu çiz hedef düğüme yakın
```

### Hover Bilgi Kartı
```
FUNCTION onMouseMove(mouseX, mouseY):
    1. Her düğüm için distance hesapla
    2. IF distance < nodeRadius:
        İnfoBox göster:
        - node.id
        - node.authors.join(", ")
        - node.title
        - node.year
        - node.citationCount
```

---

## 🧪 TEST SENARYOLARI

### Test 1: Graf Oluşturma
```
GİRDİ: 10 makaleli JSON dosyası
BEKLENEN:
- 10 düğüm oluşturulmalı
- Tüm referans kenarları doğru bağlanmalı
- İstatistikler doğru hesaplanmalı
```

### Test 2: H-Index Hesaplama
```
TEST CASE:
Makale A'ya atıf yapan makaleler: [B:17, C:9, D:6, E:3, F:2]

BEKLENEN:
- H-Index = 3
- H-Core = [B, C, D]
- H-Median = 9
```

### Test 3: İnteraktif Genişletme
```
1. Makale X için ilk sorgu → h-core: [A, B, C]
2. A düğümüne tıkla → Yeni h-core: [D, E, F]
3. KONTROL: D, E, F grafa eklendi mi?
4. KONTROL: A-D, B-E gibi ilişkiler var mı?
5. KONTROL: Renkler farklı mı?
```

### Test 4: Betweenness Centrality
```
GİRDİ: 
    A - B - C
    |   |   |
    D - E - F

BEKLENEN:
- B ve E en yüksek betweenness'a sahip (merkezi düğümler)
```

### Test 5: K-Core Decomposition
```
GİRDİ: 
    Graf + k=2

BEKLENEN:
- Derece < 2 olan tüm düğümler çıkarılmalı
- Kalan alt graf her düğümün en az 2 komşusu olmalı
```

---

## ⚠️ KRITIK HATIRLATMALAR

### 1. HAZIR KÜTÜPHANE YASAĞI
```
❌ KULLANILAMAZ:
- Networkx (Python)
- JGraphT (Java)
- Boost Graph Library (C++)
- JSON parsing kütüphaneleri (Jackson, Gson, nlohmann/json)

✅ KULLANILABİLİR:
- Görselleştirme: JavaFX, Swing, Qt, Windows Forms
- STL (C++): vector, map, queue (temel veri yapıları)
- Java Collections: ArrayList, HashMap (temel veri yapıları)
2. Algoritma Gereksinimleri


