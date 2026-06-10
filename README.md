# 📊 Citation Graph Visualizer

![Java](https://img.shields.io/badge/Java-21%2B-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![Gradle](https://img.shields.io/badge/Gradle-8.0%2B-02303A?style=for-the-badge&logo=gradle&logoColor=white)
![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg?style=for-the-badge)

[🇹🇷 Türkçe](#-türkçe) | [🇬🇧 English](#-english)

---

<a name="-türkçe"></a>
## 🇹🇷 Türkçe

**Citation Graph Visualizer**, akademik makaleler arasındaki atıf (citation) ağlarını analiz etmek ve görselleştirmek için geliştirilmiş, JavaFX tabanlı bir masaüstü uygulamasıdır. OpenAlex veri formatını destekler ve makaleler arasındaki karmaşık ilişkileri anlamlandırmak için gelişmiş grafik algoritmaları kullanır.

### 🌟 Özellikler

* **İnteraktif Görselleştirme:** Binlerce makale ve atıf bağını (node & edge) performanslı bir şekilde çizer.
* **Dinamik Etkileşim:** Zoom (yakınlaştırma), Pan (kaydırma) ve sürükle-bırak desteği.
* **Gelişmiş Analizler:**
  * **H-Index Hesaplama:** Seçilen makalenin ve yazarın H-Index değerini anlık hesaplar.
  * **K-Core Decomposition:** Ağdaki en yoğun bağlı çekirdek grupları filtreler.
  * **Betweenness Centrality:** Ağdaki bilgi akışında kilit rol oynayan "köprü" makaleleri tespit eder.
* **Detaylı Bilgi Paneli:** Seçilen makaleye dair yazar, yıl ve atıf istatistiklerini görüntüler.

### 🛠️ Teknolojiler

Bu proje aşağıdaki teknolojiler kullanılarak geliştirilmiştir:
* **Dil:** Java 21
* **Arayüz:** JavaFX
* **Build Aracı:** Gradle (Kotlin DSL)
* **Veri Yapıları:** Graph Theory (Adjacency Lists), BFS/DFS Algoritmaları

### 🚀 Kurulum ve Çalıştırma

Projeyi yerel makinenizde çalıştırmak için aşağıdaki adımları izleyin.

#### Gereksinimler
* **Java JDK 21** veya üzeri (Mutlaka kurulu olmalıdır)
* **Git**

#### 1. Projeyi İndirin
Terminal veya Komut İstemcisi'ni açarak projeyi klonlayın:
```bash
git clone https://github.com/HopeLAB-dev/Prolab-3.git
cd Prolab-3
```

#### 2. İşletim Sistemine Göre Başlatın
Proje **Gradle** Wrapper içerdiğinden, ayrıca Gradle kurmanıza gerek yoktur. İşletim sisteminize uygun komutu kullanın:

**🐧 Linux & 🍎 macOS**
```bash
chmod +x gradlew
./gradlew run
```

**🪟 Windows**
```cmd
gradlew.bat run
```
> **Not:** İlk çalıştırmada Gradle gerekli bağımlılıkları indireceği için işlem biraz zaman alabilir.

### 📖 Kullanım
1. Uygulama açıldığında **"JSON Dosyası Seç"** butonuna tıklayın.
2. Bilgisayarınızdaki uygun formatlı (OpenAlex yapısında) makale veri setini (`.json`) seçin.
3. Grafik yüklendiğinde:
    * **Sol Tık:** Bir makaleyi seçer ve detaylarını gösterir.
    * **Sağ Tık (Sürükle):** Grafiği kaydırır (Pan).
    * **Mouse Tekerleği:** Yakınlaştırır/Uzaklaştırır (Zoom).
    * **Analiz Modları:** Arayüzdeki butonları kullanarak K-Core veya Betweenness analizlerini çalıştırabilirsiniz.

---

<a name="-english"></a>
## 🇬🇧 English

**Citation Graph Visualizer** is a JavaFX-based desktop application developed to analyze and visualize citation networks between academic papers. It supports the OpenAlex data format and uses advanced graph algorithms to make sense of complex relationships between articles.

### 🌟 Features

* **Interactive Visualization:** Performantly renders thousands of articles and citation links (nodes & edges).
* **Dynamic Interaction:** Supports Zooming, Panning, and Drag & Drop.
* **Advanced Analytics:**
  * **H-Index Calculation:** Instantly calculates the H-Index value of the selected article and author.
  * **K-Core Decomposition:** Filters the most densely connected core groups in the network.
  * **Betweenness Centrality:** Identifies "bridge" articles that play a key role in information flow within the network.
* **Detailed Info Panel:** Displays author, year, and citation statistics for the selected article.

### 🛠️ Technologies

This project is built using the following technologies:
* **Language:** Java 21
* **UI:** JavaFX
* **Build Tool:** Gradle (Kotlin DSL)
* **Data Structures:** Graph Theory (Adjacency Lists), BFS/DFS Algorithms

### 🚀 Installation & Run

Follow the steps below to run the project on your local machine.

#### Requirements
* **Java JDK 21** or higher (Must be installed)
* **Git**

#### 1. Clone the Project
Open Terminal or Command Prompt and clone the repository:
```bash
git clone https://github.com/HopeLAB-dev/Prolab-3.git
cd Prolab-3
```

#### 2. Start Based on OS
Since the project includes **Gradle** Wrapper, you don't need to install Gradle separately. Use the command appropriate for your operating system:

**🐧 Linux & 🍎 macOS**
```bash
chmod +x gradlew
./gradlew run
```

**🪟 Windows**
```cmd
gradlew.bat run
```
> **Note:** The first run may take some time as Gradle downloads necessary dependencies.

### 📖 Usage
1. When the application opens, click the **"Select JSON File"** button.
2. Select a suitable article dataset (`.json`) in OpenAlex format from your computer.
3. Once the graph loads:
    * **Left Click:** Selects an article and shows details.
    * **Right Click (Drag):** Pans the graph.
    * **Mouse Wheel:** Zooms in/out.
    * **Analysis Modes:** You can run K-Core or Betweenness analyses using the buttons on the interface.

---

## 📄 License

This project is licensed under the GNU GPLv3 License. See the [LICENSE](LICENSE) file for details.