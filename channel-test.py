#!/usr/bin/env python3
from __future__ import unicode_literals
import sys
import json
import yt_dlp

class MyLogger(object):
    def debug(self, msg):
        pass

    def warning(self, msg):
        pass

    def error(self, msg):
        print(msg)


chnnelFile = open(sys.argv[1])
channelList = chnnelFile.read()
jsonObject = json.loads(channelList)
ydl_opts = {'logger': MyLogger()}
with yt_dlp.YoutubeDL(ydl_opts) as ydl:
    for item in jsonObject["channelList"]:
        try:
            info = ydl.extract_info(item["url"], download = False)
        except:
            print(item["url"])
