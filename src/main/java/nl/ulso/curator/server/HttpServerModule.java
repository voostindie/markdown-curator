package nl.ulso.curator.server;

import dagger.Binds;
import dagger.Module;
import dagger.multibindings.IntoSet;

/**
 * Runs an embedded HTTP server with REST API endpoints exposing the curators.
 */
@Module
public abstract class HttpServerModule
{
    @Binds
    abstract HttpServer bindHttpServer(JettyHttpServer jettyHttpServer);

    @Binds
    @IntoSet
    abstract RequestProcessor bindCuratorListRequest(CuratorListRequestProcessor request);

    @Binds
    @IntoSet
    abstract RequestProcessor bindQueryRequest(QueryRequestProcessor request);

    @Binds
    @IntoSet
    abstract RequestProcessor bindDocumentRequest(DocumentRequestProcessor request);
}
