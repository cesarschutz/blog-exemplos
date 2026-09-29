package br.com.cesarschutz.exemplos.criptografia;

import java.io.IOException;
import java.io.StringWriter;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Date;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.ExtendedKeyUsage;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.asn1.x509.KeyPurposeId;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.bouncycastle.openssl.jcajce.JcaPKCS8Generator;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

/**
 * As CAs e os certificados de teste, gerados a cada execução: nenhuma chave privada fica no
 * repositório. No mundo real, eles vêm de uma CA interna ou pública (no RDS, da própria AWS).
 */
final class Certificados {

    /** Um certificado com a chave privada dele, nos formatos que o Postgres e o MongoDB leem. */
    record Par(X509Certificate certificado, PrivateKey chave) {

        String certificadoPem() {
            return pem(certificado);
        }

        /** A chave privada em PKCS#8 ("BEGIN PRIVATE KEY"). */
        String chavePem() {
            try {
                return pem(new JcaPKCS8Generator(chave, null).generate());
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }

        /** O arquivo que o MongoDB pede em certificateKeyFile: certificado + chave privada. */
        String certificadoEChavePem() {
            return certificadoPem() + chavePem();
        }

        /** O certificado num arquivo temporário, para o sslrootcert do Postgres. */
        Path emArquivo() {
            try {
                Path arquivo = Files.createTempFile("ca-", ".pem");
                arquivo.toFile().deleteOnExit();
                return Files.writeString(arquivo, certificadoPem());
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }
    }

    private static final SecureRandom ALEATORIO = new SecureRandom();

    private Certificados() {
    }

    /** Uma CA autoassinada, que só assina outros certificados. */
    static Par ca(String nome) {
        KeyPair chaves = novasChaves();
        X500Name dono = new X500Name("CN=" + nome);
        X509v3CertificateBuilder construtor = construtor(dono, dono, chaves);
        try {
            construtor.addExtension(Extension.basicConstraints, true, new BasicConstraints(true));
            construtor.addExtension(Extension.keyUsage, true, new KeyUsage(KeyUsage.keyCertSign | KeyUsage.cRLSign));
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        return new Par(assinar(construtor, chaves.getPrivate()), chaves.getPrivate());
    }

    /**
     * Um certificado de servidor assinado pela CA, válido para os nomes informados. Leva também o uso
     * clientAuth, que os membros de um replica set do MongoDB usam para conversar entre si.
     */
    static Par servidor(Par ca, String... nomes) {
        KeyPair chaves = novasChaves();
        X500Name emissor = new X500Name(ca.certificado().getSubjectX500Principal().getName());
        X509v3CertificateBuilder construtor = construtor(emissor, new X500Name("CN=" + nomes[0]), chaves);
        GeneralName[] alternativos = Arrays.stream(nomes)
                .map(nome -> new GeneralName(GeneralName.dNSName, nome))
                .toArray(GeneralName[]::new);
        try {
            construtor.addExtension(Extension.basicConstraints, true, new BasicConstraints(false));
            construtor.addExtension(Extension.keyUsage, true,
                    new KeyUsage(KeyUsage.digitalSignature | KeyUsage.keyEncipherment));
            construtor.addExtension(Extension.extendedKeyUsage, false,
                    new ExtendedKeyUsage(new KeyPurposeId[] {KeyPurposeId.id_kp_serverAuth, KeyPurposeId.id_kp_clientAuth}));
            construtor.addExtension(Extension.subjectAlternativeName, false, new GeneralNames(alternativos));
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        return new Par(assinar(construtor, ca.chave()), chaves.getPrivate());
    }

    private static KeyPair novasChaves() {
        try {
            KeyPairGenerator gerador = KeyPairGenerator.getInstance("RSA");
            gerador.initialize(2048, ALEATORIO);
            return gerador.generateKeyPair();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    private static X509v3CertificateBuilder construtor(X500Name emissor, X500Name dono, KeyPair chaves) {
        Instant agora = Instant.now();
        return new JcaX509v3CertificateBuilder(
                emissor,
                new BigInteger(64, ALEATORIO),
                Date.from(agora.minus(Duration.ofMinutes(5))),
                Date.from(agora.plus(Duration.ofDays(1))),
                dono,
                chaves.getPublic());
    }

    private static X509Certificate assinar(X509v3CertificateBuilder construtor, PrivateKey chaveDaCa) {
        try {
            var assinante = new JcaContentSignerBuilder("SHA256withRSA").build(chaveDaCa);
            return new JcaX509CertificateConverter().getCertificate(construtor.build(assinante));
        } catch (OperatorCreationException | GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String pem(Object objeto) {
        StringWriter texto = new StringWriter();
        try (JcaPEMWriter escritor = new JcaPEMWriter(texto)) {
            escritor.writeObject(objeto);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        return texto.toString();
    }
}
