package com.facebook.presto.elasticsearch5;

import com.facebook.presto.elasticsearch.BaseClient;
import com.facebook.presto.elasticsearch.conf.ElasticsearchConfig;
import com.facebook.presto.spi.PrestoException;
import com.google.inject.Binder;
import com.google.inject.Inject;
import com.google.inject.Module;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.transport.client.PreBuiltTransportClient;

import javax.inject.Provider;

import java.io.IOException;
import java.net.InetAddress;

import static com.facebook.presto.elasticsearch.ElasticsearchErrorCode.UNEXPECTED_ES_ERROR;
import static java.util.Objects.requireNonNull;

public class Elasticsearch5Module
        implements Module
{
    @Override
    public void configure(Binder binder)
    {
//        configBinder(binder).bindConfig(ElasticsearchConfig.class);

        binder.bind(BaseClient.class).to(Elasticsearch5Client.class);

//        binder.bind(ElasticsearchTableProperties.class).in(Scopes.SINGLETON);
//        binder.bind(ElasticsearchSessionProperties.class).in(Scopes.SINGLETON);

        binder.bind(Client.class).toProvider(ConnectionProvider.class);
    }

    private static class ConnectionProvider
            implements Provider<Client>
    {
        private final String clusterName;
        private final String hosts;

        @Inject
        public ConnectionProvider(ElasticsearchConfig config)
        {
            requireNonNull(config, "config is null");
            this.clusterName = config.getClusterName();
            this.hosts = config.getElasticsearchHosts();
        }

        @Override
        public Client get()
        {
            try {
                Settings settings = Settings.builder().put("cluster.name", clusterName)
                        .put("client.transport.sniff", true).build();

                TransportClient client = new PreBuiltTransportClient(settings);
                for (String ip : hosts.split(",")) {
                    client.addTransportAddress(
                            new InetSocketTransportAddress(InetAddress.getByName(ip.split(":")[0]),
                                    Integer.parseInt(ip.split(":")[1])));
                }
                return client;
            }
            catch (IOException e) {
                throw new PrestoException(UNEXPECTED_ES_ERROR, "Failed to get connection to Elasticsearch", e);
            }
        }
    }
}
