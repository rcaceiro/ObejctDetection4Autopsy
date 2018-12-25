package pt.ipleiria.object_detection_autopsy.processors;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.charset.Charset;
import java.util.logging.Level;
import org.apache.http.HttpEntity;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import pt.ipleiria.object_detection_autopsy.ObjectDetectionIngestModuleFactory;
import pt.ipleiria.object_detection_autopsy.model.ImageDetection;
import pt.ipleiria.object_detection_autopsy.model.VideoDetection;

public class WebProcessor implements Processor
{
 private final CloseableHttpClient httpClient;
// private final HttpHost host;
 private final RequestConfig pingTimeoutConfig;
 private final RequestConfig fileTimeoutConfig;
 private final String address;
 private final int port;

 public WebProcessor(String address, int port) throws MalformedURLException
 {
//  this.host = new HttpHost(address, port, "http");
  this.httpClient = HttpClients.createDefault();
  this.pingTimeoutConfig = RequestConfig.custom()
    .setConnectionRequestTimeout(10000)
    .setConnectTimeout(10000)
    .setSocketTimeout(10000)
    .build();
  
  this.fileTimeoutConfig = RequestConfig.custom()
    .setConnectionRequestTimeout(0)
    .setConnectTimeout(0)
    .setSocketTimeout(0)
    .build();
  this.address = address;
  this.port = port;
 }

 @Override
 public ImageDetection processImage(File file) throws IllegalArgumentException, ClientProtocolException, IOException
 {
  String jsonString;
  try (CloseableHttpResponse closeableHttpClient = this.sendFile(RequestsPaths.PROCESS_IMAGE, file))
  {
   jsonString = this.getJsonString(closeableHttpClient);
  }
  ObjectDetectionIngestModuleFactory.ObjectDetectionLogger.log(Level.INFO, "file {0} detections: {1}", new Object[]
  {
   file.getName(), jsonString
  });
  return null;
 }

 @Override
 public VideoDetection processVideo(File file) throws IllegalArgumentException, ClientProtocolException, IOException
 {
  String jsonString;
  try (CloseableHttpResponse closeableHttpClient = this.sendFile(RequestsPaths.PROCESS_VIDEO, file))
  {
   jsonString = this.getJsonString(closeableHttpClient);
  }
  ObjectDetectionIngestModuleFactory.ObjectDetectionLogger.log(Level.INFO, "file {0} detections: {1}", new Object[]
  {
   file.getName(), jsonString
  });
  return null;
 }

 @Override
 public boolean isProcessorAvailable() throws IllegalArgumentException, ClientProtocolException, IOException
 {
//  HttpRequest request = new BasicHttpRequest("GET",RequestsPaths.PING);
//  request.setHeader("Accept", "application/json");
  
  HttpGet request = new HttpGet("http://" + this.address + ":" + this.port + RequestsPaths.PING);
  request.addHeader("Accept", "application/json");
  request.setConfig(this.pingTimeoutConfig);
  CloseableHttpResponse closableHttpResponse = this.httpClient.execute(request);
  int responseCode = closableHttpResponse.getStatusLine().getStatusCode();
  return responseCode >= 200 && responseCode < 300;
 }

 @Override
 public void close()
 {
  try
  {
   this.httpClient.close();
  }
  catch (IOException ex)
  {
   ObjectDetectionIngestModuleFactory.ObjectDetectionLogger.log(Level.SEVERE, ex.getLocalizedMessage(), ex);
  }
 }

 private CloseableHttpResponse sendFile(String endpoint, File file) throws IllegalArgumentException, ClientProtocolException, IOException
 {
  HttpPost postRequest = new HttpPost("http://" + this.address + ":" + this.port + endpoint);
  postRequest.setConfig(this.fileTimeoutConfig);
  FileBody fileBody = new FileBody(file);
  MultipartEntityBuilder multipartEntityBuilder = MultipartEntityBuilder.create();
  multipartEntityBuilder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
  multipartEntityBuilder.addPart("file", fileBody);
  HttpEntity entity = multipartEntityBuilder.build();
  postRequest.setEntity(entity);
  return this.httpClient.execute(postRequest);
 }

 private String getJsonString(CloseableHttpResponse closeableHttpResponse) throws IOException
 {
  if (closeableHttpResponse.getStatusLine().getStatusCode() != 200)
  {
   return null;
  }
  HttpEntity httpEntity = closeableHttpResponse.getEntity();
  String response = null;
  try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream())
  {
   httpEntity.writeTo(byteArrayOutputStream);
   response = new String(byteArrayOutputStream.toByteArray(), Charset.forName("UTF-8"));
  }
  return response;
 }

 private class RequestsPaths
 {
  private final static String PING = "/ping";
  private final static String PROCESS_IMAGE = "/process/image";
  private final static String PROCESS_VIDEO = "/process/video";
 }
}
