package com.google.api.services.samples.drive.cmdline;

import com.google.api.client.googleapis.media.MediaHttpDownloader;
import com.google.api.client.googleapis.media.MediaHttpDownloaderProgressListener;

public class FileDownloadProgressListener
  implements MediaHttpDownloaderProgressListener
{
  public void progressChanged(MediaHttpDownloader downloader)
  {
    switch (downloader.getDownloadState())
    {
    case MEDIA_IN_PROGRESS: 
      View.header2("Download is in progress: " + downloader.getProgress());
      break;
    case NOT_STARTED: 
      View.header2("Download is Complete!");
    }
  }
}
