package nl.ulso.curator.server;

import dagger.BindsInstance;
import dagger.Component;
import jakarta.inject.Singleton;
import nl.ulso.curator.Curator;

import java.util.Map;

@Component(modules = HttpServerModule.class)
@Singleton
public interface HttpServerFactory
{
    HttpServer httpServer();

    @Component.Builder
    interface Builder
    {
        @BindsInstance
        Builder serverPort(int serverPort);

        @BindsInstance
        Builder curators(Map<String, Curator> curators);

        HttpServerFactory build();
    }
}