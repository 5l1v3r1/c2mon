package cern.c2mon.server.elasticsearch.util;

import cern.c2mon.server.elasticsearch.config.ElasticsearchProperties;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.impl.client.HttpClientBuilder;

import java.io.IOException;

public class IndexUtils {
    public static boolean doesIndexExist(String indexName, ElasticsearchProperties properties) throws IOException {
        HttpHead httpRequest = new HttpHead(("http://" + properties.getHost() + ":" + properties.getHttpPort() + "/" + indexName));
        HttpClient httpClient = HttpClientBuilder.create().build();
        HttpResponse httpResponse = httpClient.execute(httpRequest);
        return httpResponse.getStatusLine().getStatusCode() == HttpStatus.SC_OK;
    }
}
