# 新聞直播

在 Android 系統電視上透過網路觀看新聞直播

## 下載

https://github.com/anenasa/androidtv-news/releases

## 特色

使用 [yt-dlp](https://github.com/yt-dlp/yt-dlp) 取得影片網址，無須安裝 Youtube 應用程式

使用遙控器轉台

可自訂頻道

## 用法

### 主畫面
1. 轉台：使用上/下鍵或頻道上/下鍵轉台
2. 選擇頻道：輸入數字後按 OK
3. 顯示選單：按 OK，選單中可選擇顯示頻道表、頻道資訊、新增頻道和設定

### 頻道資訊

可顯示頻道的一些資訊，也能修改頻道的名稱、網址、yt-dlp 格式、音量和 header，這裡的設定會覆蓋頻道清單檔案裡的設定。如果要回復到頻道清單檔案的設定，只要把要回復的屬性清空並儲存即可。

## 預設頻道

預設頻道條件：
1. 來源必須合法
2. 台灣能看得到
3. 無須登入

如果有頻道建議歡迎提出。

## 自訂頻道

以 json 格式自訂頻道，將自訂頻道檔案放在 /storage/emulated/0/Android/data/io.github.anenasa.news/files/config.txt，可參考[預設頻道檔案](https://anenasa.github.io/channel/config.txt)。

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
      "ytdl-format": "yt-dlp 格式（可省略）",
      "volume": 音量（可省略）,
      "header": "name: value（可省略）",
      "ytdl-options": JSON 物件（可省略）
    }

頻道也可以是網路上的頻道清單檔案：

    {
      "list": "頻道清單檔案網址"
    }

yt-dlp 格式請參考[這裡](https://github.com/yt-dlp/yt-dlp/blob/master/README.md#format-selection)。預設格式為bv*+ba/b

如果要設定多個 header 可使用 \r\n 分隔（因為反斜線在 json 格式中是特殊字元，所以要用 \\\\r\\\\n 分隔）

ytdl-options 請參考[這裡](https://github.com/yt-dlp/yt-dlp/blob/master/yt_dlp/YoutubeDL.py#L184)。目前只支援字串的選項，如果你想使用的選項不支援歡迎回報。

## 許可證
[GNU General Public License v3.0](https://github.com/anenasa/androidtv-news/blob/main/LICENSE)
