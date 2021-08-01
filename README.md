# 新聞直播

在 Android TV 上透過網路觀看新聞直播

## 下載

https://github.com/anenasa/androidtv-news/releases

## 特色

使用 [youtubedl-android](https://github.com/yausername/youtubedl-android) 取得影片網址，無須安裝 Youtube 應用程式

使用遙控器轉台

可自訂頻道

## 用法

### 主畫面
1. 轉台：使用上/下鍵或頻道上/下鍵轉台
2. 選擇頻道：輸入數字後按 OK
3. 顯示選單：按 OK，選單中可選擇顯示頻道表、頻道資訊、新增頻道和設定

### 頻道資訊

可顯示頻道的一些資訊，也能修改頻道的名稱、網址、youtube-dl 格式、音量和 header，這裡的設定會覆蓋頻道清單檔案裡的設定。如果要回復到頻道清單檔案的設定，只要把要回復的屬性清空並儲存即可。

## 預設頻道

預設頻道條件：
1. 必須是台灣的節目
2. 必須是新聞為主
3. 來源必須合法
4. youtube-dl 要支援
5. 無須設定 header

另外提供[完整頻道清單檔案](https://anenasa.github.io/channel/full.txt)，不限制是新聞和台灣頻道，可使用自訂頻道方式二觀看。

如果有頻道建議歡迎提出。

## 自訂頻道

以 json 格式自訂頻道，可參考[預設頻道檔案](https://anenasa.github.io/channel/config.txt)。

讀取優先順序為 config.txt > url.txt > 預設頻道。

### 方法一
將自訂頻道檔案放在 /storage/emulated/0/Android/data/io.github.anenasa.news/files/config.txt。

### 方式二
自訂頻道檔案放在網路上，再將檔案的網址放在 /storage/emulated/0/Android/data/io.github.anenasa.news/files/url.txt 內。

### 自訂頻道檔案格式

頻道檔案如下（最後一個頻道後面不能加逗點，其他頻道都要）：

    {
      "channelList": [
        頻道1,
        頻道2,
        ...
      ]
    }

頻道格式（最後一個選項後面不能加逗點，其他選項都要）：

    {
      "url": "頻道網址",
      "name": "頻道名稱",
      "ytdl-format": "youtube-dl 格式（可省略）",
      "volume": 音量（可省略）,
      "header": "name: value（可省略）"
    }

youtube-dl 格式請參考[這裡](https://github.com/ytdl-org/youtube-dl/blob/master/README.md#format-selection)。
如果要設定多個 header 可使用 \r\n 分隔（因為反斜線在 json 格式中是特殊字元，所以要用 \\r\\n 分隔）

## 許可證
[GNU General Public License v3.0](https://github.com/anenasa/androidtv-news/blob/main/LICENSE)
