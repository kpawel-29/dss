package eu.europa.esig.dss.pades.validation;

import eu.europa.esig.dss.enumerations.CertificateOrigin;
import eu.europa.esig.dss.model.x509.CertificateToken;
import eu.europa.esig.dss.pdf.PdfDssDict;
import eu.europa.esig.dss.pdf.PdfVRIDict;
import eu.europa.esig.dss.spi.x509.TokenCertificateSource;
import eu.europa.esig.dss.utils.Utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The certificate source extracted from a DSS dictionary
 */
public class PdfDssDictCertificateSource extends TokenCertificateSource {

    /** The DSS dictionary */
    private final PdfDssDict dssDictionary;

    /**
     * Default constructor
     *
     * @param dssDictionary {@link PdfDssDict}
     */
    public PdfDssDictCertificateSource(final PdfDssDict dssDictionary) {
        this.dssDictionary = dssDictionary;
        extractFromDSSDict();
    }

    private void extractFromDSSDict() {
        for (CertificateToken certToken : getDSSDictionaryCertValues()) {
            addCertificate(certToken, CertificateOrigin.DSS_DICTIONARY);
        }
        for (CertificateToken certToken : getVRIDictionaryCertValues()) {
            addCertificate(certToken, CertificateOrigin.VRI_DICTIONARY);
        }
    }

    /**
     * Gets a map of PDF object ids and corresponding certificate tokens
     *
     * @return a map of PDF object ids and corresponding certificate tokens
     */
    public Map<Long, CertificateToken> getCertificateMap() {
        if (dssDictionary != null) {
            Map<Long, CertificateToken> dssCerts = dssDictionary.getCERTs();
            List<PdfVRIDict> vriDicts = dssDictionary.getVRIs();
            if (Utils.isCollectionNotEmpty(vriDicts)) {
                for (PdfVRIDict vriDict : vriDicts) {
                    dssCerts.putAll(vriDict.getCERTs());
                }
            }
            return dssCerts;
        }
        return Collections.emptyMap();
    }

    /**
     * Gets list of DSS dictionary certificate tokens
     *
     * @return a list of {@link CertificateToken}s
     */
    public List<CertificateToken> getDSSDictionaryCertValues() {
        if (dssDictionary != null) {
            Map<Long, CertificateToken> dssCerts = dssDictionary.getCERTs();
            return new ArrayList<>(dssCerts.values());
        }
        return Collections.emptyList();
    }

    /**
     * Gets list of certificate tokens extracted from all VRI dictionaries
     *
     * @return a list of {@link CertificateToken}s
     */
    public List<CertificateToken> getVRIDictionaryCertValues() {
        if (dssDictionary != null) {
            Map<Long, CertificateToken> vriCerts = new HashMap<>();
            List<PdfVRIDict> vris = dssDictionary.getVRIs();
            if (vris != null) {
                for (PdfVRIDict vri : vris) {
                    vriCerts.putAll(vri.getCERTs());
                }
            }
            return new ArrayList<>(vriCerts.values());
        }
        return Collections.emptyList();
    }

}
