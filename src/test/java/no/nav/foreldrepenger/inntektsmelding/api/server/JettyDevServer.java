package no.nav.foreldrepenger.inntektsmelding.api.server;

import no.nav.foreldrepenger.konfig.Environment;

public class JettyDevServer extends JettyServer {

    private static final Environment ENV = Environment.current();

    public static void main(String[] args) throws Exception {
        initTrustStoreAndKeyStore();
        jettyServer(args).bootStrap();
    }

    private static JettyDevServer jettyServer(String[] args) {
        if (args.length > 0) {
            return new JettyDevServer(Integer.parseUnsignedInt(args[0]));
        }
        return new JettyDevServer(ENV.getProperty("server.port", Integer.class, 8041));
    }

    private JettyDevServer(int serverPort) {
        super(serverPort);
    }

    private static void initTrustStoreAndKeyStore() {
        var keystoreTruststorePassword = ENV.getProperty("vtp.ssl.passord");
        System.setProperty("javax.net.ssl.password", keystoreTruststorePassword);
    }
}
