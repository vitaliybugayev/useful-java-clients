package client.temporal;

import io.grpc.netty.shaded.io.netty.handler.codec.http2.Http2SecurityUtil;
import io.grpc.netty.shaded.io.netty.handler.ssl.*;
import io.grpc.netty.shaded.io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.temporal.api.workflowservice.v1.ListWorkflowExecutionsRequest;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowClientOptions;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.serviceclient.WorkflowServiceStubsOptions;
import lombok.extern.slf4j.Slf4j;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;

import java.io.ByteArrayInputStream;
import java.io.StringReader;
import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.Optional;

@Slf4j
public class TemporalClient {
    private final WorkflowClient client;
    private final WorkflowServiceStubs serviceStubs;

    public TemporalClient(String target, String clientKeyPathBase64, String clientCertPathBase64, String namespace) {
        Security.addProvider(new BouncyCastleProvider());
        validateConfiguration(target, clientKeyPathBase64, clientCertPathBase64, namespace);

        try {
            log.info("Parsing private key and certificate for SSL context...");
            var privateKey = parsePrivateKey(clientKeyPathBase64);
            var certificate = parseCertificate(clientCertPathBase64);

            log.info("Creating SSL context...");
            var sslContext = createSslContext(privateKey, certificate);

            log.info("Configuring Temporal service stubs...");
            var stubsOptions = WorkflowServiceStubsOptions.newBuilder()
                    .setSslContext(sslContext)
                    .setTarget(target)
                    .build();

            this.serviceStubs = WorkflowServiceStubs.newServiceStubs(stubsOptions);

            var clientOptions = WorkflowClientOptions.newBuilder()
                    .setNamespace(namespace)
                    .build();

            this.client = WorkflowClient.newInstance(serviceStubs, clientOptions);

        } catch (Exception e) {
            log.error("Error initializing TemporalClient", e);
            throw new RuntimeException("Error initializing TemporalClient: " + e.getMessage(), e);
        }
    }

    private void validateConfiguration(String target, String clientKeyPathBase64,
                                       String clientCertPathBase64, String namespace) {
        if (target == null || clientKeyPathBase64 == null
                || clientCertPathBase64 == null || namespace == null) {
            throw new IllegalArgumentException("Missing required parameters for connection");
        }
    }

    private PrivateKey parsePrivateKey(String clientKeyPathBase64) throws Exception {
        var clientKeyBytes = Base64.getDecoder().decode(clientKeyPathBase64);
        var privateKeyPem = new String(clientKeyBytes);

        try (var pemParser = new PEMParser(new StringReader(privateKeyPem))) {
            var converter = new JcaPEMKeyConverter().setProvider("BC");
            var pemObject = pemParser.readObject();

            if (pemObject instanceof PEMKeyPair) {
                var keyPair = (PEMKeyPair) pemObject;
                return converter.getPrivateKey(keyPair.getPrivateKeyInfo());
            } else if (pemObject instanceof org.bouncycastle.asn1.pkcs.PrivateKeyInfo) {
                return converter.getPrivateKey((org.bouncycastle.asn1.pkcs.PrivateKeyInfo) pemObject);
            }

            throw new IllegalArgumentException("Unsupported key format: "
                    + pemObject.getClass().getSimpleName());
        }
    }

    private X509Certificate parseCertificate(String clientCertPathBase64) throws Exception {
        var clientCertBytes = Base64.getDecoder().decode(clientCertPathBase64);
        var certificateFactory = CertificateFactory.getInstance("X.509");
        return (X509Certificate) certificateFactory.generateCertificate(
                new ByteArrayInputStream(clientCertBytes)
        );
    }

    private SslContext createSslContext(PrivateKey privateKey, X509Certificate certificate) throws Exception {
        return SslContextBuilder.forClient()
                .keyManager(privateKey, certificate)
                .trustManager(InsecureTrustManagerFactory.INSTANCE)
                .ciphers(Http2SecurityUtil.CIPHERS, io.grpc.netty.shaded.io.netty
                        .handler.ssl.SupportedCipherSuiteFilter.INSTANCE)
                .applicationProtocolConfig(new ApplicationProtocolConfig(
                        ApplicationProtocolConfig.Protocol.ALPN,
                        ApplicationProtocolConfig.SelectorFailureBehavior.NO_ADVERTISE,
                        ApplicationProtocolConfig.SelectedListenerFailureBehavior.ACCEPT,
                        ApplicationProtocolNames.HTTP_2))
                .clientAuth(ClientAuth.NONE)
                .build();
    }

    public <T> T getWorkflow(Class<T> workflowClass, String workflowId) {
        log.info("Getting workflow with ID: {}", workflowId);
        var workflowStub = client.newUntypedWorkflowStub(workflowId, Optional.empty(),
                Optional.empty());
        return client.newWorkflowStub(workflowClass, workflowStub.getExecution().getWorkflowId());
    }

    public <T> Optional<T> getWorkflowByRequestID(Class<T> workflowClass, String requestId) {
        String query = String.format("RequestID='%s'", requestId);
        log.info("Searching workflow with RequestID: {}", requestId);

        ListWorkflowExecutionsRequest listRequest = ListWorkflowExecutionsRequest.newBuilder()
                .setNamespace(client.getOptions().getNamespace())
                .setQuery(query)
                .build();

        var service = client.getWorkflowServiceStubs().blockingStub();
        var response = service.listWorkflowExecutions(listRequest);

        var executions = response.getExecutionsList();
        if (executions.isEmpty()) {
            log.warn("No workflow found for RequestID: {}", requestId);
            return Optional.empty();
        }

        String workflowId = executions.get(0).getExecution().getWorkflowId();
        log.info("Found workflowId: {}", workflowId);

        var workflowStub = client.newUntypedWorkflowStub(workflowId,
                Optional.empty(), Optional.empty());

        return Optional.of(client.newWorkflowStub(workflowClass, workflowStub.getExecution().getWorkflowId()));
    }


    public void close() {
        log.info("Shutting down Temporal service stubs...");
        serviceStubs.shutdown();
    }
}

