<div align="center">

# 🔍 InsiKt

### *Kotlin Logger++ Fork*

[![GitHub release](https://img.shields.io/github/v/release/coreyd97/insikt?style=for-the-badge&logo=github&color=blueviolet)](https://github.com/coreyd97/insikt/releases/latest)
[![License](https://img.shields.io/badge/license-GPL--3.0-blue?style=for-the-badge&logo=open-source-initiative)](LICENSE)
[![Kotlin](https://img.shields.io/badge/Kotlin-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![Burp Suite](https://img.shields.io/badge/Burp_Suite-FF6633?style=for-the-badge&logo=burp-suite&logoColor=white)](https://portswigger.net/burp)

*Now with 99% less memory usage and ~~no~~ less bugs!* ✨

<a href='https://ko-fi.com/G2G6SWLU1' target='_blank'><img height='40' style='border:0px;height:40px;' src='https://storage.ko-fi.com/cdn/kofi6.png?v=6' border='0' alt='Buy Me a Coffee at ko-fi.com' /></a>

**Developed by Corey Arthur**

</div>

---

## 📖 Description

InsiKt is a fork of Logger++ focused on the core features that I personally find useful during my tests. 



In addition to logging requests and responses from all
Burp Suite tools, the extension allows advanced filters to be defined to highlight interesting entries or filter logs to
only those which match the filter.

A built in grep tool allows the logs to be searched to locate entries which match a specified pattern, and extract the
values of the capture groups.

To enable logs to be used in other systems, the table can also be uploaded to elasticsearch.

## ✨ Features

- 📊 **Comprehensive Logging** - Logs all tools sending requests and receiving responses
- 🎯 **Selective Logging** - Ability to log from specific tools
- 🔍 **Custom Regex Support** - Show results of custom regular expressions in request/response
- ⚙️ **Customizable Headers** - Personalize column headers to your preferences
- 🎨 **Advanced Filtering** - Create filters to display only requests matching specific strings or regex patterns
- 🌈 **Row Highlighting** - Make interesting requests pop with filter-based highlighting
- 🔎 **Built-in Grep** - Search through logs efficiently
- ⚡ **Live Monitoring** - Real-time requests and responses
- 👁️ **Multiple Views** - Various display options to suit your workflow
- 🪟 **Pop-out Panels** - Detach view panels for better multitasking
- 🚀 **Multithreaded** - High-performance concurrent processing

## 📸 Screenshots

<details open>
<summary><b>🎯 Log Filters</b></summary>

![Log Filters](images/filters.png)

</details>

<details>
<summary><b>🌈 Row Highlights</b></summary>

![Row Highlights](images/colorfilters.png)

</details>

<details>
<summary><b>🔍 Grep Search</b></summary>

![Grep Panel](images/grep.png)

</details>

---

## 🚀 Usage

You can use this extension without using the BApp store. Follow these steps to install:

### Installation

1. 📥 **Download** the [latest release jar](https://github.com/coreyd97/insikt/releases/latest)

2. 🔧 **Install** in Burp Suite:
   - Click on the **"Extender"** tab
   - Navigate to the **"Extensions"** tab
   - Click **"Add"** and select the downloaded `insikt.jar` file

3. ✅ **Verify** the installation:
   - You should see the **"InsiKt"** tab in Burp Suite
   - If logging doesn't work, check your extension settings
   - If save buttons are disabled, ensure libraries loaded successfully
   - Try unloading and reloading the extension
   - [Report issues](https://github.com/coreyd97/insikt/issues) if problems persist

4. ⚙️ **Configure** the extension:
   - Use the **"Options"** tab for settings
   - Right-click on column headers for customization

5. ⭐ **Show Support** - If you like the project, give the repo a star!

<div align="center">

![Stargazers](https://starchart.cc/coreyd97/insikt.svg)

</div>

---

## 🤝 Contributing

### 🔨 Building from Source

If you'd like to build the project from source, the project uses Gradle to simplify the process:

```bash
# Clone the repository
git clone https://github.com/coreyd97/InsiKt.git

# Build the project
./gradlew jar           # Linux/Mac
gradlew.bat jar         # Windows
```

📦 Once complete, find the built JAR in the project's `releases` folder.

### 🐛 Reporting Bugs

Found an issue? Please report it via [GitHub Issues](https://github.com/coreyd97/insikt/issues/new/choose).

---

<div align="center">

**Made with ❤️ by Corey Arthur (@coreyarthur.com)**

[![GitHub followers](https://img.shields.io/github/followers/coreyd97?style=social)](https://github.com/coreyd97)
[![GitHub stars](https://img.shields.io/github/stars/coreyd97/insikt?style=social)](https://github.com/coreyd97/insikt/stargazers)

</div>
