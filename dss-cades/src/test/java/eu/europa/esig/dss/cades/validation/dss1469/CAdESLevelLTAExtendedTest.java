package eu.europa.esig.dss.cades.validation.dss1469;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;

import eu.europa.esig.dss.cades.validation.AbstractCAdESTestValidation;
import eu.europa.esig.dss.diagnostic.DiagnosticData;
import eu.europa.esig.dss.diagnostic.RelatedRevocationWrapper;
import eu.europa.esig.dss.diagnostic.RevocationRefWrappper;
import eu.europa.esig.dss.diagnostic.SignatureWrapper;
import eu.europa.esig.dss.diagnostic.TimestampWrapper;
import eu.europa.esig.dss.enumerations.RevocationOrigin;
import eu.europa.esig.dss.enumerations.RevocationRefOrigin;
import eu.europa.esig.dss.enumerations.SignatureLevel;
import eu.europa.esig.dss.enumerations.TimestampType;
import eu.europa.esig.dss.model.DSSDocument;
import eu.europa.esig.dss.model.FileDocument;
import eu.europa.esig.dss.utils.Utils;
import eu.europa.esig.dss.validation.AdvancedSignature;

public class CAdESLevelLTAExtendedTest extends AbstractCAdESTestValidation {

	@Override
	protected DSSDocument getSignedDocument() {
		return new FileDocument("src/test/resources/validation/dss-1469/cadesLTAwithATv2.sig");
	}
	
	@Override
	protected List<DSSDocument> getDetachedContents() {
		return Arrays.asList(new FileDocument("src/test/resources/validation/dss-1469/screenshot2.png"));
	}
	
	@Override
	protected void verifySourcesAndDiagnosticData(List<AdvancedSignature> advancedSignatures,
			DiagnosticData diagnosticData) {
		super.verifySourcesAndDiagnosticData(advancedSignatures, diagnosticData);
		
		SignatureWrapper signature = diagnosticData.getSignatureById(diagnosticData.getFirstSignatureId());
		assertNotNull(signature);
		List<RelatedRevocationWrapper> foundRevocations = signature.foundRevocations().getRelatedRevocationData();
		assertNotNull(foundRevocations);
		assertEquals(1, foundRevocations.size());
		List<RelatedRevocationWrapper> timestampRevocationValues = signature.foundRevocations()
				.getRelatedRevocationsByOrigin(RevocationOrigin.REVOCATION_VALUES);
		assertNotNull(timestampRevocationValues);
		assertEquals(1, timestampRevocationValues.size());
		List<RelatedRevocationWrapper> timestampRevocationRefs = signature.foundRevocations()
				.getRelatedRevocationsByRefOrigin(RevocationRefOrigin.COMPLETE_REVOCATION_REFS);
		assertNotNull(timestampRevocationRefs);
		assertEquals(1, timestampRevocationRefs.size());
	}
	
	@Override
	protected void checkTimestamps(DiagnosticData diagnosticData) {
		super.checkTimestamps(diagnosticData);
		
		List<TimestampWrapper> timestamps = diagnosticData.getTimestampList();
		assertTrue(Utils.isCollectionNotEmpty(timestamps));
		int signatureTimestampCounter = 0;
		int archiveTimestampCounter = 0;
		for (TimestampWrapper timestamp : timestamps) {
			if (TimestampType.ARCHIVE_TIMESTAMP.equals(timestamp.getType())) {
				assertEquals(11, timestamp.getTimestampedObjects().size());
				archiveTimestampCounter++;
			} else if (TimestampType.SIGNATURE_TIMESTAMP.equals(timestamp.getType())) {
				assertEquals(3, timestamp.getTimestampedObjects().size());
				List<RelatedRevocationWrapper> timestampFoundRevocations = timestamp.foundRevocations().getRelatedRevocationData();
				assertEquals(1, timestampFoundRevocations.size());
				RelatedRevocationWrapper xmlFoundRevocation = timestampFoundRevocations.get(0);
				assertTrue(xmlFoundRevocation.getOrigins().contains(RevocationOrigin.REVOCATION_VALUES));
				List<RevocationRefWrappper> revocationRefs = xmlFoundRevocation.getReferences();
				assertEquals(1, revocationRefs.size());
				RevocationRefWrappper xmlRevocationRef = revocationRefs.get(0);
				assertTrue(xmlRevocationRef.getOrigins().contains(RevocationRefOrigin.COMPLETE_REVOCATION_REFS));
				signatureTimestampCounter++;
			}
			assertTrue(timestamp.isMessageImprintDataFound());
			assertTrue(timestamp.isMessageImprintDataIntact());
		}
		assertEquals(1, signatureTimestampCounter);
		assertEquals(1, archiveTimestampCounter);
	}
	
	@Override
	protected void checkSignatureLevel(DiagnosticData diagnosticData) {
		assertEquals(SignatureLevel.CAdES_101733_C, diagnosticData.getSignatureFormat(diagnosticData.getFirstSignatureId()));
	}

}
