# 新聞直播

在 Android TV 上透過網路觀看新聞直播

## 下載

https://github.com/anenasa/androidtv-news/releases

## 特色

使用 [youtubedl-android](https://github.com/yausername/youtubedl-android) 取得影片網址，無須安裝 Youtube 應用程式

使用遙控器轉台

可自訂頻道

## 用法

1. 轉台：使用上/下或頻道上/下轉台
2. 選擇頻道：輸入數字後按 OK
3. 顯示頻道表：按 OK

## 自訂頻道表

以 json 格式自訂頻道，可參考[預設頻道表](https://github.com/anenasa/androidtv-news/blob/main/app/src/main/res/raw/config.txt)。

自訂頻道檔案放在 /storage/emulated/0/Android/data/io.github.anenasa.news/files/config.txt。格式是：

    {
      "channelList": [
        頻道1,
        頻道2,
        ...
      ]
    }

頻道格式：

    {
      "url": "頻道網址",
      "name": "頻道名稱",
      "ytdl-format": "youtube-dl 格式",
      "volume": 音量
    }

youtube-dl 格式請參考[這裡](https://github.com/ytdl-org/youtube-dl/blob/master/README.md#format-selection)。

## 許可證
[GNU General Public License v3.0](https://github.com/anenasa/androidtv-news/blob/main/LICENSE)
