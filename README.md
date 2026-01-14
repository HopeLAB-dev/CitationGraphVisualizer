# 📊 Citation Graph Visualizer

![Java](https://img.shields.io/badge/Java-21%2B-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![Gradle](https://img.shields.io/badge/Gradle-8.0%2B-02303A?style=for-the-badge&logo=gradle&logoColor=white)
![License](https://img.shields.io/badge/License-MIT-green.svg?style=for-the-badge)

**Citation Graph Visualizer**, akademik makaleler arasındaki atıf (citation) ağlarını analiz etmek ve görselleştirmek için geliştirilmiş, JavaFX tabanlı bir masaüstü uygulamasıdır. OpenAlex veri formatını destekler ve makaleler arasındaki karmaşık ilişkileri anlamlandırmak için gelişmiş grafik algoritmaları kullanır.

## 🌟 Özellikler

* **İnteraktif Görselleştirme:** Binlerce makale ve atıf bağını (node & edge) performanslı bir şekilde çizer.
* **Dinamik Etkileşim:** Zoom (yakınlaştırma), Pan (kaydırma) ve sürükle-bırak desteği.
* **Gelişmiş Analizler:**
  * **H-Index Hesaplama:** Seçilen makalenin ve yazarın H-Index değerini anlık hesaplar.
  * **K-Core Decomposition:** Ağdaki en yoğun bağlı çekirdek grupları filtreler.
  * **Betweenness Centrality:** Ağdaki bilgi akışında kilit rol oynayan "köprü" makaleleri tespit eder.
* **Detaylı Bilgi Paneli:** Seçilen makaleye dair yazar, yıl ve atıf istatistiklerini görüntüler.

## 🛠️ Teknolojiler

Bu proje aşağıdaki teknolojiler kullanılarak geliştirilmiştir:

* **Dil:** Java 21
* **Arayüz:** JavaFX
* **Build Aracı:** Gradle (Kotlin DSL)
* **Veri Yapıları:** Graph Theory (Adjacency Lists), BFS/DFS Algoritmaları

## 🚀 Kurulum ve Çalıştırma

Projeyi yerel makinenizde çalıştırmak için aşağıdaki adımları izleyin.

### Gereksinimler

* **Java JDK 21** veya üzeri (Mutlaka kurulu olmalıdır)
* **Git**

### 1. Projeyi İndirin

Terminal veya Komut İstemcisi'ni açarak projeyi klonlayın:

```bash
git clone https://github.com/KULLANICI_ADINIZ/Prolab-3.git
cd Prolab-3
```

### 2. İşletim Sistemine Göre Başlatın

Proje **Gradle** Wrapper içerdiğinden, ayrıca Gradle kurmanıza gerek yoktur. İşletim sisteminize uygun komutu kullanın:

#### 🐧 Linux & 🍎 macOS

Terminalde şu komutları sırasıyla çalıştırın:

```bash
# Çalıştırma izni verin
chmod +x gradlew

# Uygulamayı başlatın
./gradlew run
```

#### 🪟 Windows

CMD veya PowerShell üzerinden şu komutu çalıştırın:

```cmd
gradlew.bat run
```

> **Not:** İlk çalıştırmada Gradle gerekli bağımlılıkları indireceği için işlem biraz zaman alabilir.

## 📖 Kullanım

1. Uygulama açıldığında **"JSON Dosyası Seç"** butonuna tıklayın.
2. Bilgisayarınızdaki uygun formatlı (OpenAlex yapısında) makale veri setini (`.json`) seçin.
3. Grafik yüklendiğinde:
    * **Sol Tık:** Bir makaleyi seçer ve detaylarını gösterir.
    * **Sağ Tık (Sürükle):** Grafiği kaydırır (Pan).
    * **Mouse Tekerleği:** Yakınlaştırır/Uzaklaştırır (Zoom).
    * **Analiz Modları:** Arayüzdeki butonları kullanarak K-Core veya Betweenness analizlerini çalıştırabilirsiniz.

## 📂 Proje Yapısı

```text
src/main/java/com/hopelab/graph
├── model/       # Veri modelleri (Article, CitationGraph)
├── service/     # İş mantığı ve algoritmalar (Metrics, Loader)
└── ui/          # JavaFX arayüz bileşenleri (View, Panel)
```

## 🤝 Katkıda Bulunma

Katkılarınızı bekliyoruz! Lütfen önce bir issue açarak tartışın, ardından Pull Request gönderin.

1. Bu depoyu Fork'layın.
2. Yeni bir özellik dalı (feature branch) oluşturun (`git checkout -b feature/YeniOzellik`).
3. Değişikliklerinizi commit edin (`git commit -m 'Yeni özellik eklendi'`).
4. Dalınızı Push edin (`git push origin feature/YeniOzellik`).
5. Bir Pull Request oluşturun.

## 📄 Lisans

Bu proje MIT Lisansı altında lisanslanmıştır. Detaylar için `LICENSE` dosyasına bakabilirsiniz.
