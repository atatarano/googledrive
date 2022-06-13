package com.google.api.services.samples.drive.cmdline;

import com.google.api.client.googleapis.media.MediaHttpUploader;
import com.google.api.client.googleapis.media.MediaHttpUploaderProgressListener;
import java.io.IOException;
import java.text.NumberFormat;

public class FileUploadProgressListener
  implements MediaHttpUploaderProgressListener
{
  public void progressChanged(MediaHttpUploader uploader)
    throws IOException
  {
    switch (uploader.getUploadState())
    {
    case INITIATION_STARTED: 
      View.header2("Upload Initiation has started.");
      break;
    case MEDIA_COMPLETE: 
      View.header2("Upload Initiation is Complete.");
      break;
    case MEDIA_IN_PROGRESS: 
      View.header2(
        "Upload is In Progress: " + NumberFormat.getPercentInstance().format(uploader.getProgress()));
      break;
    case NOT_STARTED: 
      View.header2("Upload is Complete!");
    }
  }
}
