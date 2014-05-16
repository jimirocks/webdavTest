import com.gooddata.commons.http.ExtendedHttpClientParams;
import com.gooddata.commons.http.auth.GdcAuthenticator;
import com.gooddata.restapi.upload.webdav.SstWebDavUploader;
import com.gooddata.restapi.upload.webdav.WebDavUploader;
import com.gooddata.restapi.upload.webdav.WebDavUploaderException;
import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.URI;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.lang.RandomStringUtils;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * TODO
 */
public class WebdavTest {


    public static void main(String... args) throws IOException, InterruptedException {

        String host = "staging2.getgooddata.com";
        String stgHost = "https://na1-staging2-di.getgooddata.com";
        String sst = "ovQpH5CJehJtK2GF";

        if (args.length == 3) {
            host = args[0];
            stgHost = args[1];
            sst = args[2];
        } else if (args.length > 0) {
            System.out.println("give me three params: gdcHost, webdavHost, sst");
            System.exit(1);
        }

        final GdcAuthenticator gdcAuthenticator = new GdcAuthenticator(new HttpClient(), "https", host, 443);
        ExtendedHttpClientParams params = new ExtendedHttpClientParams();
        params.setAuthenticationPreemptive(false);
        HttpClient httpClient = new HttpClient(params);
        HostConfiguration hostConfiguration = new HostConfiguration();
        hostConfiguration.setHost(new URI(stgHost));
        httpClient.setHostConfiguration(hostConfiguration);
        SstWebDavUploader uploader = new SstWebDavUploader(httpClient, gdcAuthenticator, sst/*"YnojnNILOUeaLhUv"*/);

        String baseDir = RandomStringUtils.randomAlphabetic(4);
        uploader.createDirectory(baseDir);
        ExecutorService executorService = Executors.newFixedThreadPool(3);
        executorService.submit(new FileUploader(baseDir, uploader, httpClient));
        Thread.sleep(300);
        executorService.submit(new FileUploader(baseDir, uploader, httpClient));
        Thread.sleep(300);
        executorService.submit(new FileUploader(baseDir, uploader, httpClient));

    }

    private static class FileUploader implements Runnable {
        private String baseDir;
        private WebDavUploader webDavUploader;
        private HttpClient httpClient;

        private FileUploader(String baseDir, WebDavUploader webDavUploader, HttpClient httpClient) {
            this.baseDir = baseDir;
            this.webDavUploader = webDavUploader;
            this.httpClient = httpClient;
        }

        @Override
        public void run() {
            while (true) {
                String dir = baseDir + "/" + RandomStringUtils.randomAlphabetic(4) + "/" + RandomStringUtils.randomAlphabetic(4);
                try {
                    try {
                        webDavUploader.transferInputStream(new ByteArrayInputStream("webdavtestblabla".getBytes()), dir, "test.txt", "text/plain");
                    } catch (WebDavUploaderException e) {
                        e.printStackTrace();
                        int status = httpClient.executeMethod(new GetMethod("/uploads/" + dir + "/test.txt"));
                        System.out.println("/uploads/" + dir + "/test.txt put failed, following get status=" + status);
                        System.exit(6);
                    }
                    int status = httpClient.executeMethod(new GetMethod("/uploads/" + dir + "/test.txt"));
                    if (status != 200 && status != 401) {
                        System.out.println("/uploads/" + dir + "/test.txt is not there, status=" + status);
                        System.exit(5);
                    }
                } catch (HttpException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
