package org.snomed.snowstorm.rest;

import com.fasterxml.jackson.annotation.JsonView;
import io.kaicode.rest.util.branchpathrewrite.BranchPathUriUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.snomed.snowstorm.core.data.domain.ConceptMini;
import org.snomed.snowstorm.core.data.domain.Relationship;
import org.snomed.snowstorm.core.data.services.ConceptService;
import org.snomed.snowstorm.core.data.services.RelationshipService;
import org.snomed.snowstorm.rest.pojo.ItemsPage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@Api(tags = "Relationships", description = "-")
@RequestMapping(produces = "application/json")
public class RelationshipController {

	@Autowired
	private ConceptService conceptService;

	@Autowired
	private RelationshipService relationshipService;

	public enum RelationshipCharacteristicType {
		STATED_RELATIONSHIP, INFERRED_RELATIONSHIP, ADDITIONAL_RELATIONSHIP;

		private Relationship.CharacteristicType getCharacteristicType() {
			if (this == STATED_RELATIONSHIP) {
				return Relationship.CharacteristicType.stated;
			} else if (this == INFERRED_RELATIONSHIP) {
				return Relationship.CharacteristicType.inferred;
			} else {
				return Relationship.CharacteristicType.additional;
			}
		}
	}

	@RequestMapping(value = "{branch}/relationships", method = RequestMethod.GET)
	@ResponseBody
	@JsonView(value = View.Component.class)
	public ItemsPage<Relationship> findRelationships(@PathVariable String branch,
			@RequestParam(required = false) Boolean active,
			@RequestParam(required = false) String module,
			@RequestParam(required = false) String effectiveTime,
			@RequestParam(required = false) String source,
			@RequestParam(required = false) String type,
			@RequestParam(required = false) String destination,
			@RequestParam(required = false) RelationshipCharacteristicType characteristicType,
			@RequestParam(required = false) Integer group,
			@RequestParam(defaultValue = "0") int offset,
			@RequestParam(defaultValue = "50") int limit,
			@RequestHeader(value = "Accept-Language", defaultValue = ControllerHelper.DEFAULT_ACCEPT_LANG_HEADER) String acceptLanguageHeader) {

		branch = BranchPathUriUtil.decodePath(branch);
		List<String> languageCodes = ControllerHelper.getLanguageCodes(acceptLanguageHeader);
		Page<Relationship> relationshipPage = relationshipService.findRelationships(
				branch,
				null,
				active,
				module,
				effectiveTime,
				source,
				type,
				destination,
				characteristicType != null ? characteristicType.getCharacteristicType() : null,
				group,
				ControllerHelper.getPageRequest(offset, limit));

		expandSourceTypeAndDestination(branch, relationshipPage.getContent(), languageCodes);

		return new ItemsPage<>(relationshipPage);
	}

	private void expandSourceTypeAndDestination(String branch, List<Relationship> relationships, List<String> languageCodes) {
		Set<String> allIds = new HashSet<>();
		relationships.forEach(r -> {
			allIds.add(r.getSourceId());
			allIds.add(r.getTypeId());
			allIds.add(r.getDestinationId());
		});
		
		Map<String, ConceptMini> conceptMinis = conceptService.findConceptMinis(branch, allIds, languageCodes).getResultsMap();
		
		relationships.forEach(r -> {
			r.setSource(conceptMinis.get(r.getSourceId()));
			r.setType(conceptMinis.get(r.getTypeId()));
			r.setTarget(conceptMinis.get(r.getDestinationId()));
		});
	}

	@RequestMapping(value = "{branch}/relationships/{relationshipId}", method = RequestMethod.GET)
	@ResponseBody
	@JsonView(value = View.Component.class)
	public Relationship fetchRelationship(
			@PathVariable String branch,
			@PathVariable String relationshipId,
			@RequestHeader(value = "Accept-Language", defaultValue = ControllerHelper.DEFAULT_ACCEPT_LANG_HEADER) String acceptLanguageHeader) {
		branch = BranchPathUriUtil.decodePath(branch);
		List<String> languageCodes = ControllerHelper.getLanguageCodes(acceptLanguageHeader);
		Relationship relationship = relationshipService.findRelationship(BranchPathUriUtil.decodePath(branch), relationshipId);
		if (relationship != null) {
			expandSourceTypeAndDestination(branch, Collections.singletonList(relationship), languageCodes);
		}
		return ControllerHelper.throwIfNotFound("Relationship", relationship);
	}

	@ApiOperation(value = "Delete a relationship.")
	@RequestMapping(value = "{branch}/relationships/{relationshipId}", method = RequestMethod.DELETE)
	@ResponseBody
	@JsonView(value = View.Component.class)
	public void deleteRelationship(
			@PathVariable String branch,
			@PathVariable String relationshipId,
			@ApiParam("Force the deletion of a released relationship.")
			@RequestParam(defaultValue = "false") boolean force) {
		branch = BranchPathUriUtil.decodePath(branch);
		relationshipService.deleteRelationship(relationshipId, branch, force);
	}

}
