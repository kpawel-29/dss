package eu.europa.esig.dss.EN319102.validation.vpfltvd;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.europa.esig.dss.EN319102.bbb.Chain;
import eu.europa.esig.dss.EN319102.bbb.ChainItem;
import eu.europa.esig.dss.EN319102.validation.vpfltvd.checks.AcceptableBasicSignatureValidationCheck;
import eu.europa.esig.dss.EN319102.validation.vpfltvd.checks.RevocationBasicBuildingBlocksCheck;
import eu.europa.esig.dss.jaxb.detailedreport.XmlBasicBuildingBlocks;
import eu.europa.esig.dss.jaxb.detailedreport.XmlConstraint;
import eu.europa.esig.dss.jaxb.detailedreport.XmlConstraintsConclusion;
import eu.europa.esig.dss.jaxb.detailedreport.XmlStatus;
import eu.europa.esig.dss.jaxb.detailedreport.XmlValidationProcessLongTermData;
import eu.europa.esig.dss.validation.RevocationWrapper;
import eu.europa.esig.dss.validation.TimestampWrapper;
import eu.europa.esig.jaxb.policy.Level;
import eu.europa.esig.jaxb.policy.LevelConstraint;

/**
 * 5.5 Validation process for Signatures with Time and Signatures with Long-Term Validation Data
 */
public class ValidationProcessForSignaturesWithLongTermValidationData extends Chain<XmlValidationProcessLongTermData> {

	private static final Logger logger = LoggerFactory.getLogger(ValidationProcessForSignaturesWithLongTermValidationData.class);

	private final XmlConstraintsConclusion basicSignatureValidation;
	private final XmlConstraintsConclusion timestampValidation;

	private final Set<TimestampWrapper> timestamps;
	private final Set<RevocationWrapper> revocationData;
	private final Map<String, XmlBasicBuildingBlocks> bbbs;

	private final Date currentDate;

	public ValidationProcessForSignaturesWithLongTermValidationData(XmlConstraintsConclusion basicSignatureValidation,
			XmlConstraintsConclusion timestampValidation, Set<TimestampWrapper> timestamps, Set<RevocationWrapper> revocationData,
			Map<String, XmlBasicBuildingBlocks> bbbs, Date currentDate) {
		super(new XmlValidationProcessLongTermData());

		this.basicSignatureValidation = basicSignatureValidation;
		this.timestampValidation = timestampValidation;
		this.timestamps = timestamps;
		this.revocationData = revocationData;
		this.bbbs = bbbs;
		this.currentDate = currentDate;
	}

	@Override
	protected void initChain() {

		/*
		 * 1) The process shall initialize the set of signature time-stamp tokens from the signature time-stamp
		 * attributes present in the signature and shall initialize the best-signature-time to the current time.
		 * NOTE 1: Best-signature-time is an internal variable for the algorithm denoting the earliest time when it can
		 * be proven that a signature has existed.
		 */
		Date bestSignatureTime = currentDate;

		/*
		 * 5.5.4 2) Signature validation: the process shall perform the validation process for Basic Signatures as per
		 * clause 5.3 with all the inputs, including the processing of any signed attributes as specified. If the
		 * Signature contains long-term validation data, this data shall be passed to the validation process for Basic
		 * Signatures.
		 * 
		 * If this validation returns PASSED, INDETERMINATE/CRYPTO_CONSTRAINTS_FAILURE_NO_POE,
		 * INDETERMINATE/REVOKED_NO_POE or INDETERMINATE/OUT_OF_BOUNDS_NO_POE, the SVA
		 * shall go to the next step. Otherwise, the process shall return the status and information returned by the
		 * validation process for Basic Signatures.
		 */
		ChainItem<XmlValidationProcessLongTermData> item = firstItem = isAcceptableBasicSignatureValidation();

		if (CollectionUtils.isNotEmpty(revocationData)) {
			for (RevocationWrapper revocation : revocationData) {
				XmlBasicBuildingBlocks revocationBBB = bbbs.get(revocation.getId());
				if (revocationBBB != null) {
					item = item.setNextItem(revocationBasicBuildingBlocksValid(revocationBBB));
				}
			}
		}

		/*
		 * 3) Signature time-stamp validation:
		 * a) For each time-stamp token in the set of signature time-stamp tokens, the process shall check that the
		 * message imprint has been generated according to the corresponding signature format specification
		 * verification. If the verification fails, the process shall remove the token from the set.
		 */
		Set<TimestampWrapper> allowedTimestamps = filterInvalidMessageImprint(timestamps);

		/*
		 * b) Time-stamp token validation: For each time-stamp token remaining in the set of signature time-stamp
		 * tokens, the process shall perform the time-stamp validation process as per clause 5.4:
		 * 
		 * If PASSED is returned and if the returned generation time is before best-signature-time, the process
		 * shall set best-signature-time to this date and shall try the next token.
		 */
		for (TimestampWrapper timestampWrapper : allowedTimestamps) {
			List<XmlConstraint> constraints = timestampValidation.getConstraints();
			boolean foundValidationTSP = false;
			for (XmlConstraint tspValidation : constraints) {
				if (StringUtils.equals(timestampWrapper.getId(), tspValidation.getId())) {
					foundValidationTSP = true;
					Date productionTime = timestampWrapper.getProductionTime();
					if (XmlStatus.OK.equals(tspValidation.getStatus()) && productionTime.before(bestSignatureTime)) {
						bestSignatureTime = productionTime;
						break;
					}
				}
			}
			if (!foundValidationTSP) {
				logger.warn("Cannot find tsp validation info for tsp " + timestampWrapper.getId());
			}
		}
	}

	private ChainItem<XmlValidationProcessLongTermData> revocationBasicBuildingBlocksValid(XmlBasicBuildingBlocks revocationBBB) {
		LevelConstraint constraint = new LevelConstraint();
		constraint.setLevel(Level.FAIL);

		return new RevocationBasicBuildingBlocksCheck(result, revocationBBB, constraint);
	}

	private ChainItem<XmlValidationProcessLongTermData> isAcceptableBasicSignatureValidation() {
		LevelConstraint constraint = new LevelConstraint();
		constraint.setLevel(Level.FAIL);

		return new AcceptableBasicSignatureValidationCheck(result, basicSignatureValidation, constraint);
	}

	private Set<TimestampWrapper> filterInvalidMessageImprint(Set<TimestampWrapper> allTimestamps) {
		Set<TimestampWrapper> result = new HashSet<TimestampWrapper>();
		for (TimestampWrapper tsp : allTimestamps) {
			if (tsp.isMessageImprintDataFound() && tsp.isMessageImprintDataIntact()) {
				result.add(tsp);
			} else {
				logger.info("Timestamp " + tsp.getId() + " is skipped");
			}
		}
		return result;
	}

}
