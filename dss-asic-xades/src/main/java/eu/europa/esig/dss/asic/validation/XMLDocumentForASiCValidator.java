package eu.europa.esig.dss.asic.validation;

import eu.europa.esig.dss.DSSDocument;
import eu.europa.esig.dss.x509.CertificatePool;
import eu.europa.esig.dss.xades.validation.XMLDocumentValidator;

public class XMLDocumentForASiCValidator extends XMLDocumentValidator implements ASiCSignatureValidator {

	public XMLDocumentForASiCValidator(DSSDocument signature) {
		super(signature);
	}

	@Override
	public void setValidationCertPool(CertificatePool validationCertPool) {
		this.validationCertPool = validationCertPool;
	}

}
